package com.consulos.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentSessionRepository extends JpaRepository<PaymentSessionEntity, UUID> {

    Optional<PaymentSessionEntity> findByProviderSessionId(String providerSessionId);

    Optional<PaymentSessionEntity> findByIdAndOrganisationId(UUID id, UUID organisationId);
}
