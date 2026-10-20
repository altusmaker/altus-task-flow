package com.altusstudio.taskflow;

import java.awt.Desktop;
import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TodoApp {
    @Value("${app.browser.auto-open:true}")
    private boolean autoOpenBrowser;

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(TodoApp.class);
        application.setHeadless(false);
        application.run(args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void openBrowser() {
        if (!autoOpenBrowser || !Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            return;
        }

        try {
            Desktop.getDesktop().browse(URI.create("http://localhost:9095/"));
        } catch (Exception ignored) {
            // The application remains available at localhost even if a browser cannot be launched.
        }
    }
}
