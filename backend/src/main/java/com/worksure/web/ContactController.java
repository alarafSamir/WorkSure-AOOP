package com.worksure.web;

import com.worksure.web.request.ContactRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/contact")
public class ContactController {
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody ContactRequest body) {
        String name = String.valueOf(body.getName()).trim();
        String email = String.valueOf(body.getEmail()).trim();
        String message = String.valueOf(body.getMessage()).trim();
        if (name.isBlank() || email.isBlank() || message.isBlank()) {
            throw new ApiException(400, "name, email and message are required");
        }
        System.out.println("Contact form submission: {name=" + body.getName()
                + ", email=" + body.getEmail() + ", message=" + body.getMessage() + "}");
        return ResponseEntity.status(201).body(ApiResponses.msg("Thank you for your message. We'll get back to you soon!"));
    }
}
