package com.altusstudio.taskflow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

/** Swing popup: native Windows tray menus do not support per-row background colors. */
final class TrayTaskMenu extends JDialog {
    static final Color OVERDUE_BACKGROUND = new Color(255, 220, 220);

    TrayTaskMenu(Point location, List<Task> tasks, LocalDateTime now,
                 Consumer<Long> openTask, Runnable exit) {
        setUndecorated(true);
        setAlwaysOnTop(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(createContent(tasks, now, id -> {
            dispose();
            openTask.accept(id);
        }, () -> {
            dispose();
            exit.run();
        }));
        getRootPane().registerKeyboardAction(event -> dispose(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent event) {
                dispose();
            }
        });
        pack();
        Rectangle screen = screenBounds(location);
        setSize(Math.min(getWidth(), screen.width), Math.min(getHeight(), screen.height));
        setLocation(Math.max(screen.x, Math.min(location.x, screen.x + screen.width - getWidth())),
                Math.max(screen.y, Math.min(location.y, screen.y + screen.height - getHeight())));
    }

    static JPanel createContent(List<Task> tasks, LocalDateTime now,
                                Consumer<Long> openTask, Runnable exit) {
        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        JPanel rows = new JPanel(new GridLayout(0, 1));
        tasks.stream().filter(task -> task.getStatus() == TaskStatus.IN_PROGRESS)
                .sorted(TaskOrder.BOARD).forEach(task -> {
                    JButton row = new JButton();
                    // Titles are plain text, even if a title begins with an HTML tag.
                    row.putClientProperty("html.disable", true);
                    row.setText(task.getTitle());
                    row.setHorizontalAlignment(SwingConstants.LEADING);
                    row.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
                    row.setOpaque(true);
                    if (task.getReminderTime() != null && !task.getReminderTime().isAfter(now)) {
                        row.setBackground(OVERDUE_BACKGROUND);
                        row.setForeground(Color.BLACK);
                    }
                    row.addActionListener(event -> openTask.accept(task.getId()));
                    rows.add(row);
                });
        if (rows.getComponentCount() == 0) {
            JLabel empty = new JLabel("No in-progress tasks", SwingConstants.CENTER);
            empty.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            rows.add(empty);
        }
        JScrollPane scroll = new JScrollPane(rows);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(32);
        Dimension preferred = rows.getPreferredSize();
        scroll.setPreferredSize(new Dimension(Math.min(480, Math.max(240, preferred.width + 24)),
                Math.min(400, preferred.height)));
        content.add(scroll, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout());
        footer.add(new JSeparator(), BorderLayout.NORTH);
        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(event -> exit.run());
        footer.add(exitButton, BorderLayout.CENTER);
        content.add(footer, BorderLayout.SOUTH);
        return content;
    }

    private static Rectangle screenBounds(Point location) {
        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsConfiguration configuration = environment.getDefaultScreenDevice().getDefaultConfiguration();
        for (GraphicsDevice device : environment.getScreenDevices()) {
            if (device.getDefaultConfiguration().getBounds().contains(location)) {
                configuration = device.getDefaultConfiguration();
                break;
            }
        }
        Rectangle bounds = new Rectangle(configuration.getBounds());
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
        return new Rectangle(bounds.x + insets.left, bounds.y + insets.top,
                bounds.width - insets.left - insets.right, bounds.height - insets.top - insets.bottom);
    }
}
