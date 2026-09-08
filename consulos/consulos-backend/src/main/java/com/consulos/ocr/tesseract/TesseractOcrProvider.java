package com.consulos.ocr.tesseract;

import com.consulos.ocr.OcrExtractResult;
import com.consulos.ocr.OcrProvider;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.util.PdfUtilities;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
public class TesseractOcrProvider implements OcrProvider {

    private final TesseractOcrProperties properties;
    private final Path resolvedDatapath;

    public TesseractOcrProvider(TesseractOcrProperties properties) {
        this.properties = properties;
        this.resolvedDatapath = resolveDatapath(properties.getDatapath());
    }

    @Override
    public String providerName() {
        return "tesseract";
    }

    @Override
    public OcrExtractResult extract(String fileName, String mimeType, byte[] content) {
        if (content == null || content.length == 0) {
            return OcrExtractResult.failed("Contenu document vide");
        }
        if (resolvedDatapath == null) {
            return OcrExtractResult.failed(
                    "Tessdata introuvable. Définir CONSULOS_OCR_TESSERACT_DATAPATH ou lancer scripts/download-tessdata.ps1");
        }

        Path temp = null;
        try {
            ITesseract tesseract = createEngine();
            String mime = mimeType != null ? mimeType.toLowerCase(Locale.ROOT) : "";
            String name = fileName != null ? fileName.toLowerCase(Locale.ROOT) : "";
            boolean pdf = mime.contains("pdf") || name.endsWith(".pdf");

            String raw;
            if (pdf) {
                temp = Files.createTempFile("consulos-ocr-", ".pdf");
                Files.write(temp, content);
                raw = ocrPdf(tesseract, temp.toFile());
            } else {
                try (InputStream in = new ByteArrayInputStream(content)) {
                    BufferedImage image = ImageIO.read(in);
                    if (image == null) {
                        return OcrExtractResult.failed("Format image non supporté pour OCR Tesseract");
                    }
                    raw = tesseract.doOCR(image);
                }
            }

            if (raw == null || raw.isBlank()) {
                return OcrExtractResult.failed("Tesseract n'a extrait aucun texte");
            }
            return OcrExtractResult.ok(OcrTextStructurer.enrich(raw, fileName));
        } catch (UnsatisfiedLinkError | NoClassDefFoundError ex) {
            log.error("Bibliothèques natives Tesseract indisponibles: {}", ex.toString());
            return OcrExtractResult.failed(
                    "Natif Tesseract/Leptonica indisponible (Visual C++ Redistributable requis sous Windows)");
        } catch (TesseractException ex) {
            log.warn("Échec Tesseract: {}", ex.getMessage());
            return OcrExtractResult.failed("Échec OCR Tesseract: " + ex.getMessage());
        } catch (Exception ex) {
            log.warn("Erreur OCR Tesseract: {}", ex.getMessage());
            return OcrExtractResult.failed("Erreur OCR Tesseract: " + ex.getMessage());
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // best effort
                }
            }
        }
    }

    private String ocrPdf(ITesseract tesseract, File pdfFile) throws TesseractException, IOException {
        List<BufferedImage> pages = renderPdfPages(pdfFile);
        if (!pages.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            int maxPages = Math.min(pages.size(), 5);
            for (int i = 0; i < maxPages; i++) {
                String pageText = tesseract.doOCR(pages.get(i));
                if (pageText != null && !pageText.isBlank()) {
                    if (!sb.isEmpty()) {
                        sb.append("\n\n--- page ").append(i + 1).append(" ---\n");
                    }
                    sb.append(pageText.trim());
                }
            }
            if (!sb.isEmpty()) {
                return sb.toString();
            }
        }
        // Fallback : OCR direct du fichier PDF (si supporté par Tess4J)
        return tesseract.doOCR(pdfFile);
    }

    private List<BufferedImage> renderPdfPages(File pdfFile) {
        List<BufferedImage> images = new ArrayList<>();
        File[] pngFiles = null;
        try {
            pngFiles = PdfUtilities.convertPdf2Png(pdfFile);
            if (pngFiles == null) {
                return images;
            }
            for (File png : pngFiles) {
                if (png == null) {
                    continue;
                }
                try {
                    BufferedImage img = ImageIO.read(png);
                    if (img != null) {
                        images.add(img);
                    }
                } finally {
                    //noinspection ResultOfMethodCallIgnored
                    png.delete();
                }
            }
        } catch (Throwable ex) {
            log.debug("Rendu PDF→PNG indisponible: {}", ex.toString());
        }
        return images;
    }

    private ITesseract createEngine() {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(resolvedDatapath.toAbsolutePath().toString());
        tesseract.setLanguage(properties.getLanguage() != null ? properties.getLanguage() : "fra+eng");
        tesseract.setVariable("user_defined_dpi", String.valueOf(Math.max(72, properties.getDpi())));
        return tesseract;
    }

    private static Path resolveDatapath(String configured) {
        List<Path> candidates = new ArrayList<>();
        if (configured != null && !configured.isBlank()) {
            candidates.add(Path.of(configured));
        }
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        candidates.add(userDir.resolve("tessdata"));
        candidates.add(userDir.resolve("..").resolve("..").resolve("tessdata").normalize());
        candidates.add(userDir.resolve("..").resolve("tessdata").normalize());

        for (Path candidate : candidates) {
            Path normalized = candidate.toAbsolutePath().normalize();
            if (Files.isDirectory(normalized) && hasAnyTrainedData(normalized)) {
                log.info("Tessdata résolu: {}", normalized);
                return normalized;
            }
        }
        log.warn("Aucun dossier tessdata valide trouvé (config={})", configured);
        return null;
    }

    private static boolean hasAnyTrainedData(Path dir) {
        try (var stream = Files.list(dir)) {
            return stream.anyMatch(p -> p.getFileName().toString().endsWith(".traineddata"));
        } catch (IOException ex) {
            return false;
        }
    }
}
