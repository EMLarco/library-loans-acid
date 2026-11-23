package com.larco.biblioteca.demo;

import com.larco.biblioteca.model.Caller;
import com.larco.biblioteca.model.Loan;
import com.larco.biblioteca.model.LoanRequest;
import com.larco.biblioteca.service.LoanService;
import com.larco.biblioteca.support.ActivityLog;
import com.larco.biblioteca.transaction.TransactionRunner;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Escenarios de prueba ACID que se ejecutan desde la pestaña "Pruebas ACID" de la interfaz. */
@Service
public class AcidDemoService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final Set<String> TABLES = Set.of("authors", "books", "members", "loans", "categories", "audit_log");
    private static final Caller DEMO_ADMIN = new Caller("demo", "ADMIN");

    private final LoanService loanService;   // se usa a través del Proxy
    private final TransactionRunner tx;
    private final DataSource ds;
    private final ActivityLog activity;

    public AcidDemoService(LoanService loanService, TransactionRunner tx, DataSource ds, ActivityLog activity) {
        this.loanService = loanService;
        this.tx = tx;
        this.ds = ds;
        this.activity = activity;
    }

    // ------------------------------------------------------------------ ATOMICIDAD
    public Map<String, Object> atomicity(int memberId, int bookId) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("prueba", "ATOMICIDAD: préstamo con error simulado después de insertar y actualizar");
        Map<String, Object> before = snapshot(bookId);
        out.put("antes", before);
        try {
            loanService.borrow(new LoanRequest(memberId, bookId, 14, true, 0), new Caller("demo-atomicidad", "ADMIN"));
            out.put("resultado", "La operación terminó sin error (no se esperaba)");
        } catch (RuntimeException e) {
            out.put("resultado", "Excepción capturada: " + e.getMessage());
        }
        Map<String, Object> after = snapshot(bookId);
        out.put("despues", after);
        boolean same = before.equals(after);
        out.put("conclusion", same
                ? "ROLLBACK correcto: la base quedó EXACTAMENTE igual (no hay préstamo parcial ni cambio de disponibilidad)"
                : "ATENCIÓN: el estado cambió");
        return out;
    }

    // ------------------------------------------------------------------ CONSISTENCIA
    public Map<String, Object> consistency() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("prueba", "CONSISTENCIA: operaciones inválidas no deben romper las reglas de la base");
        Map<String, Object> before = globalState();
        out.put("antes", before);

        Integer busyBook = scalarInt("SELECT book_id FROM loans WHERE return_status = FALSE ORDER BY loan_id LIMIT 1");
        Integer freeBook = scalarInt("SELECT book_id FROM books WHERE availability_status AND active ORDER BY book_id LIMIT 1");

        List<Map<String, Object>> cases = new ArrayList<>();
        cases.add(runCase("Préstamo con socio inexistente (9999)", "Rechazado por la aplicación",
                () -> loanService.borrow(new LoanRequest(9999, freeBook, 7), DEMO_ADMIN)));
        cases.add(runCase("Préstamo de libro inexistente (9999)", "Rechazado por la aplicación",
                () -> loanService.borrow(new LoanRequest(1, 9999, 7), DEMO_ADMIN)));
        cases.add(runCase("Préstamo con 0 días", "Cantidad debe ser mayor que cero",
                () -> loanService.borrow(new LoanRequest(1, freeBook, 0), DEMO_ADMIN)));
        if (busyBook != null) {
            cases.add(runCase("Prestar libro ya prestado (" + busyBook + ")", "Rechazado: no disponible",
                    () -> loanService.borrow(new LoanRequest(2, busyBook, 7), DEMO_ADMIN)));
            cases.add(runCase("INSERT directo: 2º préstamo activo del libro " + busyBook, "Índice único ux_loans_active_book",
                    () -> rawInsert("INSERT INTO loans (book_id, member_id, due_date) VALUES (" + busyBook + ", 3, CURRENT_DATE + 7)")));
        }
        cases.add(runCase("INSERT directo: due_date anterior a checkout_date", "CHECK ck_loans_due_after_checkout",
                () -> rawInsert("INSERT INTO loans (book_id, member_id, due_date) VALUES (" + freeBook + ", 1, CURRENT_DATE - 1)")));
        cases.add(runCase("INSERT directo: socio inexistente (FK)", "FOREIGN KEY loans_member_id_fkey",
                () -> rawInsert("INSERT INTO loans (book_id, member_id, due_date) VALUES (" + freeBook + ", 9999, CURRENT_DATE + 7)")));
        cases.add(runCase("INSERT directo: multa negativa", "CHECK ck_loans_fine_non_negative",
                () -> rawInsert("INSERT INTO loans (book_id, member_id, due_date, fine_amount) VALUES (" + freeBook + ", 1, CURRENT_DATE + 7, -5)")));
        out.put("casos", cases);

        Map<String, Object> after = globalState();
        out.put("despues", after);
        out.put("conclusion", before.equals(after)
                ? "Estado válido -> transacciones rechazadas -> estado válido. Sin inconsistencias."
                : "ATENCIÓN: el estado cambió");
        return out;
    }

    // ------------------------------------------------------------------ AISLAMIENTO / CONCURRENCIA
    public Map<String, Object> concurrency(int bookId, int memberA, int memberB) throws Exception {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("prueba", "AISLAMIENTO: dos socios intentan prestar el MISMO libro al mismo tiempo");
        out.put("antes", snapshot(bookId));
        activity.add("CONCURRENCIA", "Lanzando 2 hilos simultáneos sobre el libro " + bookId);

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Map<String, Object>> a = pool.submit(() -> attempt("Cliente-A", memberA, bookId, start));
            Future<Map<String, Object>> b = pool.submit(() -> attempt("Cliente-B", memberB, bookId, start));
            start.countDown();   // ambos arrancan a la vez
            out.put("clienteA", a.get());
            out.put("clienteB", b.get());
        } finally {
            pool.shutdown();
        }
        Map<String, Object> after = snapshot(bookId);
        out.put("despues", after);
        Object active = after.get("prestamos_activos_del_libro");
        out.put("conclusion", Integer.valueOf(1).equals(active)
                ? "Resultado consistente: solo UN préstamo activo. El segundo esperó el bloqueo FOR UPDATE y fue rechazado."
                : "ATENCIÓN: préstamos activos del libro = " + active);
        return out;
    }

    private Map<String, Object> attempt(String who, int member, int book, CountDownLatch start) throws InterruptedException {
        Thread.currentThread().setName(who);
        start.await();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("cliente", who);
        r.put("socio", member);
        r.put("inicio", LocalDateTime.now().format(FMT));
        try {
            Loan l = loanService.borrow(new LoanRequest(member, book, 7, false, 1500), new Caller(who, "BIBLIOTECARIO"));
            r.put("resultado", "EXITO - préstamo #" + l.loanId());
        } catch (RuntimeException e) {
            r.put("resultado", "RECHAZADO - " + e.getMessage());
        }
        r.put("fin", LocalDateTime.now().format(FMT));
        return r;
    }

    // ------------------------------------------------------------------ DURABILIDAD
    public Map<String, Object> runtime() {
        Map<String, Object> out = new LinkedHashMap<>();
        long startMs = ManagementFactory.getRuntimeMXBean().getStartTime();
        out.put("pid_aplicacion", ProcessHandle.current().pid());
        out.put("aplicacion_iniciada", LocalDateTime.ofInstant(Instant.ofEpochMilli(startMs), ZoneId.systemDefault()).toString());
        out.put("uptime_segundos", (System.currentTimeMillis() - startMs) / 1000);
        out.put("postgres_hora", scalarString("SELECT to_char(now(), 'YYYY-MM-DD HH24:MI:SS')"));
        out.put("postgres_iniciado", scalarString("SELECT to_char(pg_postmaster_start_time(), 'YYYY-MM-DD HH24:MI:SS')"));
        out.put("postgres_version", scalarString("SELECT version()"));
        out.put("estado", globalState());
        out.put("ultimos_prestamos", query("SELECT l.loan_id, b.title, m.name AS socio, l.checkout_date, l.return_status "
                + "FROM loans l JOIN books b USING (book_id) JOIN members m USING (member_id) ORDER BY l.loan_id DESC LIMIT 5"));
        return out;
    }

    // ------------------------------------------------------------------ VISOR DE TABLAS
    public List<Map<String, Object>> table(String name) {
        String t = name.toLowerCase();
        if (!TABLES.contains(t)) throw new IllegalArgumentException("Tabla no permitida: " + name);
        String order = switch (t) {
            case "authors" -> "author_id"; case "books" -> "book_id"; case "members" -> "member_id";
            case "loans" -> "loan_id DESC"; case "categories" -> "genre_id"; default -> "audit_id DESC";
        };
        return query("SELECT * FROM " + t + " ORDER BY " + order + " LIMIT 50");
    }

    public Map<String, Object> snapshot(int bookId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total_prestamos", scalarInt("SELECT count(*) FROM loans"));
        m.put("prestamos_activos_del_libro", scalarInt("SELECT count(*) FROM loans WHERE book_id = " + bookId + " AND return_status = FALSE"));
        m.put("libro_disponible", scalarString("SELECT availability_status::text FROM books WHERE book_id = " + bookId));
        return m;
    }

    private Map<String, Object> globalState() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total_prestamos", scalarInt("SELECT count(*) FROM loans"));
        m.put("prestamos_activos", scalarInt("SELECT count(*) FROM loans WHERE return_status = FALSE"));
        m.put("libros_no_disponibles", scalarInt("SELECT count(*) FROM books WHERE availability_status = FALSE"));
        // Regla: un libro no disponible <=> tiene un préstamo activo. Debe ser 0.
        m.put("inconsistencias", scalarInt("""
                SELECT count(*) FROM books b
                 WHERE b.availability_status = EXISTS (SELECT 1 FROM loans l WHERE l.book_id = b.book_id AND NOT l.return_status)
                """));
        return m;
    }

    private interface Action { void run() throws Exception; }

    private Map<String, Object> runCase(String name, String expected, Action action) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("caso", name);
        r.put("esperado", expected);
        try {
            action.run();
            r.put("resultado", "ACEPTADO (no esperado)");
        } catch (Exception e) {
            r.put("resultado", "RECHAZADO: " + e.getMessage());
        }
        return r;
    }

    private void rawInsert(String sql) {
        tx.inTransaction("SQL directo", c -> {
            try (Statement st = c.createStatement()) { return st.executeUpdate(sql); }
        });
    }

    private Integer scalarInt(String sql) {
        String s = scalarString(sql);
        return s == null ? null : Integer.valueOf(s);
    }

    private String scalarString(String sql) {
        try (Connection c = ds.getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : null;
        } catch (SQLException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> query(String sql) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData md = rs.getMetaData();
            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= md.getColumnCount(); i++) {
                    Object v = rs.getObject(i);
                    row.put(md.getColumnLabel(i), v == null ? null : v.toString());
                }
                rows.add(row);
            }
            return rows;
        } catch (SQLException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
