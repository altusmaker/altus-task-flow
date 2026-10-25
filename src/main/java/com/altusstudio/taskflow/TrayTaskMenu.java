package com.altusstudio.taskflow;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Swing popup: native Windows tray menus do not support per-row background colors. */
final class TrayTaskMenu extends JDialog {
    static final Color OVERDUE_BACKGROUND = new Color(255, 239, 238);
    private static final Color INK = new Color(38, 57, 71);
    private static final Color MUTED = new Color(99, 117, 129);
    private static final Color TEAL = new Color(7, 101, 141);
    private static final Color BORDER = new Color(217, 228, 232);
    private static final Color HOVER = new Color(234, 245, 249);
    private static final Color DANGER = new Color(174, 53, 53);
    private static final Font MENU_FONT = new Font("Tahoma", Font.PLAIN, 13);

    TrayTaskMenu(Point location, List<Task> tasks, LocalDateTime now,
                 Consumer<Long> openTask, Runnable exit) {
        setUndecorated(true);
        setTitle("آلتوس تسک فلو");
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
        content.setBackground(Color.WHITE);
        content.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        List<Task> activeTasks = tasks.stream().filter(task -> task.getStatus() == TaskStatus.IN_PROGRESS)
                .sorted(TaskOrder.BOARD).toList();
        content.add(createHeader(activeTasks.size()), BorderLayout.NORTH);
        JPanel rows = new JPanel(new GridLayout(0, 1, 0, 4));
        rows.setBackground(Color.WHITE);
        activeTasks.forEach(task -> {
                    boolean overdue = task.getReminderTime() != null && !task.getReminderTime().isAfter(now);
                    JButton row = menuButton(task.getTitle(), overdue ? OVERDUE_BACKGROUND : Color.WHITE,
                            overdue ? DANGER : INK, overdue ? "overdue" : "task");
                    // Titles are plain text, even if a title begins with an HTML tag.
                    row.setToolTipText(task.getTitle());
                    row.getAccessibleContext().setAccessibleDescription(overdue ? "تسک عقب‌افتاده" : "تسک در حال انجام");
                    row.addActionListener(event -> openTask.accept(task.getId()));
                    rows.add(row);
                });
        if (rows.getComponentCount() == 0) {
            JLabel empty = new JLabel("هیچ تسکی در حال انجام نیست", SwingConstants.CENTER);
            empty.setFont(MENU_FONT);
            empty.setForeground(MUTED);
            empty.setBorder(BorderFactory.createEmptyBorder(26, 12, 26, 12));
            rows.add(empty);
        }
        JScrollPane scroll = new JScrollPane(rows);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(32);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        scroll.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor = BORDER;
                trackColor = Color.WHITE;
            }

            @Override
            protected JButton createDecreaseButton(int orientation) { return hiddenButton(); }

            @Override
            protected JButton createIncreaseButton(int orientation) { return hiddenButton(); }

            private JButton hiddenButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                return button;
            }
        });
        Dimension preferred = rows.getPreferredSize();
        scroll.setPreferredSize(new Dimension(Math.min(420, Math.max(320, preferred.width + 16)),
                Math.min(400, preferred.height)));
        content.add(scroll, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        JSeparator separator = new JSeparator();
        separator.setForeground(BORDER);
        separator.setBackground(Color.WHITE);
        footer.add(separator, BorderLayout.NORTH);
        JButton exitButton = menuButton("خروج", Color.WHITE, MUTED, "exit");
        exitButton.addActionListener(event -> exit.run());
        footer.add(exitButton, BorderLayout.CENTER);
        content.add(footer, BorderLayout.SOUTH);
        content.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        return content;
    }

    private static JPanel createHeader(int count) {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 16, 10));
        JPanel title = new JPanel(new GridLayout(0, 1, 0, 5));
        title.setOpaque(false);
        JLabel brand = new JLabel("آلتوس تسک فلو", SwingConstants.LEADING);
        brand.setFont(MENU_FONT.deriveFont(Font.BOLD, 11f));
        brand.setForeground(TEAL);
        JLabel subtitle = new JLabel("در حال انجام", SwingConstants.LEADING);
        subtitle.setFont(MENU_FONT.deriveFont(Font.BOLD, 18f));
        subtitle.setForeground(INK);
        title.add(brand);
        title.add(subtitle);
        header.add(title, BorderLayout.CENTER);
        String localizedCount = NumberFormat.getIntegerInstance(Locale.forLanguageTag("fa")).format(count);
        JLabel badge = new JLabel(localizedCount, SwingConstants.CENTER);
        badge.setFont(MENU_FONT.deriveFont(Font.BOLD));
        badge.setForeground(TEAL);
        badge.setOpaque(true);
        badge.setBackground(HOVER);
        badge.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        badge.getAccessibleContext().setAccessibleName(localizedCount + " تسک در حال انجام");
        JPanel badgeHolder = new JPanel(new GridBagLayout());
        badgeHolder.setOpaque(false);
        badgeHolder.add(badge);
        header.add(badgeHolder, BorderLayout.LINE_END);
        return header;
    }

    private static JButton menuButton(String text, Color background, Color foreground, String symbol) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean highlighted = getModel().isRollover() || isFocusOwner();
                g.setColor(getModel().isPressed() ? BORDER : highlighted
                        ? (background.equals(OVERDUE_BACKGROUND) ? new Color(255, 224, 222) : HOVER) : getBackground());
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                if (isFocusOwner()) {
                    g.setColor(TEAL);
                    g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 10, 10);
                }
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        button.putClientProperty("html.disable", true);
        button.setFont(MENU_FONT);
        button.setBackground(background);
        button.setForeground(foreground);
        button.setHorizontalAlignment(SwingConstants.LEADING);
        button.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 14));
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setIconTextGap(12);
        button.setIcon(new Icon() {
            public int getIconWidth() { return 16; }
            public int getIconHeight() { return 16; }

            public void paintIcon(Component component, Graphics graphics, int x, int y) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.translate(x, y);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(symbol.equals("task") ? TEAL : foreground);
                if (symbol.equals("exit")) {
                    g.drawArc(2, 2, 12, 12, 135, 270);
                    g.drawLine(8, 0, 8, 7);
                } else {
                    g.drawOval(2, 2, 12, 12);
                    g.drawLine(8, 5, 8, 8);
                    if (symbol.equals("overdue")) g.drawLine(8, 11, 8, 11);
                    else g.drawLine(8, 8, 11, 8);
                }
                g.dispose();
            }
        });
        return button;
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
