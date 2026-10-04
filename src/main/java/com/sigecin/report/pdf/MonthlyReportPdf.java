package com.sigecin.report.pdf;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.sigecin.appointment.dto.AppointmentDetailRow;
import com.sigecin.report.dto.MonthlyReport;
import com.sigecin.report.dto.MonthlySummary;
import com.sigecin.report.dto.ServiceRankingRow;
import com.sigecin.report.dto.WeeklyDistribution;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Genera el PDF del reporte mensual con iText: encabezado, resumen ejecutivo, gráfica de
 * barras de reservas por semana, ranking de servicios y tabla de citas del periodo.
 * Usa las fuentes estándar del PDF (Helvetica), que cubren los acentos del español.
 */
@Component
public class MonthlyReportPdf {

    private static final Locale LOCALE = Locale.forLanguageTag("es-MX");
    private static final Color PRIMARY = new DeviceRgb(13, 110, 253);      // --bs-primary
    private static final Color MUTED = new DeviceRgb(108, 117, 125);       // --bs-secondary-color
    private static final Color LIGHT = new DeviceRgb(248, 249, 250);       // --bs-tertiary-bg
    private static final Color BORDER = new DeviceRgb(222, 226, 230);      // --bs-border-color
    private static final DateTimeFormatter PERIOD = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", LOCALE);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d", LOCALE);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", LOCALE);
    private static final DateTimeFormatter GENERATED = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, HH:mm", LOCALE);

    private final MessageSource messages;

    public MonthlyReportPdf(MessageSource messages) {
        this.messages = messages;
    }

    public byte[] render(MonthlyReport report) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfDocument pdf = new PdfDocument(new PdfWriter(out));
             Document document = new Document(pdf, PageSize.A4)) {
            // Las fuentes pertenecen a cada documento: se crean en cada reporte
            PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            String period = PERIOD.format(report.period());
            pdf.getDocumentInfo().setTitle(text("report.pdf.title", report.businessName(), period));
            document.setMargins(40, 40, 40, 40);
            document.setFont(regular).setFontSize(9);

            header(document, report, period, bold);
            summary(document, report.summary(), bold);
            section(document, text("report.pdf.weekly.title"), bold);
            weekly(document, report.weekly(), bold);
            section(document, text("report.pdf.ranking.title"), bold);
            ranking(document, report, bold);
            section(document, text("report.pdf.appointments.title"), bold);
            appointments(document, report, bold);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el reporte PDF", e);
        }
        return out.toByteArray();
    }

    private void header(Document document, MonthlyReport report, String period, PdfFont bold) {
        document.add(new Paragraph(report.businessName()).setFont(bold).setFontSize(18).setMarginBottom(0));
        document.add(new Paragraph(text("report.pdf.subtitle", capitalize(period))).setFontSize(12)
                .setFontColor(PRIMARY).setMarginTop(0).setMarginBottom(0));
        document.add(new Paragraph(text("report.pdf.generated", GENERATED.format(report.generatedAt())))
                .setFontColor(MUTED).setFontSize(8).setMarginBottom(12));
    }

    /** Resumen ejecutivo: cinco totales en tarjetas. */
    private void summary(Document document, MonthlySummary summary, PdfFont bold) {
        section(document, text("report.pdf.summary.title"), bold);
        Table table = new Table(UnitValue.createPercentArray(5)).useAllAvailableWidth();
        table.addCell(stat(text("report.pdf.summary.total"), String.valueOf(summary.totalAppointments()), bold));
        table.addCell(stat(text("report.pdf.summary.confirmed"), String.valueOf(summary.confirmedAppointments()), bold));
        table.addCell(stat(text("report.pdf.summary.pending"), String.valueOf(summary.pendingAppointments()), bold));
        table.addCell(stat(text("report.pdf.summary.cancelled"), String.valueOf(summary.cancelledAppointments()), bold));
        table.addCell(stat(text("report.pdf.summary.income"), money(summary.estimatedIncome()), bold));
        document.add(table);
        document.add(new Paragraph(text("report.pdf.summary.note")).setFontColor(MUTED).setFontSize(7.5f));
    }

    private Cell stat(String label, String value, PdfFont bold) {
        return new Cell()
                .add(new Paragraph(label).setFontColor(MUTED).setFontSize(8).setMargin(0))
                .add(new Paragraph(value).setFont(bold).setFontSize(15).setMargin(0))
                .setBackgroundColor(LIGHT)
                .setBorder(new SolidBorder(BORDER, 0.5f))
                .setPadding(8);
    }

    /** Gráfica de barras horizontales: una barra por semana, proporcional a la semana con más reservas. */
    private void weekly(Document document, WeeklyDistribution weekly, PdfFont bold) {
        long max = Math.max(weekly.max(), 1);
        Table table = new Table(UnitValue.createPercentArray(new float[]{22, 68, 10})).useAllAvailableWidth();
        for (int week = 1; week <= weekly.counts().size(); week++) {
            long count = weekly.counts().get(week - 1);
            table.addCell(plain(text("report.pdf.weekly.label", week,
                    WeeklyDistribution.firstDay(week), weekly.lastDay(week))));
            Cell bar = plain("").setVerticalAlignment(VerticalAlignment.MIDDLE);
            if (count > 0) {
                bar.add(new Div().setHeight(11).setBackgroundColor(PRIMARY)
                        .setWidth(UnitValue.createPercentValue(100f * count / max)));
            }
            table.addCell(bar);
            table.addCell(plain(String.valueOf(count)).setFont(bold).setTextAlignment(TextAlignment.RIGHT));
        }
        document.add(table);
        document.add(new Paragraph(text("report.pdf.weekly.note", weekly.total()))
                .setFontColor(MUTED).setFontSize(7.5f));
    }

    private void ranking(Document document, MonthlyReport report, PdfFont bold) {
        if (report.ranking().isEmpty()) {
            document.add(new Paragraph(text("report.pdf.ranking.empty")).setFontColor(MUTED));
            return;
        }
        Table table = new Table(UnitValue.createPercentArray(new float[]{10, 70, 20})).useAllAvailableWidth();
        table.addHeaderCell(head("#", bold));
        table.addHeaderCell(head(text("report.pdf.ranking.service"), bold));
        table.addHeaderCell(head(text("report.pdf.ranking.bookings"), bold).setTextAlignment(TextAlignment.RIGHT));
        for (ServiceRankingRow row : report.ranking()) {
            table.addCell(row(String.valueOf(row.ranking())));
            table.addCell(row(row.serviceName()));
            table.addCell(row(String.valueOf(row.bookings())).setTextAlignment(TextAlignment.RIGHT));
        }
        document.add(table);
    }

    private void appointments(Document document, MonthlyReport report, PdfFont bold) {
        if (report.appointments().isEmpty()) {
            document.add(new Paragraph(text("report.pdf.appointments.empty")).setFontColor(MUTED));
            return;
        }
        Table table = new Table(UnitValue.createPercentArray(new float[]{12, 13, 27, 22, 13, 13})).useAllAvailableWidth();
        for (String key : new String[]{"date", "time", "client", "service", "status", "price"}) {
            Cell cell = head(text("report.pdf.appointments." + key), bold);
            table.addHeaderCell(key.equals("price") ? cell.setTextAlignment(TextAlignment.RIGHT) : cell);
        }
        for (AppointmentDetailRow a : report.appointments()) {
            table.addCell(row(DAY.format(a.startTime())));
            table.addCell(row(TIME.format(a.startTime()) + "–" + TIME.format(a.endTime())));
            table.addCell(row(a.clientName()));
            table.addCell(row(a.serviceName()));
            table.addCell(row(text("status.appointment." + a.status())));
            table.addCell(row(money(a.price())).setTextAlignment(TextAlignment.RIGHT));
        }
        document.add(table);
    }

    private void section(Document document, String title, PdfFont bold) {
        document.add(new Paragraph(title).setFont(bold).setFontSize(11).setMarginTop(14).setMarginBottom(6));
    }

    private static Cell head(String content, PdfFont bold) {
        return new Cell().add(new Paragraph(content).setFont(bold).setFontSize(8.5f))
                .setBackgroundColor(LIGHT)
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(BORDER, 1));
    }

    private static Cell row(String content) {
        return new Cell().add(new Paragraph(content == null ? "" : content))
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(BORDER, 0.5f));
    }

    private static Cell plain(String content) {
        return new Cell().add(new Paragraph(content)).setBorder(Border.NO_BORDER).setPaddingTop(3).setPaddingBottom(3);
    }

    private String text(String key, Object... args) {
        return messages.getMessage(key, args, LOCALE);
    }

    // Mismo formato que la interfaz: $1,200.00
    private static String money(BigDecimal amount) {
        DecimalFormat format = new DecimalFormat("$#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        return format.format(amount == null ? BigDecimal.ZERO : amount);
    }

    private static String capitalize(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
