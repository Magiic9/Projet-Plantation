package com.raphael.plantation_manager.dto;

/** Récapitulatif de paie d'un employé sur une période. */
public record FicheSalaire(
        Long employeId,
        String nom,
        String prenom,
        Double tauxHoraire,
        int joursTravailles,
        double heuresTotal,
        double heuresNormales,
        double heuresSup,
        double montantNormal,
        double montantSup,
        double montantTotal,
        int pointagesIncomplets) {}
