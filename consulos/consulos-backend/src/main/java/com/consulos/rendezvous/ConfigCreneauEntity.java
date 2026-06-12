package com.consulos.rendezvous;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "config_creneaux")
public class ConfigCreneauEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organisation_id", nullable = false)
    private UUID organisationId;

    @Column(name = "type_service_id")
    private UUID typeServiceId;

    @Column(name = "jour_semaine", nullable = false)
    private Integer jourSemaine;

    @Column(name = "heure_debut", nullable = false)
    private LocalTime heureDebut;

    @Column(name = "heure_fin", nullable = false)
    private LocalTime heureFin;

    @Column(name = "duree_minutes")
    private Integer dureeMinutes = 30;

    @Column(name = "quota_max")
    private Integer quotaMax = 10;

    private Boolean actif = true;
}
