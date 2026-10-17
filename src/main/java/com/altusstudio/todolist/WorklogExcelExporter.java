package com.altusstudio.todolist;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Builds a right-to-left Persian Excel timesheet from report entries. */
public final class WorklogExcelExporter {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    private WorklogExcelExporter() { }

    public static byte[] export(List<WorklogReportEntry> entries, int totalMinutes) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("گزارش کارکرد");
            sheet.setRightToLeft(true);
            sheet.setDisplayGridlines(false);
            Font headerFont = workbook.createFont();
            headerFont.setFontName("Tahoma"); headerFont.setBold(true); headerFont.setColor(IndexedColors.WHITE.getIndex());
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont); headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND); headerStyle.setAlignment(HorizontalAlignment.CENTER);
            CellStyle bodyStyle = workbook.createCellStyle();
            bodyStyle.setAlignment(HorizontalAlignment.RIGHT); bodyStyle.setVerticalAlignment(VerticalAlignment.TOP); bodyStyle.setWrapText(true);

            Row title = sheet.createRow(0); title.createCell(0).setCellValue("گزارش تایم‌شیت");
            Row summary = sheet.createRow(1); summary.createCell(0).setCellValue("جمع زمان ثبت‌شده");
            summary.createCell(1).setCellValue(totalMinutes / 60 + " ساعت و " + totalMinutes % 60 + " دقیقه");
            String[] headers = {"تسک", "تاریخ و ساعت", "توضیحات", "مدت (دقیقه)"};
            Row header = sheet.createRow(3);
            for (int column = 0; column < headers.length; column++) { Cell cell = header.createCell(column); cell.setCellValue(headers[column]); cell.setCellStyle(headerStyle); }
            int rowIndex = 4;
            for (WorklogReportEntry entry : entries) {
                Row row = sheet.createRow(rowIndex++);
                String[] values = {entry.taskTitle(), DATE_TIME.format(entry.loggedAt()), entry.description() == null ? "" : entry.description()};
                for (int column = 0; column < values.length; column++) { Cell cell = row.createCell(column); cell.setCellValue(values[column]); cell.setCellStyle(bodyStyle); }
                Cell duration = row.createCell(3); duration.setCellValue(entry.durationMinutes()); duration.setCellStyle(bodyStyle);
            }
            sheet.createFreezePane(0, 4); sheet.setColumnWidth(0, 24 * 256); sheet.setColumnWidth(1, 21 * 256);
            sheet.setColumnWidth(2, 44 * 256); sheet.setColumnWidth(3, 16 * 256);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) { throw new IllegalStateException("Could not create the Excel worklog report.", exception); }
    }
}
