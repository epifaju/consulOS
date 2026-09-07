package com.consulos.ocr;

public interface OcrProvider {

    String providerName();

    OcrExtractResult extract(String fileName, String mimeType, byte[] content);
}
