package com.altusstudio.taskflow;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.BorderLayout;
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
            JPanel footer = (JPanel) ((BorderLayout) content.getLayout())
                    .getLayoutComponent(BorderLayout.SOUTH);
            assertInstanceOf(JSeparator.class, footer.getComponent(0));
            JButton exit = (JButton) footer.getComponent(1);
            assertEquals("Exit", exit.getText());
            exit.doClick(0);
            assertTrue(exited.get());
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
