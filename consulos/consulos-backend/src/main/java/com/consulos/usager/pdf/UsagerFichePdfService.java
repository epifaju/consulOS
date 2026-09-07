package com.consulos.usager.pdf;

import com.consulos.audit.Auditable;
import com.consulos.auth.entity.OrganisationEntity;
import com.consulos.auth.repository.OrganisationRepository;
import com.consulos.common.BusinessException;
import com.consulos.usager.UsagerEntity;
import com.consulos.usager.UsagerRepository;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsagerFichePdfService {

    private static final DeviceRgb PRIMARY = new DeviceRgb(27, 79, 114);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final UsagerRepository usagerRepository;
    private final OrganisationRepository organisationRepository;

    @Transactional(readOnly = true)
    @Auditable(action = "USAGER_FICHE_PDF_EXPORT")
    public byte[] generateFiche(UUID usagerId, UUID organisationId) {
        UsagerEntity usager = usagerRepository.findById(usagerId)
                .filter(u -> organisationId.equals(u.getOrganisationId()) && Boolean.TRUE.equals(u.getActif()))
                .orElseThrow(() -> new BusinessException(
                        "USAGER_NOT_FOUND", "Usager introuvable", HttpStatus.NOT_FOUND));

        OrganisationEntity organisation = organisationRepository.findById(organisationId).orElse(null);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(40, 40, 40, 40);

            String orgName = organisation != null ? organisation.getNom() : "ConsulOS";
            document.add(new Paragraph(orgName)
                    .setFontSize(14)
                    .setFontColor(PRIMARY)
                    .setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("FICHE USAGER")
                    .setFontSize(22)
                    .setBold()
                    .setFontColor(PRIMARY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(6));
            document.add(new Paragraph(nullSafe(usager.getNumero()))
                    .setFontSize(14)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(12));
            document.add(new Paragraph(" ")
                    .setBorderBottom(new SolidBorder(PRIMARY, 2))
                    .setMarginBottom(16));

            document.add(sectionTitle("Identité"));
            Table identity = twoColTable();
            addRow(identity, "Nom", nullSafe(usager.getNom()));
            addRow(identity, "Prénom", nullSafe(usager.getPrenom()));
            addRow(identity, "Date de naissance",
                    usager.getDateNaissance() != null ? usager.getDateNaissance().format(DATE_FORMAT) : "—");
            addRow(identity, "Lieu de naissance", nullSafe(usager.getLieuNaissance()));
            addRow(identity, "Nationalité", nullSafe(usager.getNationalite()));
            addRow(identity, "Type d'usager",
                    usager.getTypeUsager() != null ? usager.getTypeUsager().name() : "—");
            document.add(identity);

            document.add(sectionTitle("Contact"));
            Table contact = twoColTable();
            addRow(contact, "Téléphone", nullSafe(usager.getTelephone()));
            addRow(contact, "E-mail", nullSafe(usager.getEmail()));
            addRow(contact, "Adresse", nullSafe(usager.getAdresse()));
            document.add(contact);

            document.add(sectionTitle("Document d'identité"));
            Table docs = twoColTable();
            addRow(docs, "Type",
                    usager.getTypeDocument() != null ? usager.getTypeDocument().name() : "—");
            addRow(docs, "Numéro", nullSafe(usager.getNumeroDocument()));
            document.add(docs);

            document.add(sectionTitle("Métadonnées"));
            Table meta = twoColTable();
            addRow(meta, "Actif", Boolean.TRUE.equals(usager.getActif()) ? "Oui" : "Non");
            addRow(meta, "Créé le",
                    usager.getCreatedAt() != null ? usager.getCreatedAt().format(DATETIME_FORMAT) : "—");
            addRow(meta, "Mis à jour le",
                    usager.getUpdatedAt() != null ? usager.getUpdatedAt().format(DATETIME_FORMAT) : "—");
            document.add(meta);

            document.add(new Paragraph(
                    "Document généré par ConsulOS — usage interne consulaire.")
                    .setFontSize(9)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(24));

            document.close();
            return baos.toByteArray();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Échec génération PDF fiche usager {}", usagerId, ex);
            throw new BusinessException(
                    "PDF_GENERATION_ECHOUEE",
                    "Impossible de générer la fiche usager PDF",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    public String suggestedFilename(UUID usagerId, UUID organisationId) {
        return usagerRepository.findById(usagerId)
                .filter(u -> organisationId.equals(u.getOrganisationId()))
                .map(u -> "fiche-" + u.getNumero() + ".pdf")
                .orElse("fiche-usager.pdf");
    }

    private Paragraph sectionTitle(String title) {
        return new Paragraph(title)
                .setBold()
                .setFontColor(PRIMARY)
                .setFontSize(12)
                .setMarginTop(12)
                .setMarginBottom(6);
    }

    private Table twoColTable() {
        return new Table(UnitValue.createPercentArray(new float[]{1, 2}))
                .useAllAvailableWidth()
                .setMarginBottom(8);
    }

    private void addRow(Table table, String label, String value) {
        table.addCell(new Cell()
                .add(new Paragraph(label).setBold())
                .setBackgroundColor(new DeviceRgb(214, 234, 248))
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
        table.addCell(new Cell()
                .add(new Paragraph(value))
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
    }

    private String nullSafe(String value) {
        return value != null && !value.isBlank() ? value : "—";
    }
}
