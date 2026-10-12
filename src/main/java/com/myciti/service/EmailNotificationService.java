package com.myciti.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:admin@myciti.com}")
    private String fromEmail;

    @Value("${app.auth.admin-emails:ranjanjena8457@gmail.com}")
    private String adminEmails;

    /**
     * Notify owner/admin when a new advertisement is submitted.
     */
    public void notifyOwnerNewSubmission(String submissionTitle, String submitterName, String submitterEmail, Long submissionId) {
        if (!mailEnabled || mailSender == null) {
            log.info("[Notification] New advertisement submitted for review: #{} '{}' by {} ({}). (Mail sending disabled)",
                    submissionId, submissionTitle, submitterName, submitterEmail);
            return;
        }

        try {
            String[] recipients = adminEmails.split(",");
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(recipients);
            message.setSubject("🔔 New Advertisement Submitted for Approval: " + submissionTitle);
            message.setText(String.format(
                    "Hello Admin,\n\n" +
                    "A new advertisement has been submitted on myCiti and is waiting for your review.\n\n" +
                    "• Submission ID: #%d\n" +
                    "• Title: %s\n" +
                    "• Submitter: %s (%s)\n\n" +
                    "Please log in to the myCiti admin review dashboard to approve or decline.\n\n" +
                    "— myCiti Team",
                    submissionId, submissionTitle, submitterName, submitterEmail
            ));
            mailSender.send(message);
            log.info("Successfully sent new submission notification email to owner for submission #{}", submissionId);
        } catch (Exception e) {
            // Requirement: "If sending fails, never block the approve/decline action; log and continue."
            log.error("Failed to send new submission email to admin: {}", e.getMessage(), e);
        }
    }

    /**
     * Notify submitter that their advertisement was approved.
     */
    public void notifyUserApproved(String toEmail, String userName, String submissionTitle, Long listingId) {
        if (!mailEnabled || mailSender == null || toEmail == null || toEmail.isBlank()) {
            log.info("[Notification] Advertisement approved: '{}' for user {} ({}). Listing ID: {}. (Mail sending disabled)",
                    submissionTitle, userName, toEmail, listingId);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail.trim());
            message.setSubject("🎉 Your Advertisement is Approved and Live on myCiti!");
            message.setText(String.format(
                    "Hello %s,\n\n" +
                    "Great news! Your advertisement \"%s\" has been approved by the myCiti team and is now live on the public map and festival guide.\n\n" +
                    "Listing ID: #%d\n\n" +
                    "You can view and manage your posts anytime from your myCiti account dashboard.\n\n" +
                    "Warm regards,\n" +
                    "The myCiti Team\n" +
                    "admin@myciti.com",
                    userName != null ? userName : "there", submissionTitle, listingId
            ));
            mailSender.send(message);
            log.info("Successfully sent approval email to submitter {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send approval email to user {}: {}", toEmail, e.getMessage(), e);
        }
    }

    /**
     * Notify submitter that their advertisement was declined with reason.
     */
    public void notifyUserDeclined(String toEmail, String userName, String submissionTitle, String reason) {
        if (!mailEnabled || mailSender == null || toEmail == null || toEmail.isBlank()) {
            log.info("[Notification] Advertisement declined: '{}' for user {} ({}). Reason: {}. (Mail sending disabled)",
                    submissionTitle, userName, toEmail, reason);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail.trim());
            message.setSubject("⚠️ Update regarding your advertisement on myCiti");
            message.setText(String.format(
                    "Hello %s,\n\n" +
                    "Thank you for submitting \"%s\" on myCiti.\n\n" +
                    "Upon review, your submission could not be approved for the following reason:\n" +
                    "\"%s\"\n\n" +
                    "You can easily edit and resubmit your advertisement anytime from the 'My Advertisements' section in the app.\n\n" +
                    "Need assistance? Please feel free to reply or write to admin@myciti.com.\n\n" +
                    "Warm regards,\n" +
                    "The myCiti Team",
                    userName != null ? userName : "there", submissionTitle, reason
            ));
            mailSender.send(message);
            log.info("Successfully sent decline email to submitter {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send decline email to user {}: {}", toEmail, e.getMessage(), e);
        }
    }
}
