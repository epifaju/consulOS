package com.consulos.rendezvous;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "jours_fermeture")
public class JourFermetureEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organisation_id", nullable = false)
    private UUID organisationId;

    @Column(name = "date_fermeture", nullable = false)
    private LocalDate dateFermeture;

    @Column(length = 200)
    private String motif;
}
