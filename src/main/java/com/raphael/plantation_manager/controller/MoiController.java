package com.raphael.plantation_manager.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

/** Qui est connecté : utilisé par les pages pour afficher le bon menu. */
@RestController
public class MoiController {

    @GetMapping("/api/moi")
    public Map<String, Object> moi(Authentication auth) {
        boolean patron = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return Map.of("utilisateur", auth.getName(), "patron", patron);
    }
}
