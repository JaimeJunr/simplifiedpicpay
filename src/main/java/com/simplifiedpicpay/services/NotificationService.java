package com.simplifiedpicpay.services;

import com.simplifiedpicpay.domain.user.User;
import com.simplifiedpicpay.dtos.NotificationDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Value("${notification.service.url}")
    private String notificationServiceUrl;

    private final RestTemplate restTemplate;

    @Autowired
    public NotificationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void sendNotification(User user, String message) {
        String email = user.getEmail();
        NotificationDTO notificationRequest = new NotificationDTO(email, message);

        try {
            ResponseEntity<String> notificationResponse = restTemplate.postForEntity(notificationServiceUrl, notificationRequest, String.class);

            if (notificationResponse.getStatusCode() != HttpStatus.OK) {
                logger.error("Failed to send notification. Status code: {}", notificationResponse.getStatusCode());
                throw new RuntimeException("Notification service returned non-OK status");
            }
        } catch (Exception e) {
            logger.error("Error sending notification: {}", e.getMessage(), e);
            throw new RuntimeException("Notification service is down or failed", e);
        }
    }
}
