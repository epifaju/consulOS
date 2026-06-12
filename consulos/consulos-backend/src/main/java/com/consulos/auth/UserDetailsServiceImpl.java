package com.consulos.auth;

import com.consulos.auth.entity.UserEntity;
import com.consulos.auth.repository.UserRepository;
import com.consulos.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable: " + email));
        return toUserDetails(user);
    }

    @Transactional(readOnly = true)
    public ConsulosUserDetails loadUserById(java.util.UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Utilisateur introuvable", HttpStatus.NOT_FOUND));
        return toUserDetails(user);
    }

    private ConsulosUserDetails toUserDetails(UserEntity user) {
        return new ConsulosUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getOrganisationId(),
                user.getRole().getCode(),
                Boolean.TRUE.equals(user.getActif())
        );
    }
}
