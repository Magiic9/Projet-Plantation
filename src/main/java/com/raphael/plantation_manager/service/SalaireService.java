package com.raphael.plantation_manager.service;

import com.raphael.plantation_manager.dto.FicheSalaire;
import com.raphael.plantation_manager.entity.Employee;
import com.raphael.plantation_manager.entity.Pointage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Calcul des salaires à partir des pointages de présence (arrivée → départ).
 * Les heures au-delà du seuil hebdomadaire sont payées avec une majoration.
 */
@Service
public class SalaireService {

    @Autowired
    private PointageService pointageService;

    @Autowired
    private EmployeeService employeeService;

    @Value("${plantation.heures-normales-par-semaine:35}")
    private double heuresNormalesParSemaine;

    // 0.25 = heures sup payées 25 % de plus
    @Value("${plantation.majoration-heures-sup:0.25}")
    private double majorationHeuresSup;

    @Transactional(readOnly = true)
    public List<FicheSalaire> calculer(LocalDate debut, LocalDate fin) {
        if (fin.isBefore(debut)) {
            throw new RuntimeException("La date de fin doit être après la date de début.");
        }

        Map<Long, Employee> employes = new LinkedHashMap<>();
        Map<Long, List<Pointage>> pointagesParEmploye = new LinkedHashMap<>();
        employeeService.findActifs().forEach(e -> employes.put(e.getId(), e));
        for (Pointage p : pointageService.findByPeriode(debut, fin, null)) {
            employes.putIfAbsent(p.getEmploye().getId(), p.getEmploye());
            pointagesParEmploye.computeIfAbsent(p.getEmploye().getId(), k -> new ArrayList<>()).add(p);
        }

        List<FicheSalaire> fiches = new ArrayList<>();
        for (Employee e : employes.values()) {
            fiches.add(calculerFiche(e, pointagesParEmploye.getOrDefault(e.getId(), List.of()),
                    heuresNormalesParSemaine, majorationHeuresSup));
        }
        fiches.sort(Comparator.comparing(FicheSalaire::nom).thenComparing(FicheSalaire::prenom));
        return fiches;
    }

    /** Calcul pur, sans base de données : facile à tester. */
    static FicheSalaire calculerFiche(Employee employe, List<Pointage> pointages,
                                      double seuilHebdo, double majoration) {
        Map<LocalDate, Double> heuresParSemaine = new TreeMap<>();
        Set<LocalDate> jours = new HashSet<>();
        int incomplets = 0;

        for (Pointage p : pointages) {
            if (p.getHeureDepart() == null) {
                // Départ non pointé : pas d'heures comptées tant que le patron n'a pas corrigé
                if (p.getDate().isBefore(LocalDate.now())) {
                    incomplets++;
                }
                continue;
            }
            LocalDate lundi = p.getDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            heuresParSemaine.merge(lundi, Heures.entre(p.getHeureArrivee(), p.getHeureDepart()), Double::sum);
            jours.add(p.getDate());
        }

        double normales = 0;
        double sup = 0;
        for (double heures : heuresParSemaine.values()) {
            normales += Math.min(heures, seuilHebdo);
            sup += Math.max(heures - seuilHebdo, 0);
        }

        double taux = employe.getTauxHoraire() != null ? employe.getTauxHoraire() : 0;
        double montantNormal = Heures.arrondi(normales * taux);
        double montantSup = Heures.arrondi(sup * taux * (1 + majoration));

        return new FicheSalaire(
                employe.getId(), employe.getNom(), employe.getPrenom(), employe.getTauxHoraire(),
                jours.size(),
                Heures.arrondi(normales + sup), Heures.arrondi(normales), Heures.arrondi(sup),
                montantNormal, montantSup, Heures.arrondi(montantNormal + montantSup),
                incomplets);
    }

    /** Export CSV (séparateur ; et virgule décimale, lisible directement dans Excel). */
    public String exporterCsv(LocalDate debut, LocalDate fin) {
        StringBuilder csv = new StringBuilder("﻿");
        csv.append("Nom;Prénom;Taux horaire;Jours travaillés;Heures totales;Heures normales;Heures sup;")
           .append("Montant normal;Montant heures sup;Total à payer;Pointages à corriger\n");
        for (FicheSalaire f : calculer(debut, fin)) {
            csv.append(champ(f.nom())).append(';')
               .append(champ(f.prenom())).append(';')
               .append(nombre(f.tauxHoraire())).append(';')
               .append(f.joursTravailles()).append(';')
               .append(nombre(f.heuresTotal())).append(';')
               .append(nombre(f.heuresNormales())).append(';')
               .append(nombre(f.heuresSup())).append(';')
               .append(nombre(f.montantNormal())).append(';')
               .append(nombre(f.montantSup())).append(';')
               .append(nombre(f.montantTotal())).append(';')
               .append(f.pointagesIncomplets()).append('\n');
        }
        return csv.toString();
    }

    private static String champ(String valeur) {
        if (valeur == null) {
            return "";
        }
        return "\"" + valeur.replace("\"", "\"\"") + "\"";
    }

    private static String nombre(Double valeur) {
        return valeur == null ? "" : String.format(Locale.ROOT, "%.2f", valeur).replace('.', ',');
    }
}
