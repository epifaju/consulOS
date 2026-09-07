package com.consulos.notification;

import com.consulos.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "notifications")
public class NotificationEntity extends BaseEntity {

    @Column(name = "recipient_user_id", nullable = false)
    private UUID recipientUserId;

    @Column(name = "type_notification", nullable = false, length = 50)
    private String typeNotification;

    @Column(name = "titre", nullable = false, length = 200)
    private String titre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(length = 500)
    private String lien;

    @Column(nullable = false)
    private Boolean lu = false;

    @Column(name = "lu_at")
    private LocalDateTime luAt;
}
