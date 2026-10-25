package com.altusstudio.taskflow;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Desktop;
import java.awt.Point;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.CompletableFuture;

/** Notifications belong to the desktop running this local application, not a browser tab. */
@Service
public class DesktopNotificationService {
    private static final Logger log = LoggerFactory.getLogger(DesktopNotificationService.class);
    private final TaskRepository tasks;
    private final ConfigurableApplicationContext applicationContext;
    private TrayIcon trayIcon;
    private TrayTaskMenu taskMenu;
    private long menuRequest;

    public DesktopNotificationService(TaskRepository tasks, ConfigurableApplicationContext applicationContext) {
        this.tasks = tasks;
        this.applicationContext = applicationContext;
    }

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
                candidate.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent event) {
                        if (event.getButton() == MouseEvent.BUTTON1 && event.getClickCount() == 1) {
                            openHomePage();
                        }
                    }

                    @Override
                    public void mouseReleased(MouseEvent event) {
                        if (event.isPopupTrigger() || event.getButton() == MouseEvent.BUTTON3) {
                            showTaskMenu(event.getLocationOnScreen());
                        }
                    }
                });
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

    private void openHomePage() {
        openBrowser(URI.create("http://localhost:18080"));
    }

    private void showTaskMenu(Point location) {
        // Database access must not block the AWT event thread.
        long request = ++menuRequest;
        if (taskMenu != null) taskMenu.dispose();
        CompletableFuture.supplyAsync(tasks::findAll).whenComplete((snapshot, failure) ->
                SwingUtilities.invokeLater(() -> {
                    if (request != menuRequest || !isAvailable()) return;
                    if (failure != null) {
                        log.warn("Could not load tray tasks", failure);
                    }
                    taskMenu = new TrayTaskMenu(location, snapshot == null ? java.util.List.of() : snapshot,
                            LocalDateTime.now(ZoneId.of("Asia/Tehran")),
                            id -> CompletableFuture.runAsync(() ->
                                    openBrowser(URI.create("http://localhost:18080/?task=" + id))),
                            this::exitApplication);
                    taskMenu.setVisible(true);
                    taskMenu.requestFocus();
                }));
    }

    private void exitApplication() {
        // Close Spring (including persistence) before terminating this application's JVM.
        new Thread(() -> System.exit(SpringApplication.exit(applicationContext, () -> 0)),
                "tray-exit").start();
    }

    private void openBrowser(URI uri) {
        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                log.warn("Could not open home page: desktop browsing is unavailable.");
                return;
            }
            Desktop.getDesktop().browse(uri);
        } catch (Exception | LinkageError exception) {
            log.warn("Could not open home page in the default browser", exception);
        }
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
        SwingUtilities.invokeLater(() -> {
            menuRequest++;
            if (taskMenu != null) taskMenu.dispose();
        });
        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
            trayIcon = null;
        }
    }
}
