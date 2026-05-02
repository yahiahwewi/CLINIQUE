package com.example.demo.service;

import com.example.demo.entity.Prescription;
import com.example.demo.entity.PrescriptionItem;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PrescriptionPdfService {

    private static final Color LUMEN_700 = new Color(0x0F, 0x4C, 0x5C);
    private static final Color INK_500   = new Color(0x6B, 0x66, 0x60);
    private static final Color INK_700   = new Color(0x3A, 0x36, 0x31);

    public byte[] render(Prescription rx) {
        try {
            Document doc = new Document(PageSize.A4, 48, 48, 56, 56);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font brand = new Font(Font.HELVETICA, 18, Font.BOLD, LUMEN_700);
            Font sub   = new Font(Font.HELVETICA, 9, Font.BOLD, INK_500);
            Font h2    = new Font(Font.HELVETICA, 13, Font.BOLD, LUMEN_700);
            Font body  = new Font(Font.HELVETICA, 11, Font.NORMAL, INK_700);
            Font bodyB = new Font(Font.HELVETICA, 11, Font.BOLD, INK_700);
            Font small = new Font(Font.HELVETICA, 9, Font.NORMAL, INK_500);

            // Header
            Paragraph header = new Paragraph("LUMEN HEALTH", brand);
            header.setSpacingAfter(2f);
            doc.add(header);
            Paragraph tag = new Paragraph("CARE, COORDINATED.", sub);
            tag.setSpacingAfter(20f);
            doc.add(tag);

            // Patient + doctor + date block
            PdfPTable meta = new PdfPTable(2);
            meta.setWidthPercentage(100);
            meta.setSpacingAfter(20f);
            meta.addCell(metaCell("PATIENT", small));
            meta.addCell(metaCell("PRESCRIBED BY", small));
            meta.addCell(metaCell(fullName(rx.getPatient().getFirstName(), rx.getPatient().getLastName()), bodyB));
            meta.addCell(metaCell("Dr. " + fullName(rx.getDoctor().getFirstName(), rx.getDoctor().getLastName()), bodyB));
            meta.addCell(metaCell(rx.getPatient().getEmail(), small));
            meta.addCell(metaCell(rx.getDoctor().getEmail(), small));
            doc.add(meta);

            PdfPTable date = new PdfPTable(2);
            date.setWidthPercentage(100);
            date.setSpacingAfter(20f);
            date.addCell(metaCell("ISSUED", small));
            date.addCell(metaCell("PRESCRIPTION ID", small));
            date.addCell(metaCell(rx.getCreatedAt().format(DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' HH:mm")), body));
            date.addCell(metaCell("#" + rx.getId(), body));
            doc.add(date);

            // Title
            Paragraph rxTitle = new Paragraph("Rx", h2);
            rxTitle.setSpacingAfter(8f);
            doc.add(rxTitle);

            // Items table
            PdfPTable items = new PdfPTable(new float[]{3.2f, 1.4f, 1.8f, 1f});
            items.setWidthPercentage(100);
            items.setSpacingAfter(16f);

            items.addCell(headerCell("Medication", small));
            items.addCell(headerCell("Dose", small));
            items.addCell(headerCell("Frequency", small));
            items.addCell(headerCell("Days", small));

            for (PrescriptionItem item : rx.getItems()) {
                items.addCell(bodyCell(item.getDrugName(), bodyB));
                items.addCell(bodyCell(orDash(item.getDose()), body));
                items.addCell(bodyCell(orDash(item.getFrequency()), body));
                items.addCell(bodyCell(item.getDurationDays() == null ? "—" : item.getDurationDays().toString(), body));
                if (item.getNotes() != null && !item.getNotes().isBlank()) {
                    PdfPCell notes = new PdfPCell(new Phrase("↳ " + item.getNotes(), small));
                    notes.setColspan(4);
                    notes.setBorder(Rectangle.BOTTOM);
                    notes.setBorderColorBottom(new Color(0xE6, 0xE0, 0xD2));
                    notes.setPaddingTop(2f);
                    notes.setPaddingBottom(8f);
                    notes.setPaddingLeft(8f);
                    items.addCell(notes);
                }
            }
            doc.add(items);

            if (rx.getInstructions() != null && !rx.getInstructions().isBlank()) {
                Paragraph instTitle = new Paragraph("Instructions", h2);
                instTitle.setSpacingAfter(6f);
                doc.add(instTitle);
                Paragraph inst = new Paragraph(rx.getInstructions(), body);
                inst.setSpacingAfter(20f);
                doc.add(inst);
            }

            // Signature
            Paragraph sig = new Paragraph("Signed,", small);
            sig.setSpacingBefore(40f);
            doc.add(sig);
            Paragraph sigName = new Paragraph(
                    "Dr. " + fullName(rx.getDoctor().getFirstName(), rx.getDoctor().getLastName()), bodyB);
            doc.add(sigName);

            // Footer
            Paragraph footer = new Paragraph(
                    "Generated by Lumen Health · This document is for clinical use. Verify identity before dispensing.",
                    small);
            footer.setSpacingBefore(40f);
            doc.add(footer);

            doc.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate prescription PDF", e);
        }
    }

    private String fullName(String first, String last) {
        return ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
    }

    private String orDash(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    private PdfPCell metaCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(2f);
        return cell;
    }

    private PdfPCell headerCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text.toUpperCase(), font));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColorBottom(LUMEN_700);
        cell.setPadding(8f);
        cell.setBackgroundColor(new Color(0xF6, 0xF1, 0xE7));
        return cell;
    }

    private PdfPCell bodyCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColorBottom(new Color(0xE6, 0xE0, 0xD2));
        cell.setPadding(8f);
        return cell;
    }
}
