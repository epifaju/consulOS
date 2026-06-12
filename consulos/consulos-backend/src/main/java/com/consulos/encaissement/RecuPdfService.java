package com.consulos.encaissement;

import com.consulos.auth.entity.OrganisationEntity;
import com.consulos.auth.entity.UserEntity;
import com.consulos.dossier.DossierEntity;
import com.consulos.dossier.TypeServiceEntity;
import com.consulos.usager.UsagerEntity;
import com.itextpdf.barcodes.BarcodeQRCode;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class RecuPdfService {

    private static final DeviceRgb PRIMARY = new DeviceRgb(27, 79, 114);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generateRecu(
            EncaissementEntity enc,
            UsagerEntity usager,
            DossierEntity dossier,
            TypeServiceEntity typeService,
            OrganisationEntity organisation,
            UserEntity caissier
    ) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(40, 40, 40, 40);

            document.add(new Paragraph(organisation.getNom())
                    .setFontSize(14)
                    .setFontColor(PRIMARY)
                    .setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("REÇU CONSULAIRE")
                    .setFontSize(22)
                    .setBold()
                    .setFontColor(PRIMARY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10));

            document.add(new Paragraph(enc.getNumeroRecu())
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(15));

            document.add(new Paragraph(" ").setBorderBottom(new SolidBorder(PRIMARY, 2)).setMarginBottom(15));

            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 2}))
                    .useAllAvailableWidth()
                    .setMarginBottom(15);
            addRow(infoTable, "Date", enc.getCreatedAt() != null ? enc.getCreatedAt().format(DATE_FORMAT) : "—");
            addRow(infoTable, "Mode de paiement", enc.getModePaiement() != null ? enc.getModePaiement().name() : "—");
            String caissierNom = caissier != null ? caissier.getPrenom() + " " + caissier.getNom() : "—";
            addRow(infoTable, "Caissier", caissierNom);
            addRow(infoTable, "Usager", usager.getPrenom() + " " + usager.getNom() + " (" + usager.getNumero() + ")");
            String service = typeService != null ? typeService.getLibelle() : "—";
            addRow(infoTable, "Service", service);
            addRow(infoTable, "Dossier", dossier != null ? dossier.getReference() : "—");
            document.add(infoTable);

            BigDecimal reste = enc.getMontantTotal().subtract(
                    enc.getMontantPercu() != null ? enc.getMontantPercu() : BigDecimal.ZERO
            ).setScale(2, RoundingMode.HALF_UP);

            Table montantTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1}))
                    .useAllAvailableWidth()
                    .setMarginBottom(15);
            montantTable.addHeaderCell(headerCell("Montant total"));
            montantTable.addHeaderCell(headerCell("Montant perçu"));
            montantTable.addHeaderCell(headerCell("Reste dû"));
            montantTable.addCell(dataCell(formatMontant(enc.getMontantTotal(), enc.getDevise())));
            montantTable.addCell(dataCell(formatMontant(enc.getMontantPercu(), enc.getDevise())));
            montantTable.addCell(dataCell(formatMontant(reste, enc.getDevise())));
            document.add(montantTable);

            document.add(new Paragraph("Statut : " + enc.getStatut().name())
                    .setBold()
                    .setFontColor(statutColor(enc.getStatut()))
                    .setMarginBottom(20));

            String qrPayload = buildQrPayload(enc, dossier);
            BarcodeQRCode qrCode = new BarcodeQRCode(qrPayload);
            Image qrImage = new Image(qrCode.createFormXObject(ColorConstants.BLACK, pdfDoc))
                    .setWidth(90)
                    .setHeight(90)
                    .setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
            document.add(qrImage);

            document.add(new Paragraph(
                    "Ce reçu est un justificatif officiel de paiement auprès du consulat. "
                            + "Conservez-le pour toute correspondance ultérieure.")
                    .setFontSize(9)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(15));

            document.add(new Paragraph("\n\nSignature caissier : _________________________")
                    .setFontSize(10)
                    .setMarginTop(30));

            document.close();
            return baos.toByteArray();
        } catch (Exception ex) {
            log.error("Échec génération PDF reçu {}", enc.getNumeroRecu(), ex);
            throw new com.consulos.common.BusinessException(
                    "PDF_GENERATION_ECHOUEE",
                    "Impossible de générer le reçu PDF",
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private String buildQrPayload(EncaissementEntity enc, DossierEntity dossier) {
        String ref = dossier != null ? dossier.getReference() : "";
        String date = enc.getCreatedAt() != null ? enc.getCreatedAt().toString() : "";
        return "{\"recu\":\"" + enc.getNumeroRecu()
                + "\",\"dossier\":\"" + ref
                + "\",\"montant\":" + enc.getMontantTotal()
                + ",\"date\":\"" + date + "\"}";
    }

    private void addRow(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label).setBold()).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
        table.addCell(new Cell().add(new Paragraph(value)).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
    }

    private Cell headerCell(String text) {
        return new Cell().add(new Paragraph(text).setBold())
                .setBackgroundColor(new DeviceRgb(214, 234, 248))
                .setTextAlignment(TextAlignment.CENTER);
    }

    private Cell dataCell(String text) {
        return new Cell().add(new Paragraph(text)).setTextAlignment(TextAlignment.CENTER);
    }

    private String formatMontant(BigDecimal montant, String devise) {
        if (montant == null) {
            return "0,00 " + (devise != null ? devise : "EUR");
        }
        return montant.setScale(2, RoundingMode.HALF_UP).toString().replace('.', ',')
                + " " + (devise != null ? devise : "EUR");
    }

    private DeviceRgb statutColor(StatutEncaissement statut) {
        return switch (statut) {
            case PAYE -> new DeviceRgb(30, 132, 73);
            case PARTIEL -> new DeviceRgb(183, 149, 11);
            case DU -> new DeviceRgb(192, 57, 43);
            case EXONERE -> new DeviceRgb(46, 134, 193);
            case REMBOURSE -> new DeviceRgb(108, 52, 131);
        };
    }
}
