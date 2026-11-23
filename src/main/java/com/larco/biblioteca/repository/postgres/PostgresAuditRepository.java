package com.larco.biblioteca.repository.postgres;

import com.larco.biblioteca.repository.AuditRepository;
import com.larco.biblioteca.transaction.TransactionFailedException;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class PostgresAuditRepository implements AuditRepository {
    private final DataSource ds;

    public PostgresAuditRepository(DataSource ds) { this.ds = ds; }

    @Override
    public void save(String eventType, String performedBy, String details) {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO audit_log (event_type, performed_by, details) VALUES (?,?,?)")) {
            ps.setString(1, eventType);
            ps.setString(2, performedBy);
            ps.setString(3, details);
            ps.executeUpdate();
        } catch (SQLException e) { throw new TransactionFailedException(e.getMessage(), e); }
    }

    @Override
    public List<Map<String, Object>> latest(int limit) {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT audit_id, event_type, performed_by, details, to_char(created_at, 'YYYY-MM-DD HH24:MI:SS') "
                             + "FROM audit_log ORDER BY audit_id DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<Map<String, Object>> out = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("audit_id", rs.getLong(1));
                    row.put("event_type", rs.getString(2));
                    row.put("performed_by", rs.getString(3));
                    row.put("details", rs.getString(4));
                    row.put("created_at", rs.getString(5));
                    out.add(row);
                }
                return out;
            }
        } catch (SQLException e) { throw new TransactionFailedException(e.getMessage(), e); }
    }
}
