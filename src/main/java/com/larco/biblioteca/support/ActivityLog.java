package com.larco.biblioteca.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Bitácora en memoria que se muestra en la interfaz web (además de la consola).
 * Permite ver en una sola pantalla BEGIN/COMMIT/ROLLBACK, Proxy, Observer y Circuit Breaker.
 */
@Component
public class ActivityLog {

    private static final Logger log = LoggerFactory.getLogger("ACTIVIDAD");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final int MAX = 400;

    public record Entry(long seq, String time, String thread, String source, String message) { }

    private final LinkedList<Entry> entries = new LinkedList<>();
    private long seq = 0;

    public synchronized void add(String source, String message) {
        Entry e = new Entry(++seq, LocalTime.now().format(FMT), Thread.currentThread().getName(), source, message);
        entries.addLast(e);
        if (entries.size() > MAX) entries.removeFirst();
        log.info("[{}] {}", source, message);
    }

    public synchronized List<Entry> since(long afterSeq) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : entries) if (e.seq() > afterSeq) out.add(e);
        return out;
    }

    public synchronized void clear() {
        entries.clear();
    }
}
