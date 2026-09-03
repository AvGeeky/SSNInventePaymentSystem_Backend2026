package com.saipbuilds.inventepayment2026.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<Map<String, String>> error(HttpServletRequest request) {
        return ResponseEntity.status(500).body(
                Map.of("error", "Unknown error occurred. Please try again later.",
                        "status", String.valueOf(request.getAttribute("javax.servlet.error.status_code")),
                        "message", (String) request.getAttribute("javax.servlet.error.message"))
        );
    }
}
