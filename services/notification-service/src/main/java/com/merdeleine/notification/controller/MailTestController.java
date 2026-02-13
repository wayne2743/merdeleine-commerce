package com.merdeleine.notification.controller;

import com.merdeleine.notification.service.GmailTestMailService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MailTestController {

    private final GmailTestMailService mailService;

    public MailTestController(GmailTestMailService mailService) {
        this.mailService = mailService;
    }

    @GetMapping("/api/test/mail")
    public String sendTestMail(@RequestParam String to) {
        mailService.sendTestMail(to);
        return "Mail sent to: " + to;
    }
}
