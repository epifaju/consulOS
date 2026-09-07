package com.consulos.sms;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SmsLogRepository extends JpaRepository<SmsLogEntity, UUID> {
}
