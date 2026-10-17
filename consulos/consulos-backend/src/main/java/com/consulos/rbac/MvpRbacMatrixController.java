package com.consulos.rbac;

import com.consulos.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rbac")
@RequiredArgsConstructor
public class MvpRbacMatrixController {

    @GetMapping("/matrix")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Map<String, List<String>>>> matrix() {
        return ApiResponse.ok(Map.of(
                "permissions", MvpRbacMatrix.PERMISSIONS,
                "nav", MvpRbacMatrix.NAV
        ));
    }
}
