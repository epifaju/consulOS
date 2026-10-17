package com.consulos.document.query;

import com.consulos.document.DocumentEntity;
import com.consulos.document.StatutControle;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class DocumentSpecificationsTest {

    @Mock
    private Root<DocumentEntity> root;
    @Mock
    private CriteriaQuery<?> query;
    @Mock
    private CriteriaBuilder cb;
    @Mock
    private Path organisationPath;
    @Mock
    private Path actifPath;
    @Mock
    private Path fieldPath;
    @Mock
    private Predicate predicate;
    @Mock
    private Predicate orgPredicate;
    @Mock
    private Predicate actifPredicate;

    @Test
    void forOrganisation_shouldFilterTenantAndActif() {
        UUID organisationId = UUID.randomUUID();
        Specification<DocumentEntity> spec = DocumentSpecifications.forOrganisation(organisationId);

        when(root.get("organisationId")).thenReturn(organisationPath);
        when(root.get("actif")).thenReturn(actifPath);
        when(cb.equal(organisationPath, organisationId)).thenReturn(orgPredicate);
        when(cb.equal(actifPath, true)).thenReturn(actifPredicate);
        when(cb.and(orgPredicate, actifPredicate)).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
        verify(cb).equal(organisationPath, organisationId);
        verify(cb).equal(actifPath, true);
    }

    @Test
    void withTypeDocument_blank_shouldReturnNull() {
        assertThat(DocumentSpecifications.withTypeDocument(null)).isNull();
        assertThat(DocumentSpecifications.withTypeDocument("  ")).isNull();
    }

    @Test
    void withTypeDocument_present_shouldEqual() {
        Specification<DocumentEntity> spec = DocumentSpecifications.withTypeDocument("PASSEPORT");
        when(root.get("typeDocument")).thenReturn(fieldPath);
        when(cb.equal(fieldPath, "PASSEPORT")).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
    }

    @Test
    void withStatuts_nullOrEmpty_shouldReturnNull() {
        assertThat(DocumentSpecifications.withStatuts(null)).isNull();
        assertThat(DocumentSpecifications.withStatuts(List.of())).isNull();
    }

    @Test
    void withStatuts_present_shouldIn() {
        List<StatutControle> statuts = List.of(StatutControle.RECU, StatutControle.VALIDE);
        Specification<DocumentEntity> spec = DocumentSpecifications.withStatuts(statuts);
        when(root.get("statutControle")).thenReturn(fieldPath);
        when(fieldPath.in(statuts)).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
    }

    @Test
    void withSearch_blank_shouldReturnNull() {
        assertThat(DocumentSpecifications.withSearch(null)).isNull();
        assertThat(DocumentSpecifications.withSearch("   ")).isNull();
    }

    @Test
    void withSearch_shouldLikeLowerTrimmed() {
        Specification<DocumentEntity> spec = DocumentSpecifications.withSearch("  Scan  ");
        when(root.get("nomOriginal")).thenReturn(fieldPath);
        when(cb.lower(fieldPath)).thenReturn((Expression) fieldPath);
        when(cb.like(fieldPath, "%scan%")).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
        verify(cb).like(fieldPath, "%scan%");
    }

    @Test
    void withDossierReference_blank_shouldReturnNull() {
        assertThat(DocumentSpecifications.withDossierReference(null, UUID.randomUUID())).isNull();
        assertThat(DocumentSpecifications.withDossierReference("  ", UUID.randomUUID())).isNull();
    }

    @Test
    void withDossierReference_present_shouldBuildNonNullSpec() {
        assertThat(DocumentSpecifications.withDossierReference("DOS-1", UUID.randomUUID())).isNotNull();
    }

    @Test
    void withCreatedBetween_bothNull_shouldReturnNull() {
        assertThat(DocumentSpecifications.withCreatedBetween(null, null)).isNull();
    }

    @Test
    void withCreatedBetween_both_shouldBetween() {
        OffsetDateTime debut = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime fin = OffsetDateTime.of(2026, 1, 31, 0, 0, 0, 0, ZoneOffset.UTC);
        Specification<DocumentEntity> spec = DocumentSpecifications.withCreatedBetween(debut, fin);
        when(root.get("createdAt")).thenReturn(fieldPath);
        when(cb.between(fieldPath, debut, fin)).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
    }

    @Test
    void withCreatedBetween_debutOnly_shouldGreaterOrEqual() {
        OffsetDateTime debut = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        Specification<DocumentEntity> spec = DocumentSpecifications.withCreatedBetween(debut, null);
        when(root.get("createdAt")).thenReturn(fieldPath);
        when(cb.greaterThanOrEqualTo(fieldPath, debut)).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
    }

    @Test
    void withCreatedBetween_finOnly_shouldLessOrEqual() {
        OffsetDateTime fin = OffsetDateTime.of(2026, 1, 31, 0, 0, 0, 0, ZoneOffset.UTC);
        Specification<DocumentEntity> spec = DocumentSpecifications.withCreatedBetween(null, fin);
        when(root.get("createdAt")).thenReturn(fieldPath);
        when(cb.lessThanOrEqualTo(fieldPath, fin)).thenReturn(predicate);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(predicate);
    }
}
