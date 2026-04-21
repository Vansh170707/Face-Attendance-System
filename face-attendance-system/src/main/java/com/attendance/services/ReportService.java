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

            // Title
            Paragraph title = new Paragraph("📊 Attendance Report", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            // Date Range
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
            Paragraph dateRange = new Paragraph(
                    "Period: " + startDate.format(formatter) + " to " + endDate.format(formatter),
                    new Font(Font.FontFamily.HELVETICA, 12, Font.ITALIC, BaseColor.GRAY));
            dateRange.setAlignment(Element.ALIGN_CENTER);
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
            addTableHeader(table, "Check Out");

            // Data rows
            for (Attendance record : records) {
                addTableCell(table, record.getFormattedDate());
                addTableCell(table, record.getUserName());
                addTableCell(table, record.getDepartment() != null ? record.getDepartment() : "-");
                addTableCell(table, record.getFormattedCheckIn());
                addTableCell(table, record.getFormattedCheckOut());
            }

            document.add(table);

            // Summary
            Paragraph summary = new Paragraph(
                    "\nTotal Records: " + records.size(),
                    new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD));
            summary.setSpacingBefore(20);
            document.add(summary);

            // Footer
            Paragraph footer = new Paragraph(
                    "Generated on: " + LocalDate.now().format(formatter),
                    new Font(Font.FontFamily.HELVETICA, 9, Font.ITALIC, BaseColor.GRAY));
            footer.setSpacingBefore(30);
            document.add(footer);

            document.close();
            System.out.println("✅ PDF Report generated: " + filePath);
            return true;

        } catch (Exception e) {
            System.err.println("❌ PDF generation failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void addTableHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setBackgroundColor(new BaseColor(70, 130, 180)); // Steel Blue
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(8);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, CELL_FONT));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6);
        table.addCell(cell);
    }
}
