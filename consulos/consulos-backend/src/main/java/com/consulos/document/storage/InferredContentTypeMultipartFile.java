package com.consulos.document.storage;

import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

final class InferredContentTypeMultipartFile implements MultipartFile {

    private final MultipartFile delegate;
    private final String contentType;

    private InferredContentTypeMultipartFile(MultipartFile delegate, String contentType) {
        this.delegate = delegate;
        this.contentType = contentType;
    }

    static MultipartFile withInferredType(MultipartFile file) {
        if (file == null || StringUtils.hasText(file.getContentType())) {
            return file;
        }
        String inferred = infer(file.getOriginalFilename());
        if (inferred == null) {
            return file;
        }
        return new InferredContentTypeMultipartFile(file, inferred);
    }

    private static String infer(String filename) {
        if (!StringUtils.hasText(filename)) {
            return null;
        }
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return null;
    }

    @Override
    @NonNull
    public String getName() {
        return delegate.getName();
    }

    @Override
    public String getOriginalFilename() {
        return delegate.getOriginalFilename();
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public long getSize() {
        return delegate.getSize();
    }

    @Override
    @NonNull
    public byte[] getBytes() throws IOException {
        return delegate.getBytes();
    }

    @Override
    @NonNull
    public InputStream getInputStream() throws IOException {
        return delegate.getInputStream();
    }

    @Override
    public void transferTo(@NonNull File dest) throws IOException, IllegalStateException {
        delegate.transferTo(dest);
    }
}
