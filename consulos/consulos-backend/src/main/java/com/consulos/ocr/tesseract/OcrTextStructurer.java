package com.consulos.ocr.tesseract;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enrichit le texte brut Tesseract avec des lignes KEY: value
 * pour le pré-remplissage ({@code OcrSuggestionService}).
 */
public final class OcrTextStructurer {

    private static final Pattern DATE_ISO = Pattern.compile("\\b(19|20)\\d{2}[-/.](0[1-9]|1[0-2])[-/.](0[1-9]|[12]\\d|3[01])\\b");
    private static final Pattern DATE_FR = Pattern.compile("\\b(0[1-9]|[12]\\d|3[01])[-/.](0[1-9]|1[0-2])[-/.](19|20)\\d{2}\\b");
    private static final Pattern PASSPORT = Pattern.compile("\\bP[A-Z0-9]{6,9}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern NATIONALITE = Pattern.compile("\\b(GNB|SEN|FRA|PRT|CIV|MLI|BFA|NER|GIN|COD|CMR)\\b", Pattern.CASE_INSENSITIVE);

    private OcrTextStructurer() {
    }

    public static String enrich(String rawText, String fileName) {
        String raw = rawText != null ? rawText.trim() : "";
        StringBuilder out = new StringBuilder();
        out.append("[OCR tesseract]\n");
        if (fileName != null) {
            out.append("Fichier: ").append(fileName).append('\n');
        }
        out.append("---\n");
        out.append(raw);
        out.append("\n---\n");

        String upper = raw.toUpperCase(Locale.ROOT);
        String nom = extractAfterLabel(upper, "NOM", "NAME", "SURNAME");
        String prenom = extractAfterLabel(upper, "PRENOM", "PRÉNOM", "GIVEN", "FIRST NAME");
        String date = extractDate(raw);
        String nationalite = extractNationalite(raw);
        String numero = extractDocumentNumber(raw, fileName);
        String typeDoc = guessTypeDocument(fileName, upper);

        if (nom != null) {
            out.append("NOM: ").append(toTitle(nom)).append('\n');
        }
        if (prenom != null) {
            out.append("PRENOM: ").append(toTitle(prenom)).append('\n');
        }
        if (date != null) {
            out.append("DATE_NAISSANCE: ").append(date).append('\n');
        }
        if (nationalite != null) {
            out.append("NATIONALITE: ").append(nationalite.toUpperCase(Locale.ROOT)).append('\n');
        }
        if (numero != null) {
            out.append("NUMERO_DOCUMENT: ").append(numero.toUpperCase(Locale.ROOT)).append('\n');
        }
        if (typeDoc != null) {
            out.append("TYPE_DOCUMENT: ").append(typeDoc).append('\n');
        }
        return out.toString();
    }

    private static String extractAfterLabel(String upperText, String... labels) {
        for (String label : labels) {
            Pattern p = Pattern.compile(
                    "(?m)^\\s*" + Pattern.quote(label) + "\\s*[:\\-]?\\s*([A-ZÀ-Ü][A-ZÀ-Ü'\\- ]{1,40})\\s*$",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            Matcher m = p.matcher(upperText);
            if (m.find()) {
                return m.group(1).trim();
            }
        }
        return null;
    }

    private static String extractDate(String text) {
        Matcher iso = DATE_ISO.matcher(text);
        if (iso.find()) {
            return iso.group().replace('.', '-').replace('/', '-');
        }
        Matcher fr = DATE_FR.matcher(text);
        if (fr.find()) {
            String[] p = fr.group().split("[-/.]");
            return p[2] + "-" + p[1] + "-" + p[0];
        }
        return null;
    }

    private static String extractNationalite(String text) {
        Matcher m = NATIONALITE.matcher(text);
        return m.find() ? m.group() : null;
    }

    private static String extractDocumentNumber(String text, String fileName) {
        Matcher m = PASSPORT.matcher(text);
        if (m.find()) {
            return m.group();
        }
        if (fileName != null && fileName.toLowerCase(Locale.ROOT).contains("cni")) {
            Matcher cni = Pattern.compile("\\b\\d{6,12}\\b").matcher(text);
            if (cni.find()) {
                return cni.group();
            }
        }
        return null;
    }

    private static String guessTypeDocument(String fileName, String upperText) {
        String seed = ((fileName != null ? fileName : "") + " " + upperText).toUpperCase(Locale.ROOT);
        if (seed.contains("PASSEPORT") || seed.contains("PASSPORT")) {
            return "PASSEPORT";
        }
        if (seed.contains("CNI") || seed.contains("IDENTITE") || seed.contains("IDENTITY")) {
            return "CNI";
        }
        if (seed.contains("SEJOUR") || seed.contains("RESIDENCE")) {
            return "TITRE_SEJOUR";
        }
        return null;
    }

    private static String toTitle(String value) {
        String[] parts = value.toLowerCase(Locale.ROOT).split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }
}
