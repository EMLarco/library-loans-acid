package com.larco.biblioteca.config;

import com.larco.biblioteca.notification.ConsoleNotificationService;
import com.larco.biblioteca.notification.EmailNotificationService;
import com.larco.biblioteca.notification.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Elige la implementación por configuración; el resto del sistema solo conoce NotificationService. */
@Configuration
public class NotificationConfig {
    @Bean
    public NotificationService notificationService(@Value("${app.notification.channel}") String channel) {
        return "email".equalsIgnoreCase(channel) ? new EmailNotificationService() : new ConsoleNotificationService();
    }
}
