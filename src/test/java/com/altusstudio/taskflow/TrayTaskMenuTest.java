package com.altusstudio.taskflow;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class TrayTaskMenuTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 26, 12, 0);

    @Test
    void listsOnlyInProgressTitlesInBoardOrderAndHighlightsPastReminders() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Task overdue = task(1, "<html>Overdue", TaskStatus.IN_PROGRESS, NOW.minusMinutes(1));
            Task dueNow = task(2, "Due now", TaskStatus.IN_PROGRESS, NOW);
            Task future = task(3, "Future", TaskStatus.IN_PROGRESS, NOW.plusMinutes(1));
            Task older = task(4, "Older", TaskStatus.IN_PROGRESS, null);
            older.setUpdatedAt(NOW.minusDays(1));
            Task newer = task(5, "Newer", TaskStatus.IN_PROGRESS, null);
            newer.setUpdatedAt(NOW);
            Task tie = task(6, "Higher ID", TaskStatus.IN_PROGRESS, null);
            tie.setUpdatedAt(NOW);
            List<Long> opened = new ArrayList<>();
            JPanel content = TrayTaskMenu.createContent(List.of(older, future, newer, overdue, dueNow, tie,
                    task(7, "Todo", TaskStatus.TODO, NOW.minusHours(1)),
                    task(8, "Done", TaskStatus.DONE, NOW.minusHours(2))), NOW, opened::add, () -> {});
            JPanel rows = rows(content);

            assertEquals(6, rows.getComponentCount());
            assertEquals(List.of("<html>Overdue", "Due now", "Future", "Higher ID", "Newer", "Older"),
                    java.util.Arrays.stream(rows.getComponents()).map(row -> ((JButton) row).getText()).toList());
            for (int index = 0; index < rows.getComponentCount(); index++) {
                JButton row = (JButton) rows.getComponent(index);
                assertEquals(index < 2, TrayTaskMenu.OVERDUE_BACKGROUND.equals(row.getBackground()));
                assertEquals(true, row.getClientProperty("html.disable"));
                assertFalse(row.getComponentOrientation().isLeftToRight());
                assertEquals(index < 2 ? "تسک عقب‌افتاده" : "تسک در حال انجام",
                        row.getAccessibleContext().getAccessibleDescription());
                row.doClick(0);
            }
            assertEquals(List.of(1L, 2L, 3L, 6L, 5L, 4L), opened);
        });
    }

    @Test
    void emptyListStillHasSeparatedWorkingExitButton() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicBoolean exited = new AtomicBoolean();
            JPanel content = TrayTaskMenu.createContent(List.of(), NOW,
                    id -> fail("No task should be opened"), () -> exited.set(true));
            assertInstanceOf(JLabel.class, rows(content).getComponent(0));
            assertEquals("هیچ تسکی در حال انجام نیست", ((JLabel) rows(content).getComponent(0)).getText());
            JPanel footer = (JPanel) ((BorderLayout) content.getLayout())
                    .getLayoutComponent(BorderLayout.SOUTH);
            assertInstanceOf(JSeparator.class, footer.getComponent(0));
            JButton exit = (JButton) footer.getComponent(1);
            assertEquals("خروج", exit.getText());
            exit.doClick(0);
            assertTrue(exited.get());
        });
    }

    @Test
    void styledMenuHasHeaderBoundedScrollingAndVisibleHoverState() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            List<Task> tasks = new ArrayList<>();
            for (int id = 1; id <= 30; id++) {
                tasks.add(task(id, "A long task title ".repeat(20), TaskStatus.IN_PROGRESS, null));
            }
            JPanel content = TrayTaskMenu.createContent(tasks, NOW, id -> {}, () -> {});
            JPanel header = (JPanel) ((BorderLayout) content.getLayout()).getLayoutComponent(BorderLayout.NORTH);
            JPanel titles = (JPanel) ((BorderLayout) header.getLayout()).getLayoutComponent(BorderLayout.CENTER);
            assertEquals("مدیریت کارهای آلتوس", ((JLabel) titles.getComponent(0)).getText());
            assertEquals("در حال انجام", ((JLabel) titles.getComponent(1)).getText());
            assertFalse(content.getComponentOrientation().isLeftToRight());
            JPanel badgeHolder = (JPanel) ((BorderLayout) header.getLayout()).getLayoutComponent(BorderLayout.LINE_END);
            JLabel badge = (JLabel) badgeHolder.getComponent(0);
            assertEquals("۳۰", badge.getText());
            assertEquals("۳۰ تسک در حال انجام", badge.getAccessibleContext().getAccessibleName());
            JScrollPane scroll = (JScrollPane) ((BorderLayout) content.getLayout()).getLayoutComponent(BorderLayout.CENTER);
            assertEquals(420, scroll.getPreferredSize().width);
            assertEquals(400, scroll.getPreferredSize().height);
            assertEquals(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER, scroll.getHorizontalScrollBarPolicy());

            JButton row = (JButton) rows(content).getComponent(0);
            assertNotNull(row.getIcon());
            assertFalse(row.isContentAreaFilled());
            assertEquals(row.getText(), row.getToolTipText());
            row.setSize(320, 44);
            BufferedImage image = new BufferedImage(320, 44, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            row.paint(graphics);
            int normal = image.getRGB(300, 5);
            row.getModel().setRollover(true);
            row.paint(graphics);
            assertNotEquals(normal, image.getRGB(300, 5));
            assertEquals(Color.WHITE.getRGB(), normal);
            graphics.dispose();
        });
    }

    private static JPanel rows(JPanel content) {
        JScrollPane scroll = (JScrollPane) ((BorderLayout) content.getLayout())
                .getLayoutComponent(BorderLayout.CENTER);
        return (JPanel) scroll.getViewport().getView();
    }

    private static Task task(long id, String title, TaskStatus status, LocalDateTime reminder) {
        return new Task(id, title, status, reminder, List.of());
    }
}
