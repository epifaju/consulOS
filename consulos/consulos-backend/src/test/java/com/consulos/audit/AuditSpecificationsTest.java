package com.consulos.audit;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class AuditSpecificationsTest {

    @Mock
    private Root<AuditLogEntity> root;
    @Mock
    private CriteriaQuery<?> query;
    @Mock
    private CriteriaBuilder cb;
    @Mock
    private Path path;
    @Mock
    private Predicate predicate;

    @Test
    void forOrganisation_shouldEqual() {
        UUID organisationId = UUID.randomUUID();
        Specification<AuditLogEntity> spec = AuditSpecifications.forOrganisation(organisationId);
        when(root.get("organisationId")).thenReturn(path);
        when(cb.equal(path, organisationId)).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
    }

    @Test
    void withAction_nullOrBlank_shouldReturnNull() {
        assertThat(AuditSpecifications.withAction(null)).isNull();
        assertThat(AuditSpecifications.withAction("  ")).isNull();
    }

    @Test
    void withAction_present_shouldEqual() {
        Specification<AuditLogEntity> spec = AuditSpecifications.withAction("DOSSIER_CREATE");
        when(root.get("action")).thenReturn(path);
        when(cb.equal(path, "DOSSIER_CREATE")).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
    }

    @Test
    void withEntiteType_nullOrBlank_shouldReturnNull() {
        assertThat(AuditSpecifications.withEntiteType(null)).isNull();
        assertThat(AuditSpecifications.withEntiteType("")).isNull();
    }

    @Test
    void withEntiteType_present_shouldEqual() {
        Specification<AuditLogEntity> spec = AuditSpecifications.withEntiteType("DOSSIER");
        when(root.get("entiteType")).thenReturn(path);
        when(cb.equal(path, "DOSSIER")).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
    }

    @Test
    void withUserId_null_shouldReturnNull() {
        assertThat(AuditSpecifications.withUserId(null)).isNull();
    }

    @Test
    void withUserId_present_shouldEqual() {
        UUID userId = UUID.randomUUID();
        Specification<AuditLogEntity> spec = AuditSpecifications.withUserId(userId);
        when(root.get("userId")).thenReturn(path);
        when(cb.equal(path, userId)).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
    }

    @Test
    void betweenDates_bothNull_shouldReturnNull() {
        assertThat(AuditSpecifications.betweenDates(null, null)).isNull();
    }

    @Test
    void betweenDates_bothPresent_shouldBetween() {
        OffsetDateTime debut = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime fin = OffsetDateTime.of(2026, 1, 31, 0, 0, 0, 0, ZoneOffset.UTC);
        Specification<AuditLogEntity> spec = AuditSpecifications.betweenDates(debut, fin);
        when(root.get("createdAt")).thenReturn(path);
        when(cb.between(path, debut, fin)).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
        verify(cb).between(path, debut, fin);
    }

    @Test
    void betweenDates_debutOnly_shouldGreaterThanOrEqual() {
        OffsetDateTime debut = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        Specification<AuditLogEntity> spec = AuditSpecifications.betweenDates(debut, null);
        when(root.get("createdAt")).thenReturn(path);
        when(cb.greaterThanOrEqualTo(path, debut)).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
    }

    @Test
    void betweenDates_finOnly_shouldLessThanOrEqual() {
        OffsetDateTime fin = OffsetDateTime.of(2026, 1, 31, 0, 0, 0, 0, ZoneOffset.UTC);
        Specification<AuditLogEntity> spec = AuditSpecifications.betweenDates(null, fin);
        when(root.get("createdAt")).thenReturn(path);
        when(cb.lessThanOrEqualTo(path, fin)).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
    }
}
