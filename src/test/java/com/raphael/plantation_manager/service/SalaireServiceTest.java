package com.raphael.plantation_manager.service;

import com.raphael.plantation_manager.dto.FicheSalaire;
import com.raphael.plantation_manager.entity.Employee;
import com.raphael.plantation_manager.entity.Pointage;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SalaireServiceTest {

    // Lundi 5 octobre 2026
    private static final LocalDate LUNDI = LocalDate.of(2026, 10, 5);

    private static Employee employe(Double taux) {
        Employee e = new Employee();
        e.setNom("Joseph");
        e.setPrenom("Marie");
        e.setTauxHoraire(taux);
        return e;
    }

    private static Pointage pointage(LocalDate date, String arrivee, String depart) {
        Pointage p = new Pointage();
        p.setDate(date);
        p.setHeureArrivee(LocalTime.parse(arrivee));
        p.setHeureDepart(depart == null ? null : LocalTime.parse(depart));
        return p;
    }

    @Test
    void semaineSansHeuresSup() {
        List<Pointage> pointages = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            pointages.add(pointage(LUNDI.plusDays(i), "07:00", "15:00")); // 8 h x 4 = 32 h
        }
        FicheSalaire f = SalaireService.calculerFiche(employe(10.0), pointages, 35, 0.25);

        assertEquals(4, f.joursTravailles());
        assertEquals(32, f.heuresTotal());
        assertEquals(0, f.heuresSup());
        assertEquals(320, f.montantTotal());
    }

    @Test
    void heuresSupMajoreesParSemaine() {
        List<Pointage> pointages = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            pointages.add(pointage(LUNDI.plusDays(i), "07:00", "15:00")); // 40 h la 1re semaine
        }
        pointages.add(pointage(LUNDI.plusDays(7), "07:00", "17:00"));    // 10 h la 2e semaine

        FicheSalaire f = SalaireService.calculerFiche(employe(10.0), pointages, 35, 0.25);

        assertEquals(50, f.heuresTotal());
        assertEquals(45, f.heuresNormales());
        assertEquals(5, f.heuresSup());
        assertEquals(450, f.montantNormal());
        assertEquals(62.5, f.montantSup());   // 5 h x 10 x 1,25
        assertEquals(512.5, f.montantTotal());
    }

    @Test
    void plusieursPointagesLeMemeJour() {
        List<Pointage> pointages = List.of(
                pointage(LUNDI, "07:00", "11:30"),
                pointage(LUNDI, "13:00", "16:15"));

        FicheSalaire f = SalaireService.calculerFiche(employe(12.0), pointages, 35, 0.25);

        assertEquals(1, f.joursTravailles());
        assertEquals(7.75, f.heuresTotal());
        assertEquals(93, f.montantTotal());
    }

    @Test
    void departOublieNeComptePasEtEstSignale() {
        List<Pointage> pointages = List.of(
                pointage(LUNDI, "07:00", "15:00"),
                pointage(LUNDI.plusDays(1), "07:00", null));

        FicheSalaire f = SalaireService.calculerFiche(employe(10.0), pointages, 35, 0.25);

        assertEquals(8, f.heuresTotal());
        assertEquals(1, f.pointagesIncomplets());
    }

    @Test
    void sansTauxHoraireLeMontantEstZero() {
        FicheSalaire f = SalaireService.calculerFiche(employe(null),
                List.of(pointage(LUNDI, "07:00", "15:00")), 35, 0.25);

        assertEquals(8, f.heuresTotal());
        assertEquals(0, f.montantTotal());
    }
}
