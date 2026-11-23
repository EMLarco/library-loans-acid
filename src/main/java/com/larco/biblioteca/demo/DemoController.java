package com.larco.biblioteca.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/demo")
public class DemoController {
    private final AcidDemoService demo;

    public DemoController(AcidDemoService demo) { this.demo = demo; }

    @PostMapping("/atomicity")
    public Map<String, Object> atomicity(@RequestParam int memberId, @RequestParam int bookId) { return demo.atomicity(memberId, bookId); }

    @PostMapping("/consistency")
    public Map<String, Object> consistency() { return demo.consistency(); }

    @PostMapping("/concurrency")
    public Map<String, Object> concurrency(@RequestParam int bookId, @RequestParam int memberA, @RequestParam int memberB) throws Exception {
        return demo.concurrency(bookId, memberA, memberB);
    }

    @GetMapping("/runtime")
    public Map<String, Object> runtime() { return demo.runtime(); }

    @GetMapping("/table/{name}")
    public List<Map<String, Object>> table(@PathVariable String name) { return demo.table(name); }
}
