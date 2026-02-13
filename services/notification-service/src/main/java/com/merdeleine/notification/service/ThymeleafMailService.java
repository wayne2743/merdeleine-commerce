package com.merdeleine.notification.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class ThymeleafMailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public ThymeleafMailService(
            JavaMailSender mailSender,
            TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    public void sendThresholdReachMail(
            String to,
            String productName,
            String sellWindowName,
            String batchId,
            int currentQty,
            int thresholdQty,
            String reachedAt
    ) throws Exception {
        Context ctx = new Context();
        ctx.setVariable("productName", productName);
        ctx.setVariable("sellWindowName", sellWindowName);
        ctx.setVariable("batchId", batchId);
        ctx.setVariable("currentQty", currentQty);
        ctx.setVariable("thresholdQty", thresholdQty);
        ctx.setVariable("reachedAt", "2026-02-05 14:32");

        String html = templateEngine.process("batch-threshold-reached", ctx);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper =
                new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject("訂單成立通知");
        helper.setText(html, true);

        mailSender.send(message);
    }
}
