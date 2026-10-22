package com.raphael.plantation_manager.dto;

import java.time.LocalTime;

/**
 * Situation d'un employé pour une journée.
 * statut : PRESENT (sur place), PARTI (a pointé son départ), ABSENT (aucun pointage),
 * INCOMPLET (jour passé sans départ pointé).
 */
public record PresenceEmploye(
        Long employeId,
        String nom,
        String prenom,
        String photo,
        String statut,
        LocalTime arrivee,
        LocalTime depart,
        boolean enRetard,
        double heuresPresence,
        String tacheEnCours,
        String parcelleEnCours) {}
