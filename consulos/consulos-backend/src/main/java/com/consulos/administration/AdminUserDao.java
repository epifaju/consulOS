package com.consulos.administration;

import com.consulos.auth.entity.UserEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AdminUserDao {

    @PersistenceContext
    private EntityManager entityManager;

    public Page<UserEntity> findByOrganisation(UUID organisationId, Pageable pageable) {
        TypedQuery<UserEntity> query = entityManager.createQuery("""
                SELECT u FROM UserEntity u
                JOIN FETCH u.role
                WHERE u.organisationId = :orgId
                ORDER BY u.nom, u.prenom
                """, UserEntity.class);
        query.setParameter("orgId", organisationId);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        List<UserEntity> content = query.getResultList();

        Long total = entityManager.createQuery("""
                SELECT COUNT(u) FROM UserEntity u WHERE u.organisationId = :orgId
                """, Long.class)
                .setParameter("orgId", organisationId)
                .getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    public Optional<UserEntity> findByIdAndOrganisation(UUID id, UUID organisationId) {
        List<UserEntity> results = entityManager.createQuery("""
                SELECT u FROM UserEntity u
                JOIN FETCH u.role
                WHERE u.id = :id AND u.organisationId = :orgId
                """, UserEntity.class)
                .setParameter("id", id)
                .setParameter("orgId", organisationId)
                .getResultList();
        return results.stream().findFirst();
    }

    public boolean existsByEmail(String email) {
        Long count = entityManager.createQuery("""
                SELECT COUNT(u) FROM UserEntity u WHERE LOWER(u.email) = LOWER(:email)
                """, Long.class)
                .setParameter("email", email)
                .getSingleResult();
        return count > 0;
    }
}
