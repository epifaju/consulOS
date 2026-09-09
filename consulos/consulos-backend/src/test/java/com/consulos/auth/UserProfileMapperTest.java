package com.consulos.auth;

import com.consulos.auth.dto.UserProfileDTO;
import com.consulos.auth.entity.OrganisationEntity;
import com.consulos.auth.entity.RoleEntity;
import com.consulos.auth.entity.UserEntity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserProfileMapperTest {

    private final UserProfileMapper mapper = new UserProfileMapper();

    @Test
    void toDto_fromUserEntity_shouldMapRoleAndOrganisation() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        RoleEntity role = new RoleEntity();
        role.setCode("ROLE_AGENT");

        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setEmail("agent@consulos.local");
        user.setNom("Diallo");
        user.setPrenom("Fatou");
        user.setOrganisationId(orgId);
        user.setRole(role);

        OrganisationEntity org = new OrganisationEntity();
        org.setId(orgId);
        org.setNom("Consulat Paris");
        org.setCode("GNB-FR");

        UserProfileDTO dto = mapper.toDto(user, org);

        assertThat(dto.id()).isEqualTo(userId);
        assertThat(dto.email()).isEqualTo("agent@consulos.local");
        assertThat(dto.nom()).isEqualTo("Diallo");
        assertThat(dto.prenom()).isEqualTo("Fatou");
        assertThat(dto.role()).isEqualTo("ROLE_AGENT");
        assertThat(dto.organisationId()).isEqualTo(orgId);
        assertThat(dto.organisationNom()).isEqualTo("Consulat Paris");
        assertThat(dto.organisationCode()).isEqualTo("GNB-FR");
    }

    @Test
    void toDto_fromUserDetails_whenOrgNull_shouldLeaveOrgFieldsNull() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        ConsulosUserDetails details = new ConsulosUserDetails(
                userId, "admin@consulos.local", "hash", orgId, "ROLE_ADMIN", true
        );

        UserProfileDTO dto = mapper.toDto(details, null);

        assertThat(dto.id()).isEqualTo(userId);
        assertThat(dto.nom()).isNull();
        assertThat(dto.prenom()).isNull();
        assertThat(dto.role()).isEqualTo("ROLE_ADMIN");
        assertThat(dto.organisationNom()).isNull();
        assertThat(dto.organisationCode()).isNull();
    }
}
