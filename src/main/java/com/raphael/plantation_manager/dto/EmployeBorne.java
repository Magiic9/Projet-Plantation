package com.raphael.plantation_manager.dto;

/** Employé tel que montré sur l'écran d'accueil de la borne (sans infos sensibles). */
public record EmployeBorne(Long id, String nom, String prenom, String photo, boolean present, boolean pinDefini) {}
