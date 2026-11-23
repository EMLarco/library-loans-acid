package com.larco.biblioteca.repository;

import java.util.List;
import java.util.Map;

public interface AuditRepository {
    void save(String eventType, String performedBy, String details);
    List<Map<String, Object>> latest(int limit);
}
