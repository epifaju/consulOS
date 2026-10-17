package com.consulos.rbac;

import com.consulos.administration.dto.RoleDTO;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Superpose la matrice unique sur {@code AdminRoleService.list()} sans modifier le service legacy.
 */
@Aspect
@Component
public class AdminRolePermissionsOverlayAspect {

    @Around("execution(* com.consulos.administration.AdminRoleService.list(..))")
    public Object overlayUnifiedPermissions(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        if (!(result instanceof List<?> roles)) {
            return result;
        }
        List<RoleDTO> unified = new ArrayList<>();
        for (Object item : roles) {
            if (item instanceof RoleDTO role) {
                List<String> permissions = MvpRbacMatrix.permissionsFor(role.code());
                if (permissions.isEmpty()) {
                    unified.add(role);
                } else {
                    unified.add(new RoleDTO(role.id(), role.code(), role.libelle(), permissions));
                }
            }
        }
        return unified;
    }
}
