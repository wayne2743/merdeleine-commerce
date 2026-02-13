            package com.merdeleine.notification.service;

            import org.springframework.mail.SimpleMailMessage;
            import org.springframework.mail.javamail.JavaMailSender;
            import org.springframework.stereotype.Service;



            @Service
            public class GmailTestMailService {

                private final JavaMailSender mailSender;

                public GmailTestMailService(JavaMailSender mailSender) {
                    this.mailSender = mailSender;
                }

                public void sendTestMail(String toEmail) {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setTo(toEmail);
                    message.setSubject("Spring Boot Gmail SMTP 測試");
                    message.setText("如果你收到這封信，代表 SMTP 設定成功！");
                    message.setFrom("yourname@gmail.com");

                    mailSender.send(message);
                }
            }
