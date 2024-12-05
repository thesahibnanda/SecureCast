package org.captaintree.utils;

import lombok.extern.slf4j.Slf4j;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

@Slf4j
public class MailUtils {

    public static void sendEmail(String to, String subject, String body) {
        String from = System.getenv("EMAIL_USERNAME");
        final String username = System.getenv("EMAIL_USERNAME");
        final String password = System.getenv("EMAIL_PASSWORD");

        String host = "smtp.gmail.com";


        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", "587");


        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setContent(body, "text/plain");
            Transport.send(message);

            log.info("Email sent successfully!");
        } catch (MessagingException e) {
            log.info(e.fillInStackTrace().getLocalizedMessage());
        }
    }
}
