package com.saipbuilds.inventepayment2026.Controller;

import com.saipbuilds.inventepayment2026.Services.ExternalControllerReceiverService;
import com.saipbuilds.inventepayment2026.Services.InternalControllerReceiverService;
import com.saipbuilds.inventepayment2026.dto.HackathonRegistrationRequest;
import com.saipbuilds.inventepayment2026.dto.StandardRegistrationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class InternalController {
    private static final Logger log = LoggerFactory.getLogger(InternalController.class);

    private final InternalControllerReceiverService internalControllerReceiverService;

    public InternalController(InternalControllerReceiverService internalControllerReceiverService) {
        this.internalControllerReceiverService = internalControllerReceiverService;

    }




    @PostMapping("/restricted/v1/approve-payment")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> request){
        String ticketIdStr = request.get("ticket_id");
        if (ticketIdStr == null || ticketIdStr.isBlank()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "E");
            errorResponse.put("message", "ticket_id not provided in request body.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
        UUID ticketId = UUID.fromString(ticketIdStr);
        internalControllerReceiverService.setPaymentVerified(ticketId);

        // Send Response
        Map<String, Object> response = new HashMap<>();
        response.put("ticket_id", ticketId);
        response.put("message", "Approved!");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/restricted/v1/reject-payment")
    public ResponseEntity<Map<String, Object>> reject(@RequestBody Map<String, String> request){
        String ticketIdStr = request.get("ticket_id");
        if (ticketIdStr == null || ticketIdStr.isBlank()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "E");
            errorResponse.put("message", "ticket_id not provided in request body.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
        UUID ticketId = UUID.fromString(ticketIdStr);
        internalControllerReceiverService.setPaymentRejected(ticketId);

        // Send Response
        Map<String, Object> response = new HashMap<>();
        response.put("ticket_id", ticketId);
        response.put("message", "Rejected!");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("restricted/v1/trigger-stats-email")
    public ResponseEntity<Map<String, String>> triggerStatsEmail(@RequestBody Map<String,String> recipientEmail) {
        Map<String, String> payload = new HashMap<>();
        String email = recipientEmail.get("recipientEmail");


        payload.put("recipient_email", email);

        internalControllerReceiverService.sendStatsEmail(payload);



        return ResponseEntity.ok(Map.of(
                "message", "Statistics compilation job queued successfully.",
                "recipient", email
        ));
    }
}