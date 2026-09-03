package com.saipbuilds.inventepayment2026.Controller;

import com.saipbuilds.inventepayment2026.Services.ExternalControllerReceiverService;
import com.saipbuilds.inventepayment2026.dto.HackathonRegistrationRequest;
import com.saipbuilds.inventepayment2026.dto.StandardRegistrationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
public class ExternalController {
    private static final Logger log = LoggerFactory.getLogger(ExternalController.class);

    private final ExternalControllerReceiverService externalControllerReceiverService;

    @Autowired
    public ExternalController(ExternalControllerReceiverService externalControllerReceiverService){
        this.externalControllerReceiverService = externalControllerReceiverService;
    }

    @PostMapping("/api/v1/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody StandardRegistrationRequest request){
        log.info("Received registration request for email: {}", request.getEmail());

        // Pass DTO TO Service layer
        UUID ticketId = externalControllerReceiverService.handleStandardRegistration(request);

        // Send Response
        Map<String, Object> response = new HashMap<>();
        response.put("ticket_id", ticketId);
        response.put("message", "Save this ID. Provide it when uploading the payment receipt PDF.");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/api/v1/register-hackathon")
    public ResponseEntity<Map<String, Object>> register(@RequestBody HackathonRegistrationRequest request){
        log.info("Received Hackathon registration request for email: {}", request.getLeader().getEmail());

        // Pass DTO TO Service layer
        UUID ticketId = externalControllerReceiverService.handleHackathonRegistration(request);

        // Send Response
        Map<String, Object> response = new HashMap<>();
        response.put("ticket_id", ticketId);
        response.put("message", "Save this ID. Provide it when uploading the hackathon payment receipt PDF.");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}