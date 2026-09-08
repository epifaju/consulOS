package com.consulos.ocr.tesseract;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "consulos.ocr.tesseract")
public class TesseractOcrProperties {

    /** Dossier contenant *.traineddata (ex. ./tessdata). */
    private String datapath = "tessdata";

    /** Codes langue Tesseract, ex. fra+eng. */
    private String language = "fra+eng";

    /** DPI pour le rendu PDF. */
    private int dpi = 300;

    public String getDatapath() {
        return datapath;
    }

    public void setDatapath(String datapath) {
        this.datapath = datapath;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getDpi() {
        return dpi;
    }

    public void setDpi(int dpi) {
        this.dpi = dpi;
    }
}
