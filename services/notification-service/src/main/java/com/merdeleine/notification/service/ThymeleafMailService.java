package com.merdeleine.notification.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

@Service
public class ThymeleafMailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String from;

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

    public void sendHtml(String to, String subject, String templateKey, Map<String, Object> payload) {
        Context ctx = new Context(Locale.TAIWAN);
        ctx.setVariables(payload);

        String html = templateEngine.process(templateKey, ctx);

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Send email failed: " + e.getMessage(), e);
        }
    }
}
