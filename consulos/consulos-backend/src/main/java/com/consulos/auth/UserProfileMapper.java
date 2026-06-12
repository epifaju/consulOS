package com.consulos.auth;

import com.consulos.auth.dto.UserProfileDTO;
import com.consulos.auth.entity.OrganisationEntity;
import com.consulos.auth.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserProfileMapper {

    public UserProfileDTO toDto(UserEntity user, OrganisationEntity organisation) {
        return new UserProfileDTO(
                user.getId(),
                user.getEmail(),
                user.getNom(),
                user.getPrenom(),
                user.getRole().getCode(),
                user.getOrganisationId(),
                organisation != null ? organisation.getNom() : null,
                organisation != null ? organisation.getCode() : null
        );
    }

    public UserProfileDTO toDto(ConsulosUserDetails userDetails, OrganisationEntity organisation) {
        return new UserProfileDTO(
                userDetails.getUserId(),
                userDetails.getEmail(),
                null,
                null,
                userDetails.getRole(),
                userDetails.getOrganisationId(),
                organisation != null ? organisation.getNom() : null,
                organisation != null ? organisation.getCode() : null
        );
    }
}
