package com.attendance.services;

import com.attendance.models.Attendance;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Report generation service for PDF/Excel exports
 */
public class ReportService {

    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, BaseColor.DARK_GRAY);
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.WHITE);
    private static final Font CELL_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);

    public boolean generatePDFReport(List<Attendance> records, String filePath, LocalDate startDate,
            LocalDate endDate) {
        try {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();

            // Premium Feature: Professional PDF Header
            PdfPTable headerTable = new PdfPTable(1);
            headerTable.setWidthPercentage(100);
            PdfPCell headerCell = new PdfPCell();
            headerCell.setBorder(Rectangle.BOTTOM);
            headerCell.setBorderColor(new BaseColor(0, 198, 255));
            headerCell.setBorderWidth(2f);
            headerCell.setPaddingBottom(10f);

            Paragraph title = new Paragraph("FACE ATTENDANCE SYSTEM", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            headerCell.addElement(title);

            Paragraph subTitle = new Paragraph("Official Attendance Report", new Font(Font.FontFamily.HELVETICA, 14, Font.NORMAL, BaseColor.GRAY));
            subTitle.setAlignment(Element.ALIGN_CENTER);
            headerCell.addElement(subTitle);

            headerTable.addCell(headerCell);
            document.add(headerTable);

            // Date Range
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
            Paragraph dateRange = new Paragraph(
                    "Report Period: " + startDate.format(formatter) + "  —  " + endDate.format(formatter),
                    new Font(Font.FontFamily.HELVETICA, 12, Font.ITALIC, BaseColor.DARK_GRAY));
            dateRange.setAlignment(Element.ALIGN_RIGHT);
            dateRange.setSpacingBefore(15);
            dateRange.setSpacingAfter(20);
            document.add(dateRange);

            // Table
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            table.setWidths(new float[] { 2, 3, 2, 2, 2 });

            // Headers
            addTableHeader(table, "Date");
            addTableHeader(table, "Name");
            addTableHeader(table, "Department");
            addTableHeader(table, "Check In");
            addTableHeader(table, "Status");

            // Data rows
            boolean isAlternate = false;
            for (Attendance record : records) {
                BaseColor rowColor = isAlternate ? new BaseColor(245, 245, 250) : BaseColor.WHITE;
                addTableCell(table, record.getFormattedDate(), rowColor);
                addTableCell(table, record.getUserName(), rowColor);
                addTableCell(table, record.getDepartment() != null ? record.getDepartment() : "-", rowColor);
                addTableCell(table, record.getFormattedCheckIn(), rowColor);
                
                // Status with color
                PdfPCell statusCell = new PdfPCell(new Phrase(record.getStatus(), 
                    new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, 
                        "PRESENT".equals(record.getStatus()) ? new BaseColor(40, 167, 69) : new BaseColor(220, 53, 69))));
                statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                statusCell.setBackgroundColor(rowColor);
                statusCell.setPadding(8);
                statusCell.setBorderColor(new BaseColor(220, 220, 220));
                table.addCell(statusCell);
                
                isAlternate = !isAlternate;
            }

            document.add(table);

            // Summary Statistics Box
            PdfPTable summaryTable = new PdfPTable(1);
            summaryTable.setWidthPercentage(40);
            summaryTable.setHorizontalAlignment(Element.ALIGN_LEFT);
            summaryTable.setSpacingBefore(30);
            
            PdfPCell summaryCell = new PdfPCell();
            summaryCell.setBackgroundColor(new BaseColor(240, 248, 255));
            summaryCell.setBorderColor(new BaseColor(0, 198, 255));
            summaryCell.setBorderWidth(1f);
            summaryCell.setPadding(10f);
            
            long presentCount = records.stream().filter(r -> "PRESENT".equals(r.getStatus())).count();

            summaryCell.addElement(new Paragraph("Summary Statistics", new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD)));
            summaryCell.addElement(new Paragraph("Total Records: " + records.size(), CELL_FONT));
            summaryCell.addElement(new Paragraph("Present: " + presentCount, CELL_FONT));
            summaryCell.addElement(new Paragraph("Absent: " + (records.size() - presentCount), CELL_FONT));
            
            summaryTable.addCell(summaryCell);
            document.add(summaryTable);

            // Footer
            Paragraph footer = new Paragraph(
                    "Generated automatically by Face Attendance System on " + LocalDate.now().format(formatter),
                    new Font(Font.FontFamily.HELVETICA, 9, Font.ITALIC, BaseColor.GRAY));
            footer.setAlignment(Element.ALIGN_CENTER);    
            footer.setSpacingBefore(40);
            document.add(footer);

            document.close();
            System.out.println("✅ Custom PDF Report generated: " + filePath);
            return true;

        } catch (Exception e) {
            System.err.println("❌ PDF generation failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void addTableHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setBackgroundColor(new BaseColor(16, 16, 28)); // Dark modern header
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(10);
        cell.setBorderColor(new BaseColor(0, 198, 255));
        cell.setBorderWidthBottom(2f);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, BaseColor bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, CELL_FONT));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8);
        cell.setBackgroundColor(bgColor);
        cell.setBorderColor(new BaseColor(220, 220, 220));
        table.addCell(cell);
    }

    // ==================== Excel Export ====================

    public boolean generateExcelReport(List<Attendance> records, String filePath, LocalDate startDate, LocalDate endDate) {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.xssf.usermodel.XSSFSheet sheet = workbook.createSheet("Attendance Report");

            // Header style
            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);

            // Title row
            org.apache.poi.ss.usermodel.Row titleRow = sheet.createRow(0);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Face Attendance System — Report (" + startDate + " to " + endDate + ")");
            org.apache.poi.ss.usermodel.CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 5));

            // Header row
            org.apache.poi.ss.usermodel.Row header = sheet.createRow(2);
            String[] columns = {"Date", "Name", "Department", "Check In", "Check Out", "Status"};
            for (int i = 0; i < columns.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 3;
            for (Attendance record : records) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(record.getFormattedDate());
                row.createCell(1).setCellValue(record.getUserName());
                row.createCell(2).setCellValue(record.getDepartment() != null ? record.getDepartment() : "-");
                row.createCell(3).setCellValue(record.getFormattedCheckIn());
                row.createCell(4).setCellValue(record.getFormattedCheckOut());
                row.createCell(5).setCellValue(record.getStatus());
            }

            // Summary
            rowNum += 1;
            org.apache.poi.ss.usermodel.Row summaryRow = sheet.createRow(rowNum);
            summaryRow.createCell(0).setCellValue("Total Records: " + records.size());
            long present = records.stream().filter(r -> "PRESENT".equals(r.getStatus())).count();
            sheet.createRow(rowNum + 1).createCell(0).setCellValue("Present: " + present);
            sheet.createRow(rowNum + 2).createCell(0).setCellValue("Absent: " + (records.size() - present));

            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(filePath)) {
                workbook.write(fos);
            }

            System.out.println("✅ Excel Report generated: " + filePath);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Excel generation failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
