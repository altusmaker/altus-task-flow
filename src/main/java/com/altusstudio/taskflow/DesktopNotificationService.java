package com.altusstudio.taskflow;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.SystemTray;
import java.awt.TrayIcon;

/** Notifications belong to the desktop running this local application, not a browser tab. */
@Service
public class DesktopNotificationService {
    private static final Logger log = LoggerFactory.getLogger(DesktopNotificationService.class);
    private TrayIcon trayIcon;

    @PostConstruct
    public void initialize() {
        try {
            if (!SystemTray.isSupported()) {
                log.warn("Desktop notifications unavailable: no OS system tray in this session.");
                return;
            }
            try (var icon = getClass().getResourceAsStream("/static/images/todo-logo.png")) {
                if (icon == null) throw new IllegalStateException("Missing notification icon");
                TrayIcon candidate = new TrayIcon(ImageIO.read(icon), "Altus Task Flow");
                candidate.setImageAutoSize(true);
                SystemTray.getSystemTray().add(candidate);
                trayIcon = candidate;
            }
        } catch (Exception | LinkageError exception) {
            log.warn("Desktop notifications unavailable; the application will keep running.", exception);
        }
    }

    public boolean isAvailable() {
        return trayIcon != null;
    }

    public boolean showReminder(String title) {
        if (!isAvailable()) return false;
        try {
            trayIcon.displayMessage("یادآوری تسک", title, TrayIcon.MessageType.INFO);
            return true;
        } catch (RuntimeException exception) {
            log.warn("Could not display desktop reminder", exception);
            return false;
        }
    }

    @PreDestroy
    public void close() {
        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
            trayIcon = null;
        }
    }
}
