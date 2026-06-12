package com.consulos.usager;

import com.consulos.usager.dto.UsagerCreateDTO;
import com.consulos.usager.dto.UsagerDetailDTO;
import com.consulos.usager.dto.UsagerSummaryDTO;
import com.consulos.usager.dto.UsagerUpdateDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface UsagerMapper {

    UsagerSummaryDTO toSummary(UsagerEntity entity);

    UsagerDetailDTO toDetail(UsagerEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organisationId", ignore = true)
    @Mapping(target = "numero", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "actif", constant = "true")
    @Mapping(target = "metadata", expression = "java(new java.util.HashMap<>())")
    UsagerEntity toEntity(UsagerCreateDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organisationId", ignore = true)
    @Mapping(target = "numero", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "actif", ignore = true)
    @Mapping(target = "metadata", ignore = true)
    void updateEntity(@MappingTarget UsagerEntity entity, UsagerUpdateDTO dto);
}
