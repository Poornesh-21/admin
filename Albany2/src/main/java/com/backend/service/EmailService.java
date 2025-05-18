//package com.backend.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.mail.SimpleMailMessage;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class EmailService {
//
//    private final JavaMailSender mailSender;
//
//    @Async
//    public void sendOtpEmail(String to, String otp) {
//        try {
//            SimpleMailMessage message = new SimpleMailMessage();
//            message.setTo(to);
//            message.setSubject("Your OTP for Albany Vehicle Service");
//            message.setText(String.format(
//                "Hello,\n\n" +
//                "Your OTP for Albany Vehicle Service is: %s\n\n" +
//                "This code will expire in 5 minutes.\n\n" +
//                "If you did not request this code, please ignore this email.\n\n" +
//                "Best regards,\n" +
//                "Albany Vehicle Service Team", otp));
//            
//            mailSender.send(message);
//            log.info("OTP email sent to: {}", to);
//        } catch (Exception e) {
//            log.error("Error sending OTP email to {}: {}", to, e.getMessage());
//            throw new RuntimeException("Failed to send OTP email: " + e.getMessage());
//        }
//    }
//
//    @Async
//    public void sendWelcomeEmail(String to, String name) {
//        try {
//            SimpleMailMessage message = new SimpleMailMessage();
//            message.setTo(to);
//            message.setSubject("Welcome to Albany Vehicle Service");
//            message.setText(String.format(
//                "Hello %s,\n\n" +
//                "Welcome to Albany Vehicle Service! Your account has been created successfully.\n\n" +
//                "You can now log in to your account using the OTP sent to your email.\n\n" +
//                "Best regards,\n" +
//                "Albany Vehicle Service Team", name));
//            
//            mailSender.send(message);
//            log.info("Welcome email sent to: {}", to);
//        } catch (Exception e) {
//            log.error("Error sending welcome email to {}: {}", to, e.getMessage());
//        }
//    }
//
////    @Async
////    public void sendBookingConfirmationEmail(String to, String name, String bookingId, String serviceDate, String serviceTypes) {
////        try {
////            SimpleMailMessage message = new SimpleMailMessage();
////            message.setTo(to);
////            message.setSubject("Your Service Booking Confirmation - Albany Vehicle Service");
////            message.setText(String.format(
////                "Hello %s,\n\n" +
////                "Thank you for booking with Albany Vehicle Service! Your service booking has been confirmed.\n\n" +
////                "Booking Details:\n" +
////                "Booking ID: %s\n" +
////                "Service Date: %s\n" +
////                "Service Types: %s\n\n" +
////                "Our team will contact you soon to confirm your appointment.\n\n" +
////                "Best regards,\n" +
////                "Albany Vehicle Service Team", name, bookingId, serviceDate, serviceTypes));
////            
////            mailSender.send(message);
////            log.info("Booking confirmation email sent to: {}", to);
////        } catch (Exception e) {
////            log.error("Error sending booking confirmation email to {}: {}", to, e.getMessage());
////            throw new RuntimeException("Failed to send booking confirmation email: " + e.getMessage());
////        }
//    @Async
//    public void sendBookingConfirmationEmail(String to, String name, String bookingId, String serviceDate, String serviceTypes) {
//        try {
//            log.info("Preparing to send booking confirmation email to: {}", to);
//            SimpleMailMessage message = new SimpleMailMessage();
//            message.setTo(to);
//            message.setSubject("Your Service Booking Confirmation - Albany Vehicle Service");
//            message.setText(String.format(
//                "Hello %s,\n\n" +
//                "Thank you for booking with Albany Vehicle Service! Your service booking has been confirmed.\n\n" +
//                "Booking Details:\n" +
//                "Booking ID: %s\n" +
//                "Service Date: %s\n" +
//                "Service Types: %s\n\n" +
//                "Our team will contact you soon to confirm your appointment.\n\n" +
//                "Best regards,\n" +
//                "Albany Vehicle Service Team", name, bookingId, serviceDate, serviceTypes));
//            
//            log.info("Attempting to send email with JavaMailSender");
//            mailSender.send(message);
//            log.info("Booking confirmation email sent successfully to: {}", to);
//        } catch (Exception e) {
//            log.error("Error sending booking confirmation email to {}: {}", to, e.getMessage(), e);
//            // Throw a custom exception but don't let it break the booking process
//            throw new RuntimeException("Failed to send booking confirmation email: " + e.getMessage());
//        }
//    }
//}

package com.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendOtpEmail(String to, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Your OTP for Albany Vehicle Service");
            message.setText(String.format(
                "Hello,\n\n" +
                "Your OTP for Albany Vehicle Service is: %s\n\n" +
                "This code will expire in 5 minutes.\n\n" +
                "If you did not request this code, please ignore this email.\n\n" +
                "Best regards,\n" +
                "Albany Vehicle Service Team", otp));
            
            mailSender.send(message);
            log.info("OTP email sent to: {}", to);
        } catch (Exception e) {
            log.error("Error sending OTP email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send OTP email: " + e.getMessage());
        }
    }

    @Async
    public void sendWelcomeEmail(String to, String name) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Welcome to Albany Vehicle Service");
            message.setText(String.format(
                "Hello %s,\n\n" +
                "Welcome to Albany Vehicle Service! Your account has been created successfully.\n\n" +
                "You can now log in to your account using the OTP sent to your email.\n\n" +
                "Best regards,\n" +
                "Albany Vehicle Service Team", name));
            
            mailSender.send(message);
            log.info("Welcome email sent to: {}", to);
        } catch (Exception e) {
            log.error("Error sending welcome email to {}: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendBookingConfirmationEmail(String to, String name, String bookingId, String serviceDate, String serviceTypes) {
        try {
            log.info("Preparing to send booking confirmation email to: {}", to);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Your Service Booking Confirmation - Albany Vehicle Service");
            message.setText(String.format(
                "Hello %s,\n\n" +
                "Thank you for booking with Albany Vehicle Service! Your service booking has been confirmed.\n\n" +
                "Booking Details:\n" +
                "Booking ID: %s\n" +
                "Service Date: %s\n" +
                "Service Types: %s\n\n" +
                "Our team will contact you soon to confirm your appointment.\n\n" +
                "Best regards,\n" +
                "Albany Vehicle Service Team", name, bookingId, serviceDate, serviceTypes));
            
            log.info("Attempting to send email with JavaMailSender");
            mailSender.send(message);
            log.info("Booking confirmation email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Error sending booking confirmation email to {}: {}", to, e.getMessage(), e);
            // Throw a custom exception but don't let it break the booking process
            throw new RuntimeException("Failed to send booking confirmation email: " + e.getMessage());
        }
    }
    
    /**
     * Sends a confirmation email for premium membership with an HTML membership card
     */
    @Async
    public void sendMembershipConfirmationEmail(String to, String name, String startDate, String endDate, String membershipCardHtml) {
        try {
            log.info("Preparing to send membership confirmation email to: {}", to);
            
            // Create a MIME message for HTML content
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject("Premium Membership Confirmation - Albany Vehicle Service");
            
            // Create HTML content with membership card
            String htmlContent = String.format(
                "<html>" +
                "<head>" +
                "  <style>" +
                "    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                "    .container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                "    .header { background-color: #722F37; color: white; padding: 10px 20px; text-align: center; }" +
                "    .content { padding: 20px; background-color: #f9f9f9; }" +
                "    .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #666; }" +
                "    .highlight { color: #722F37; font-weight: bold; }" +
                "    .benefits { margin: 20px 0; padding-left: 20px; }" +
                "  </style>" +
                "</head>" +
                "<body>" +
                "  <div class='container'>" +
                "    <div class='header'>" +
                "      <h1>Premium Membership Confirmed!</h1>" +
                "    </div>" +
                "    <div class='content'>" +
                "      <p>Hello %s,</p>" +
                "      <p>Thank you for upgrading to <span class='highlight'>Premium Membership</span> with Albany Vehicle Service!</p>" +
                "      <p>Your membership is now active and valid from <strong>%s</strong> until <strong>%s</strong>.</p>" +
                "      <p>As a Premium member, you now enjoy the following benefits:</p>" +
                "      <ul class='benefits'>" +
                "        <li><strong>30%% discount</strong> on all services</li>" +
                "        <li><strong>Priority service</strong> during peak hours</li>" +
                "        <li>Premium customer support</li>" +
                "        <li>Exclusive seasonal offers</li>" +
                "        <li>Free basic vehicle inspection twice a year</li>" +
                "      </ul>" +
                "      <p>Please find your digital membership card below:</p>" +
                "      %s" + // Membership card HTML
                "      <p>Save or print this card for your records. You can also access it anytime from your account dashboard.</p>" +
                "    </div>" +
                "    <div class='footer'>" +
                "      <p>If you have any questions, please contact our customer support at support@albany.com</p>" +
                "      <p>&copy; 2025 Albany Vehicle Service. All rights reserved.</p>" +
                "    </div>" +
                "  </div>" +
                "</body>" +
                "</html>",
                name, startDate, endDate, membershipCardHtml);
            
            helper.setText(htmlContent, true);
            
            mailSender.send(mimeMessage);
            log.info("Membership confirmation email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Error sending membership confirmation email to {}: {}", to, e.getMessage(), e);
        }
    }
}