package com.consulos.dossier;

import com.consulos.dossier.dto.TypeServiceDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TypeServiceMapper {

    TypeServiceDTO toDto(TypeServiceEntity entity);
}
