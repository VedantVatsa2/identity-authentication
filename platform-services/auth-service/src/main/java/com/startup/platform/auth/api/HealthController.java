package com.startup.platform.auth.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
public class HealthController {
    @GetMapping("/health")
    public String health() {
        return "ok";
    }
}
