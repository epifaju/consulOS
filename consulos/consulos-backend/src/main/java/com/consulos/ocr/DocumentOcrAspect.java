package com.consulos.ocr;

import com.consulos.document.dto.DocumentDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "consulos.ocr", name = "enabled", havingValue = "true")
public class DocumentOcrAspect {

    private final OcrService ocrService;

    @AfterReturning(
            pointcut = "execution(* com.consulos.document.DocumentService.upload(..))",
            returning = "dto"
    )
    public void afterDocumentUpload(DocumentDTO dto) {
        if (dto == null || dto.id() == null) {
            return;
        }
        try {
            ocrService.processAsyncByDocumentId(dto.id());
        } catch (Exception ex) {
            log.warn("Déclenchement OCR échoué pour {}: {}", dto.id(), ex.getMessage());
        }
    }
}
