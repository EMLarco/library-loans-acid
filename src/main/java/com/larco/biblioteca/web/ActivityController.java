package com.larco.biblioteca.web;

import com.larco.biblioteca.repository.AuditRepository;
import com.larco.biblioteca.support.ActivityLog;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ActivityController {
    private final ActivityLog activity;
    private final AuditRepository audit;

    public ActivityController(ActivityLog activity, AuditRepository audit) {
        this.activity = activity;
        this.audit = audit;
    }

    @GetMapping("/activity")
    public List<ActivityLog.Entry> activity(@RequestParam(defaultValue = "0") long after) { return activity.since(after); }

    @DeleteMapping("/activity")
    public Map<String, Object> clear() {
        activity.clear();
        return Map.of("cleared", true);
    }

    @GetMapping("/audit")
    public List<Map<String, Object>> audit(@RequestParam(defaultValue = "15") int limit) { return audit.latest(limit); }
}
