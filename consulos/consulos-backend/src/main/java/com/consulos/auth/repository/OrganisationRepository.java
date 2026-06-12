package com.consulos.auth.repository;

import com.consulos.auth.entity.OrganisationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrganisationRepository extends JpaRepository<OrganisationEntity, UUID> {
}
