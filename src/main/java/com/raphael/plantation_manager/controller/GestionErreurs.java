package com.raphael.plantation_manager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

/** Renvoie les erreurs métier en JSON lisible ({"erreur": "..."}) au lieu d'une erreur 500. */
@RestControllerAdvice
public class GestionErreurs {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> erreur(RuntimeException e) {
        String message = e.getMessage() != null ? e.getMessage() : "Erreur inattendue.";
        return ResponseEntity.badRequest().body(Map.of("erreur", message));
    }
}
