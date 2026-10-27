package com.security.shiftmanager.controller;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Demonstrates the graceful shutdown (server.shutdown=graceful): start a slow process, stop the
 * application while it runs, and the request still finishes if it ends within
 * spring.lifecycle.timeout-per-shutdown-phase (20 s). ADMIN only (see SecurityConfig).
 */
@RestController
@RequestMapping("/api/admin/demo")
public class DemoController {

    private static final Logger log = LoggerFactory.getLogger(DemoController.class);
    // Above the 20 s shutdown timeout on purpose, so the "process cut off" case can be shown too.
    private static final int MAX_SECONDS = 30;

    /** Example: GET /api/admin/demo/slow-process?seconds=15 */
    @GetMapping("/slow-process")
    public Map<String, Object> slowProcess(@RequestParam(defaultValue = "15") int seconds) {
        if (seconds < 1 || seconds > MAX_SECONDS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "'seconds' must be between 1 and " + MAX_SECONDS);
        }
        Instant start = Instant.now();
        log.info("Slow process started ({} s)", seconds);
        try {
            Thread.sleep(Duration.ofSeconds(seconds));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Slow process interrupted after {} ms", Duration.between(start, Instant.now()).toMillis());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Process interrupted");
        }
        Instant end = Instant.now();
        log.info("Slow process finished ({} s)", seconds);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("seconds", seconds);
        result.put("startedAt", start.toString());
        result.put("finishedAt", end.toString());
        return result;
    }
}
