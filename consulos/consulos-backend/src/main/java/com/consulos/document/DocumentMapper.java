package com.consulos.document;

import com.consulos.document.dto.DocumentDTO;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

    public DocumentDTO toDto(DocumentEntity entity) {
        return new DocumentDTO(
                entity.getId(),
                entity.getDossierId(),
                entity.getUsagerId(),
                entity.getNomFichier(),
                entity.getNomOriginal(),
                entity.getTypeDocument(),
                entity.getMimeType(),
                entity.getTailleOctets(),
                entity.getStatutControle(),
                entity.getVersion(),
                entity.getNotes(),
                entity.getCreatedAt()
        );
    }
}
