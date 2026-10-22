package com.raphael.plantation_manager.dto;

import java.time.LocalTime;

/** Ce que la borne affiche à un employé après son code PIN. */
public record StatutBorne(
        Long employeId,
        String nom,
        String prenom,
        boolean present,
        LocalTime arrivee,
        Long travailId,
        String tacheEnCours,
        String parcelleEnCours,
        LocalTime debutTache) {}
