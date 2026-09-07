package com.saipbuilds.inventepayment2026.TicketEmailService;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketEmailSenderService {

    private final JavaMailSender mailSender;

    private static final String THEME_COLOR = "#dc8400";         // Vibrant Tech Orange
    private static final String THEME_DARK = "#1A1A1A";          // Premium Dark Charcoal
    private static final String BG_LIGHT = "#F8F9FA";          // Modern Clean Canvas Background
    private static final String CARD_BG = "#FFFFFF";           // Pure White Card Background
    private static final String BORDER_COLOR = "#E2E8F0";      // Soft Border Divider Color
    private static final String TEXT_MUTED = "#64748B";          // Slate Muted Text Color

    public void sendTicketPurchaseMail(Map<String, Object> userDetails, List<Map<String, Object>> events, UUID ticketId) {
        if (!"on".equalsIgnoreCase(System.getenv("EMAIL_KILLSWITCH"))) {
            log.info("EMAIL_KILLSWITCH is off. Skipping email for {}", userDetails.get("email"));
            return;
        }

        String recipientEmail = (String) userDetails.get("email");

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(System.getenv("MAIL_ID"),"Invente 2026 Passes");
            helper.setTo(recipientEmail);
            helper.setSubject("Your Invente 2026 Entry Pass is here!");
            helper.setReplyTo(System.getenv("MAIL_ID"), "Invente 2026 Support");
            helper.setSentDate(new Date());

            String htmlContent = generateTicketDetailsHtml(userDetails, events, ticketId);
            helper.setText(htmlContent, true);

            byte[] qrCodeBytes = generateQRCodeBytes(ticketId.toString());
            helper.addInline("qrImage", new ByteArrayResource(qrCodeBytes), "image/png");
            try {
                org.springframework.core.io.ClassPathResource logoResource = new org.springframework.core.io.ClassPathResource("invente-orange.png");
                byte[] logoBytes = logoResource.getInputStream().readAllBytes();
                helper.addInline("logoImage", new ByteArrayResource(logoBytes), "image/png");
            } catch (Exception e) {
                log.warn("Could not attach local orange logo inline, falling back or failing. Error: {}", e.getMessage());
            }
            mailSender.send(message);
            log.info("Successfully sent ticket email to {}", recipientEmail);

        } catch (MessagingException | WriterException | IOException e) {
            throw new RuntimeException("Failed to send email to " + recipientEmail, e);
        }
    }

    public void sendHackathonTicketMail(Map<String, Object> paymentDetails, Map<String, Object> teamDetails, List<Map<String, Object>> members, UUID ticketId) {
        if (!"on".equalsIgnoreCase(System.getenv("EMAIL_KILLSWITCH"))) {
            log.info("EMAIL_KILLSWITCH is off. Skipping hackathon email for team {}", teamDetails.get("team_name"));
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(System.getenv("MAIL_ID"),"HackInfinity 2026 Passes");

            String[] recipientEmails = members.stream()
                    .map(m -> (String) m.get("email"))
                    .toArray(String[]::new);

            helper.setTo(recipientEmails);
            helper.setSubject("Your HackInfinity 2026 Confirmation - Team: " + teamDetails.get("team_name"));

            helper.setReplyTo(System.getenv("MAIL_ID"), "Invente 2026 Support");
            helper.setSentDate(new Date());

            String htmlContent = generateHackathonHtml(paymentDetails, teamDetails, members, ticketId);
            helper.setText(htmlContent, true);

            byte[] qrCodeBytes = generateQRCodeBytes(ticketId.toString());
            helper.addInline("qrImage", new ByteArrayResource(qrCodeBytes), "image/png");
            try {
                org.springframework.core.io.ClassPathResource logoResource = new org.springframework.core.io.ClassPathResource("invente-orange.png");
                byte[] logoBytes = logoResource.getInputStream().readAllBytes();
                helper.addInline("logoImage", new ByteArrayResource(logoBytes), "image/png");
            } catch (Exception e) {
                log.warn("Could not attach local orange logo inline to hackathon email. Error: {}", e.getMessage());
            }
            mailSender.send(message);
            log.info("Successfully sent hackathon email to team {} ({} recipients)", teamDetails.get("team_name"), recipientEmails.length);

        } catch (MessagingException | WriterException | IOException e) {
            throw new RuntimeException("Failed to send hackathon email for ticket " + ticketId, e);
        }
    }

    public void sendPaymentReminderMail(String recipientEmail, UUID ticketId) {
        if (!"on".equalsIgnoreCase(System.getenv("EMAIL_KILLSWITCH"))) {
            log.info("EMAIL_KILLSWITCH is off. Skipping reminder email for {}", recipientEmail);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(System.getenv("MAIL_ID"), "Invente 2026 Registrations");
            helper.setTo(recipientEmail);
            helper.setSubject("Action Required: Upload Payment Proof for Invente 2026");
            helper.setReplyTo(System.getenv("MAIL_ID"), "Invente 2026 Support");
            helper.setSentDate(new Date());

            String uploadLink = System.getenv("BASE_UPLOAD_URL") + "/" + ticketId.toString();
            String htmlContent = generatePaymentReminderHtml(uploadLink, ticketId);

            helper.setText(htmlContent, true);

            try {
                org.springframework.core.io.ClassPathResource logoResource = new org.springframework.core.io.ClassPathResource("invente-orange.png");
                byte[] logoBytes = logoResource.getInputStream().readAllBytes();
                helper.addInline("logoImage", new ByteArrayResource(logoBytes), "image/png");
            } catch (Exception e) {
                log.warn("Could not attach local orange logo inline to reminder email. Error: {}", e.getMessage());
            }

            mailSender.send(message);
            log.info("Successfully sent payment reminder email to {}", recipientEmail);

        } catch (MessagingException | IOException e) {
            throw new RuntimeException("Failed to send payment reminder email to " + recipientEmail, e);
        }
    }
    public void sendPaymentRejectionMail(String recipientEmail, UUID ticketId) {
        if (!"on".equalsIgnoreCase(System.getenv("EMAIL_KILLSWITCH"))) {
            log.info("EMAIL_KILLSWITCH is off. Skipping rejection email for {}", recipientEmail);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(System.getenv("MAIL_ID"), "Invente 2026 Registrations Team");
            helper.setTo(recipientEmail);
            helper.setSubject("UPDATE ON YOUR PASS: Your Payment Has Been Rejected - Invente 2026");
            helper.getMimeMessage().setReplyTo(new Address[] {
                    new InternetAddress("kathirezhil2310288@ssn.edu.in", "Kathir Ezhil"),
                    new InternetAddress("bharath2310957@ssn.edu.in", "Bharath Ram S K")
            });
            helper.setSentDate(new Date());

            
            String htmlContent = generatePaymentRejectionHtml(ticketId);

            helper.setText(htmlContent, true);


            mailSender.send(message);
            log.info("Successfully sent payment rejection email to {}", recipientEmail);

        } catch (MessagingException | IOException e) {
            throw new RuntimeException("Failed to send payment rejection email to " + recipientEmail, e);
        }
    }
    private String generatePaymentRejectionHtml(UUID ticketId) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
                .header { background-color: #d9534f; color: white; padding: 15px; text-align: center; border-radius: 5px 5px 0 0; }
                .header h2 { margin: 0; font-size: 20px; }
                .content { background-color: #f9f9f9; padding: 20px; border: 1px solid #ddd; border-top: none; border-radius: 0 0 5px 5px; }
                .ticket-id { font-family: monospace; background: #eee; padding: 3px 6px; border-radius: 3px; font-weight: bold; }
                .contact-box { background-color: #fff; border-left: 4px solid #d9534f; padding: 15px; margin: 20px 0; border-radius: 3px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
                .contact-person { margin-bottom: 15px; }
                .contact-person:last-child { margin-bottom: 0; }
                .contact-person a { color: #d9534f; text-decoration: none; }
                .footer { text-align: center; margin-top: 20px; font-size: 0.9em; color: #777; }
            </style>
        </head>
        <body>
            <div class="header">
                <h2>Payment Verification Failed</h2>
            </div>
            <div class="content">
                <p>Dear Participant,</p>
                
                <p>We are writing to inform you that the payment proof submitted for your registration (Ticket ID: <span class="ticket-id">%s</span>) has been rejected.</p>
                
                <p>After a manual verification by our volunteer team, we were unable to validate your transaction based on the proof provided. This could be due to but not limited to an improper PDF, mismatched transaction ID, or an incomplete transaction.</p>
                
                <p>To resolve this issue or if you believe this is an error, please reach out to our support team immediately with a clear copy of your payment receipt:</p>
                <p>Please <b>REPLY ALL</b> to this email with your updated payment proof (without changing the subject line and recipient) or contact us directly via the details below.</p>
                
                <div class="contact-box">
                    <div class="contact-person">
                        <strong>Kathir Ezhil</strong><br>
                        Phone / WhatsApp: <a href="tel:+917550254009">+91 75502 54009</a><br>
                        Email: <a href="mailto:kathirezhil2310288@ssn.edu.in">kathirezhil2310288@ssn.edu.in</a>
                    </div>
                    <div class="contact-person">
                        <strong>Bharath Ram S K</strong><br>
                        Phone / WhatsApp: <a href="tel:+918825992601">+91 88259 92601</a><br>
                        Email: <a href="mailto:bharath2310957@ssn.edu.in">bharath2310957@ssn.edu.in</a>
                    </div>
                </div>
                
                <p>Please ensure you include your Ticket ID in all communications.</p>
                
                <p>Best regards,<br><strong>SSN Invente Team</strong></p>
            </div>
            <div class="footer">
                &copy; 2026 SSN Invente. All rights reserved.
            </div>
        </body>
        </html>
        """.formatted(ticketId.toString());
    }

    private byte[] generateQRCodeBytes(String data) throws WriterException, IOException {
        // 1. Render at higher resolution (Retina/HiDPI crispness)
        int qrSize = 800;
        int padding = 40;
        int totalWidth = qrSize + (padding * 2);
        int totalHeight = qrSize + (padding * 2);

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H); // 30% recovery tolerance
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, qrSize, qrSize, hints);

        // Use ARGB or RGB with proper rendering buffer
        BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

        // 2. Embed Crisp Local Logo
        try {
            org.springframework.core.io.ClassPathResource resource =
                    new org.springframework.core.io.ClassPathResource("invente-black.png");
            BufferedImage logo = ImageIO.read(resource.getInputStream());

            if (logo != null) {
                Graphics2D gLogo = qrImage.createGraphics();

                // Set maximum rendering fidelity hints
                gLogo.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                gLogo.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                gLogo.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                gLogo.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
                gLogo.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);

                // Safe size ratio (20-22% keeps QR read rates near 100%)
                int logoWidth = (int) (qrSize * 0.22);
                int logoHeight = (int) (logoWidth * ((double) logo.getHeight() / logo.getWidth()));
                int lx = (qrSize - logoWidth) / 2;
                int ly = (qrSize - logoHeight) / 2;

                int pillPadding = 16;

                // Draw crisp white background plate with rounded edges
                gLogo.setColor(Color.WHITE);
                gLogo.fillRoundRect(
                        lx - pillPadding,
                        ly - pillPadding,
                        logoWidth + (pillPadding * 2),
                        logoHeight + (pillPadding * 2),
                        28, 28
                );

                // Optional subtle outline to separate logo plate cleanly from QR modules
                gLogo.setColor(new Color(226, 232, 240)); // Slate border #E2E8F0
                gLogo.setStroke(new BasicStroke(2f));
                gLogo.drawRoundRect(
                        lx - pillPadding,
                        ly - pillPadding,
                        logoWidth + (pillPadding * 2),
                        logoHeight + (pillPadding * 2),
                        28, 28
                );

                // High-quality smooth scaled draw
                gLogo.drawImage(logo, lx, ly, logoWidth, logoHeight, null);
                gLogo.dispose();
            }
        } catch (Exception e) {
            log.warn("Could not embed local logo into QR code, falling back to standard QR. Error: {}", e.getMessage());
        }

        // 3. Assemble Outer Card Canvas
        BufferedImage cardImage = new BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = cardImage.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Canvas Background
        g2d.setColor(Color.WHITE);
        g2d.fillRoundRect(0, 0, totalWidth, totalHeight, 36, 36);

        // Frame Border
        g2d.setColor(Color.decode(THEME_COLOR));
        g2d.setStroke(new BasicStroke(6f));
        g2d.drawRoundRect(2, 2, totalWidth - 4, totalHeight - 4, 36, 36);

        // Draw centered QR image
        g2d.drawImage(qrImage, padding, padding, null);
        g2d.dispose();

        // 4. Output as PNG
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        ImageIO.write(cardImage, "PNG", pngOutputStream);
        return pngOutputStream.toByteArray();
    }


    private String generatePaymentReminderHtml(String uploadLink, UUID ticketId) {
        return String.format("""
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset='utf-8'>
        <meta name='viewport' content='width=device-width, initial-scale=1.0'>
    </head>
    <body style='margin:0; padding:0; background-color:%s; font-family:"Segoe UI", Helvetica, Arial, sans-serif;'>
        <table role='presentation' width='100%%' cellspacing='0' cellpadding='0' style='background-color:%s; padding:40px 0;'>
            <tr>
                <td align='center'>
                    <table role='presentation' width='600' cellspacing='0' cellpadding='0' style='background-color:%s; border-radius:16px; overflow:hidden; box-shadow:0 10px 40px rgba(0,0,0,0.06); border:1px solid %s;'>
                        
                        <tr>
                            <td style='height:6px; background:linear-gradient(90deg, %s, #FFA726);'></td>
                        </tr>
                        
                        <tr>
                            <td style='padding:45px 40px;'>
                                
                                <table role='presentation' width='100%%' cellspacing='0' cellpadding='0'>
                                    <tr>
                                        <td align='center' style='padding-bottom:30px;'>
                                            <img src='cid:logoImage' alt='Invente Logo' width='170' style='display:block; border:0;'>
                                        </td>
                                    </tr>
                                </table>
                                
                                <div style='text-align:center; margin-bottom:35px;'>
                                    <h1 style='color:%s; font-size:24px; font-weight:700; margin:0 0 12px 0; letter-spacing:-0.5px;'>Action Required</h1>

                                    <p style='font-size:15px; color:%s; margin:0 0 25px 0; line-height:1.6;'>
                                        Please upload the payment receipt PDF from <strong>RazorPay</strong> that was sent to your email inbox to complete your registration.
                                    </p>

                                    <!-- Informational Context Box -->
                                    <div style='background-color:%s; border:1px solid %s; border-radius:10px; padding:16px 20px; display:inline-block; text-align:left; max-width:85%%;'>
                                        <p style='font-size:13px; color:%s; margin:0 0 10px 0; line-height:1.6;text-align:center;'>
                                            <strong style='color:%s;'>⏱ Processing:</strong> You will receive your Event Pass within <strong>3-4 working days</strong> after submitting your payment proof.
                                        </p>

                                        <div style='border-top:1px dashed %s; margin:10px 0; height:1px;'></div>

                                        <p style='font-size:12px; color:%s; margin:0; line-height:1.5; font-style:italic; text-align:center;'>
                                            Ignore this email if you have already completed this step.
                                        </p>
                                    </div>
                                </div>
                                
                                <!-- Call to Action Box -->
                                <table role='presentation' width='100%%' cellspacing='0' cellpadding='0' style='background-color:%s; border:1px solid %s; border-radius:10px; margin-bottom:30px; text-align:center;'>
                                    <tr>
                                        <td style='padding:30px 24px;'>
                                            <a href='%s' style='display:inline-block; padding:14px 28px; background-color:%s; color:#FFFFFF; text-decoration:none; font-weight:600; border-radius:8px; font-size:14px; box-shadow:0 4px 12px rgba(220,132,0,0.25);'>Upload Payment Proof</a>
                                        </td>
                                    </tr>
                                </table>
                                
                                <div style='text-align:center; margin-bottom:35px;'>
                                    <p style='font-size:13px; color:%s; margin:0;'>Your Ticket ID: <br><strong style='color:%s; font-family:Consolas, monospace;'>%s</strong></p>
                                </div>
                        
                                <div style='text-align:center; border-top:1px solid %s; padding-top:25px;'>
                                    <p style='color:%s; font-size:12px; line-height:1.6; margin:0 0 10px 0;'>
                                        Sent by <strong>Invente 2026 Payment System</strong>.<br>
                                        Payment System Built by <a href='https://www.linkedin.com/in/saipranav-m/' target='_blank' style='color:%s; text-decoration:none; font-weight:600;'>Saipranav M</a>.<br>
                                        Attendance System Built by <a href='https://www.linkedin.com/in/pranav-vijay-524410329/' target='_blank' style='color:%s; text-decoration:none; font-weight:600;'>Pranav Vijay</a> and <a href='https://linkedin.com/in/pranav-krishna-p' target='_blank' style='color:%s; text-decoration:none; font-weight:600;'>Pranav Krishna</a>.
                                    </p>
                                </div>
                                
                            </td>
                        </tr>
                    </table>
                </td>
            </tr>
        </table>
    </body>
    </html>
    """,
                // 1-4: Body & Main Tables
                BG_LIGHT, BG_LIGHT, CARD_BG, BORDER_COLOR,
                // 5: Top Gradient
                THEME_COLOR,
                // 6-7: Main Headers
                THEME_DARK, TEXT_MUTED,
                // 8-13: Informational Context Box
                BG_LIGHT, BORDER_COLOR, TEXT_MUTED, THEME_DARK, BORDER_COLOR, TEXT_MUTED,
                // 14-15: CTA Wrapper Table
                BG_LIGHT, BORDER_COLOR,
                // 16-17: CTA Button
                uploadLink, THEME_COLOR,
                // 18-20: Ticket ID
                TEXT_MUTED, THEME_DARK, ticketId.toString(),
                // 21-25: Footer Lines & Links
                BORDER_COLOR, TEXT_MUTED, THEME_COLOR, THEME_COLOR, THEME_COLOR
        );
    }
    private String generateTicketDetailsHtml(Map<String, Object> userDetails, List<Map<String, Object>> events, UUID ticketId) {
        StringBuilder eventsTableHtml = new StringBuilder();

        if (events != null && !events.isEmpty()) {
            for (Map<String, Object> event : events) {
                eventsTableHtml.append(String.format("""
                    <tr>
                        <td style='padding:12px 16px; border-bottom:1px solid %s; color:%s; font-size:13px;'>%s</td>
                        <td style='padding:12px 16px; border-bottom:1px solid %s; color:%s; font-weight:600; font-size:13px;'>%s</td>
                        <td style='padding:12px 16px; border-bottom:1px solid %s; color:%s; font-size:13px;'>%s</td>
                    </tr>
                """,
                        BORDER_COLOR, TEXT_MUTED, event.get("event_id").toString(),
                        BORDER_COLOR, THEME_DARK, event.get("event_name").toString(),
                        BORDER_COLOR, TEXT_MUTED, event.get("dept_name").toString()));
            }
        } else {
            eventsTableHtml.append(String.format("<tr><td colspan='3' style='padding:20px; text-align:center; color:%s; font-style:italic; font-size:13px;'>No specific event slots bound (General Campus Entry).</td></tr>", TEXT_MUTED));
        }

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset='utf-8'>
                <meta name='viewport' content='width=device-width, initial-scale=1.0'>
            </head>
            <body style='margin:0; padding:0; background-color:%s; font-family:"Segoe UI", Helvetica, Arial, sans-serif;'>
                <table role='presentation' width='100%%' cellspacing='0' cellpadding='0' style='background-color:%s; padding:40px 0;'>
                    <tr>
                        <td align='center'>
                            <table role='presentation' width='600' cellspacing='0' cellpadding='0' style='background-color:%s; border-radius:16px; overflow:hidden; box-shadow:0 10px 40px rgba(0,0,0,0.06); border:1px solid %s;'>
                                
                                <!-- Decorative Top Color Bar -->
                                <tr>
                                    <td style='height:6px; background:linear-gradient(90deg, %s, #FFA726);'></td>
                                </tr>
                                
                                <!-- Main Content Area -->
                                <tr>
                                    <td style='padding:45px 40px;'>
                                        
                                        <!-- Logo Header -->
                                        <table role='presentation' width='100%%' cellspacing='0' cellpadding='0'>
                                            <tr>
                                                <td align='center' style='padding-bottom:30px;'>
                                                    <img src='cid:logoImage' alt='Invente Logo' width='170' style='display:block; border:0;'>
                                                </td>
                                            </tr>
                                        </table>
                                        
                                        <!-- Heading Message -->
                                        <div style='text-align:center; margin-bottom:35px;'>
                                            <h1 style='color:%s; font-size:22px; font-weight:700; margin:0 0 10px 0; letter-spacing:-0.5px;'>Registration Confirmed!</h1>
                                            <p style='font-size:15px; color:%s; margin:0; line-height:1.5;'>Hello <strong style='color:%s;'>%s</strong>, your transaction was successful. We are thrilled to host you at Invente 2026.</p>
                                        </div>
                                
                                        <!-- Transaction Summary Box -->
                                        <table role='presentation' width='100%%' cellspacing='0' cellpadding='0' style='background-color:%s; border:1px solid %s; border-radius:10px; margin-bottom:35px;'>
                                            <tr>
                                                <td style='padding:20px 24px;'>
                                                    <table role='presentation' width='100%%' cellspacing='0' cellpadding='0' style='font-size:14px; color:%s; line-height:1.8;'>
                                                        <tr>
                                                            <td style='color:%s; width:35%%;'>Ticket ID</td>
                                                            <td style='color:%s; font-weight:600; font-family:Consolas, monospace;'>%s</td>
                                                        </tr>
                                                        <tr>
                                                            <td style='color:%s;'>Pass Category</td>
                                                            <td style='color:%s; font-weight:600;'>%s</td>
                                                        </tr>
                                                        <tr>
                                                            <td style='color:%s;'>Amount</td>
                                                            <td style='color:%s; font-weight:600;'>₹%s</td>
                                                        </tr>
                                                        <tr>
                                                            <td style='color:%s;'>Issued Timestamp</td>
                                                            <td style='color:%s;'>%s</td>
                                                        </tr>
                                                    </table>
                                                </td>
                                            </tr>
                                        </table>
                                
                                        <!-- QR Pass Container -->
                                        <div style='border:1px solid %s; border-radius:12px; background-color:#FFFFFF; padding:30px 20px; text-align:center; margin-bottom:35px;'>
                                            <h3 style='color:%s; font-size:16px; font-weight:600; margin:0 0 8px 0;'>Your Digital Entry Pass</h3>
                                            <p style='font-size:13px; color:%s; margin:0 0 20px 0;'>Save this pass for verification.</p>
                                            
                                            <div style='display:inline-block; padding:12px; background:#FFFFFF; border:1px solid %s; border-radius:12px; box-shadow:0 4px 15px rgba(0,0,0,0.03);'>
                                                <img src='cid:qrImage' alt='Entry QR Code' width='250' height='250' style='display:block; border-radius:8px;'>
                                            </div>
                                            
                                            <!-- Booked Sessions Subtable -->
                                            <div style='margin-top:30px; text-align:left;'>
                                                <h4 style='color:%s; font-size:14px; font-weight:600; margin:0 0 12px 4px;'>Events Reserved</h4>
                                                <div style='border:1px solid %s; border-radius:8px; overflow:hidden;'>
                                                    <table role='presentation' width='100%%' cellspacing='0' cellpadding='0' style='border-collapse:collapse;'>
                                                        <thead>
                                                            <tr style='background-color:%s;'>
                                                                <th style='padding:10px 16px; text-align:left; color:%s; font-size:12px; font-weight:600; border-bottom:2px solid %s;'>Event ID</th>
                                                                <th style='padding:10px 16px; text-align:left; color:%s; font-size:12px; font-weight:600; border-bottom:2px solid %s;'>Event Title</th>
                                                                <th style='padding:10px 16px; text-align:left; color:%s; font-size:12px; font-weight:600; border-bottom:2px solid %s;'>Department</th>
                                                            </tr>
                                                        </thead>
                                                        <tbody>
                                                            %s
                                                        </tbody>
                                                    </table>
                                                </div>
                                            </div>
                                        </div>
                                
                                        <!-- Footer Disclaimer -->
                                        <div style='text-align:center; border-top:1px solid %s; padding-top:25px;'>
                                            <p style='color:%s; font-size:12px; line-height:1.6; margin:0 0 10px 0;'>
                                                Sent by <strong>Invente 2026 Payment System</strong>.<br>
                                                Payment System Built by <a href='https://www.linkedin.com/in/saipranav-m/' target='_blank' style='color:%s; text-decoration:none; font-weight:600;'>Saipranav M</a>.
                                                Attendance System Built by <a href='https://www.linkedin.com/in/pranav-vijay-524410329/' target='_blank' text-decoration:none; font-weight:600;'>Pranav Vijay </a> and <a href='https://linkedin.com/in/pranav-krishna-p' target='_blank' text-decoration:none; font-weight:600;'>Pranav Krishna </a>.
                                            </p>
                                            <p style='color:#94A3B8; font-size:11px; margin:0;'>
                                                Please present the QR badge directly at the venue scanner checkpoints.
                                            </p>
                                        </div>
                                        
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """,
                BG_LIGHT, BG_LIGHT, CARD_BG, BORDER_COLOR,
                THEME_COLOR,

                THEME_DARK, TEXT_MUTED, THEME_DARK, userDetails.get("name"),
                BG_LIGHT, BORDER_COLOR,
                TEXT_MUTED,
                TEXT_MUTED, THEME_DARK, ticketId.toString(),
                TEXT_MUTED, THEME_DARK, userDetails.get("ticket_type"),
                TEXT_MUTED, THEME_DARK, userDetails.get("amount_paid"),
                TEXT_MUTED, THEME_DARK, userDetails.get("paid_on"),
                BORDER_COLOR,
                THEME_DARK, TEXT_MUTED, BORDER_COLOR,
                THEME_DARK, BORDER_COLOR, BG_LIGHT, TEXT_MUTED, BORDER_COLOR, TEXT_MUTED, BORDER_COLOR, TEXT_MUTED, BORDER_COLOR,
                eventsTableHtml.toString(),
                BORDER_COLOR, TEXT_MUTED, THEME_COLOR
        );
    }

    private String generateHackathonHtml(Map<String, Object> paymentDetails, Map<String, Object> teamDetails, List<Map<String, Object>> members, UUID ticketId) {
        StringBuilder membersHtml = new StringBuilder();
        for (Map<String, Object> member : members) {
            String roleHtml = Boolean.TRUE.equals(member.get("is_lead"))
                    ? String.format("<span style='background-color:%s; color:#FFFFFF; padding:3px 8px; border-radius:4px; font-size:11px; font-weight:700; letter-spacing:0.3px;'>TEAM LEAD</span>", THEME_COLOR)
                    : String.format("<span style='color:%s; font-size:12px;'>Member</span>", TEXT_MUTED);

            membersHtml.append(String.format("""
                <tr>
                    <td style='padding:12px 16px; border-bottom:1px solid %s;'><strong style='color:%s; font-size:13px;'>%s</strong><br>%s</td>
                    <td style='padding:12px 16px; border-bottom:1px solid %s; color:%s; font-size:13px;'>%s</td>
                    <td style='padding:12px 16px; border-bottom:1px solid %s; color:%s; font-size:13px;'>%s</td>
                    <td style='padding:12px 16px; border-bottom:1px solid %s; color:%s; font-size:13px;'>Year %s</td>
                </tr>
            """,
                    BORDER_COLOR, THEME_DARK, member.get("name"), roleHtml,
                    BORDER_COLOR, TEXT_MUTED, member.get("email"),
                    BORDER_COLOR, TEXT_MUTED, member.get("phno") != null ? member.get("phno") : "N/A",
                    BORDER_COLOR, TEXT_MUTED, member.get("year_of_study")));
        }

        String psDescription = (String) teamDetails.get("ps_description");
        String psHtml = (psDescription != null && !psDescription.isEmpty())
                ? String.format("""
                    <table role='presentation' width='100%%' cellspacing='0' cellpadding='0' style='background-color:%s; border-left:4px solid %s; border-radius:0 8px 8px 0; margin-bottom:30px;'>
                        <tr>
                            <td style='padding:16px 20px; font-size:13px; color:%s; line-height:1.6;'>
                                <strong style='color:%s; display:block; margin-bottom:6px; font-size:14px;'>Selected Problem Statement:</strong>
                                %s
                            </td>
                        </tr>
                    </table>
                """, BG_LIGHT, THEME_COLOR, THEME_DARK, THEME_DARK, psDescription.replace("\n", "<br>"))
                : "";

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset='utf-8'>
                <meta name='viewport' content='width=device-width, initial-scale=1.0'>
            </head>
            <body style='margin:0; padding:0; background-color:%s; font-family:"Segoe UI", Helvetica, Arial, sans-serif;'>
                <table role='presentation' width='100%%' cellspacing='0' cellpadding='0' style='background-color:%s; padding:40px 0;'>
                    <tr>
                        <td align='center'>
                            <table role='presentation' width='620' cellspacing='0' cellpadding='0' style='background-color:%s; border-radius:16px; overflow:hidden; box-shadow:0 10px 40px rgba(0,0,0,0.06); border:1px solid %s;'>
                                
                                <!-- Decorative Top Color Bar -->
                                <tr>
                                    <td style='height:6px; background:linear-gradient(90deg, %s, #FFA726);'></td>
                                </tr>
                                
                                <!-- Main Content Area -->
                                <tr>
                                    <td style='padding:45px 40px;'>
                                        
                                        <!-- Logo Header -->
                                        <table role='presentation' width='100%%' cellspacing='0' cellpadding='0'>
                                            <tr>
                                                <td align='center' style='padding-bottom:30px;'>
                                                    <img src='cid:logoImage' alt='Invente Logo' width='170' style='display:block; border:0;'>
                                                </td>
                                            </tr>
                                        </table>
                                        
                                        <!-- Heading Message -->
                                        <div style='text-align:center; margin-bottom:35px;'>
                                            <h1 style='color:%s; font-size:22px; font-weight:700; margin:0 0 10px 0; letter-spacing:-0.5px;'>HackInfinity 2026 Confirmed!</h1>
                                            <p style='font-size:15px; color:%s; margin:0; line-height:1.5;'>Welcome aboard, Team <strong style='color:%s;'>%s</strong>.</p>
                                        </div>
                                
                                        <!-- Team Meta Overview Box -->
                                        <table role='presentation' width='100%%' cellspacing='0' cellpadding='0' style='background-color:%s; border:1px solid %s; border-radius:10px; margin-bottom:25px;'>
                                            <tr>
                                                <td style='padding:20px 24px;'>
                                                    <table role='presentation' width='100%%' cellspacing='0' cellpadding='0' style='font-size:14px; color:%s; line-height:1.8;'>
                                                        <tr>
                                                            <td style='color:%s; width:35%%;'>Challenge Domain</td>
                                                            <td style='color:%s; font-weight:600;'>%s</td>
                                                        </tr>
                                                        <tr>
                                                            <td style='color:%s;'>Challenge Track</td>
                                                            <td style='color:%s; font-weight:600;'>%s</td>
                                                        </tr>
                                                        <tr>
                                                            <td style='color:%s;'>Ticket ID</td>
                                                            <td style='color:%s; font-family:Consolas, monospace;'>%s</td>
                                                        </tr>
                                                        <tr>
                                                            <td style='color:%s;'>Total Contribution</td>
                                                            <td style='color:%s; font-weight:600;'>₹%s</td>
                                                        </tr>
                                                        <tr>
                                                            <td style='color:%s;'>Verification Date</td>
                                                            <td style='color:%s;'>%s</td>
                                                        </tr>
                                                    </table>
                                                </td>
                                            </tr>
                                        </table>
                                        
                                        %s
                                
                                        <!-- Team QR Container -->
                                        <div style='border:1px solid %s; border-radius:12px; background-color:#FFFFFF; padding:30px 20px; text-align:center; margin-bottom:35px;'>
                                            <h3 style='color:%s; font-size:16px; font-weight:600; margin:0 0 6px 0;'>Team Entry Pass</h3>
                                            <p style='font-size:13px; color:%s; margin:0 0 20px 0;'>Present this QR code for the entire team's event access.</p>
                                            
                                            <div style='display:inline-block; padding:12px; background:#FFFFFF; border:1px solid %s; border-radius:12px; box-shadow:0 4px 15px rgba(0,0,0,0.03);'>
                                                <img src='cid:qrImage' alt='Team QR Pass' width='240' height='240' style='display:block; border-radius:8px;'>
                                            </div>
                                        </div>
                                        
                                        <!-- Team Roster Table Section -->
                                        <h3 style='color:%s; font-size:16px; font-weight:600; margin:0 0 12px 4px;'>Your Team Members</h3>
                                        <div style='border:1px solid %s; border-radius:8px; overflow:hidden; margin-bottom:35px;'>
                                            <table role='presentation' width='100%%' cellspacing='0' cellpadding='0' style='border-collapse:collapse;'>
                                                <thead>
                                                    <tr style='background-color:%s;'>
                                                        <th style='padding:12px 16px; text-align:left; color:%s; font-size:12px; font-weight:600; border-bottom:2px solid %s;'>Member & Role</th>
                                                        <th style='padding:12px 16px; text-align:left; color:%s; font-size:12px; font-weight:600; border-bottom:2px solid %s;'>Email Address</th>
                                                        <th style='padding:12px 16px; text-align:left; color:%s; font-size:12px; font-weight:600; border-bottom:2px solid %s;'>Contact Phone</th>
                                                        <th style='padding:12px 16px; text-align:left; color:%s; font-size:12px; font-weight:600; border-bottom:2px solid %s;'>Year</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    %s
                                                </tbody>
                                            </table>
                                        </div>
                                
                                        <!-- Footer Disclaimer -->
                                        <div style='text-align:center; border-top:1px solid %s; padding-top:25px;'>
                                            <p style='color:%s; font-size:12px; line-height:1.6; margin:0 0 10px 0;'>
                                                Sent by <strong>Invente 2026 Payment System</strong>.<br>
                                                Payment System Built by <a href='https://www.linkedin.com/in/saipranav-m/' target='_blank' style='color:%s; text-decoration:none; font-weight:600;'>Saipranav M</a>.
                                                Attendance System Built by <a href='https://www.linkedin.com/in/pranav-vijay-524410329/' target='_blank' text-decoration:none; font-weight:600;'>Pranav Vijay </a> and <a href='https://linkedin.com/in/pranav-krishna-p' target='_blank' text-decoration:none; font-weight:600;'>Pranav Krishna </a>.
                                            </p>
                                            <p style='color:#94A3B8; font-size:11px; margin:0;'>
                                                Please carry valid college identification cards alongside this digital pass.
                                            </p>
                                        </div>
                                        
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """,
                BG_LIGHT, BG_LIGHT, CARD_BG, BORDER_COLOR,
                THEME_COLOR,

                THEME_DARK, TEXT_MUTED, THEME_COLOR, teamDetails.get("team_name"),
                BG_LIGHT, BORDER_COLOR,
                TEXT_MUTED,
                TEXT_MUTED, THEME_DARK, teamDetails.get("domain"),
                TEXT_MUTED, THEME_DARK, teamDetails.get("track"),
                TEXT_MUTED, THEME_DARK, ticketId.toString(),
                TEXT_MUTED, THEME_DARK, paymentDetails.get("amount_paid"),
                TEXT_MUTED, THEME_DARK, paymentDetails.get("paid_on"),
                psHtml,
                BORDER_COLOR,
                THEME_DARK, TEXT_MUTED, BORDER_COLOR,
                THEME_DARK,
                BORDER_COLOR, BG_LIGHT, TEXT_MUTED, BORDER_COLOR, TEXT_MUTED, BORDER_COLOR, TEXT_MUTED, BORDER_COLOR, TEXT_MUTED, BORDER_COLOR,
                membersHtml.toString(),
                BORDER_COLOR, TEXT_MUTED, THEME_COLOR
        );
    }


}