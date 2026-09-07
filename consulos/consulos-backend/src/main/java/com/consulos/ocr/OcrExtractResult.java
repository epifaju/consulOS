package com.consulos.ocr;

public record OcrExtractResult(boolean success, String text, String errorMessage) {

    public static OcrExtractResult ok(String text) {
        return new OcrExtractResult(true, text, null);
    }

    public static OcrExtractResult failed(String errorMessage) {
        return new OcrExtractResult(false, null, errorMessage);
    }
}
