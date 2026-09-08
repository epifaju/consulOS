package com.consulos.document.content;

public record DocumentContentPayload(
        byte[] bytes,
        String fileName,
        String mimeType
) {
}
