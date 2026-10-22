package com.raphael.plantation_manager.service;

import com.raphael.plantation_manager.dto.BorneRequest;
import com.raphael.plantation_manager.dto.PresenceEmploye;
import com.raphael.plantation_manager.dto.StatutBorne;
import com.raphael.plantation_manager.entity.Employee;
import com.raphael.plantation_manager.entity.Parcelle;
import com.raphael.plantation_manager.entity.Tache;
import com.raphael.plantation_manager.entity.Travail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class BorneServiceTest {

    @Autowired private BorneService borneService;
    @Autowired private EmployeeService employeeService;
    @Autowired private TacheService tacheService;
    @Autowired private ParcelleService parcelleService;
    @Autowired private TravailService travailService;
    @Autowired private PointageService pointageService;

    private Employee employe;
    private Tache recolte;
    private Tache desherbage;
    private Parcelle parcelle;

    @BeforeEach
    void preparer() {
        employe = new Employee();
        employe.setNom("Rosier");
        employe.setPrenom("Jean");
        employe.setCodePin("4321");
        employe = employeeService.save(employe);

        recolte = new Tache();
        recolte.setNom("Récolte");
        recolte = tacheService.save(recolte);
        desherbage = new Tache();
        desherbage.setNom("Désherbage");
        desherbage = tacheService.save(desherbage);

        parcelle = new Parcelle();
        parcelle.setNom("Nord");
        parcelle = parcelleService.save(parcelle);
    }

    private BorneRequest avecPin(String pin) {
        return new BorneRequest(employe.getId(), pin, null, null);
    }

    @Test
    void lePinEstStockeChiffre() {
        assertNotEquals("4321", employe.getCodePin());
        assertTrue(employeeService.verifierPin(employe, "4321"));
    }

    @Test
    void mauvaisPinRefuse() {
        RuntimeException e = assertThrows(RuntimeException.class, () -> borneService.arrivee(avecPin("0000")));
        assertTrue(e.getMessage().startsWith("Code PIN incorrect"));
        assertTrue(pointageService.findOuvertAujourdhui(employe.getId()).isEmpty());
    }

    @Test
    void bloqueApresCinqErreurs() {
        for (int i = 0; i < 5; i++) {
            assertThrows(RuntimeException.class, () -> borneService.identifier(avecPin("0000")));
        }
        RuntimeException e = assertThrows(RuntimeException.class, () -> borneService.identifier(avecPin("4321")));
        assertTrue(e.getMessage().startsWith("Trop d'erreurs"));
    }

    @Test
    void journeeComplete() {
        StatutBorne s = borneService.arrivee(avecPin("4321"));
        assertTrue(s.present());

        // Double arrivée refusée
        assertThrows(RuntimeException.class, () -> borneService.arrivee(avecPin("4321")));

        s = borneService.demarrerTravail(new BorneRequest(employe.getId(), "4321", recolte.getId(), parcelle.getId()));
        assertEquals("Récolte", s.tacheEnCours());

        // Changer de tâche termine la précédente
        s = borneService.demarrerTravail(new BorneRequest(employe.getId(), "4321", desherbage.getId(), parcelle.getId()));
        assertEquals("Désherbage", s.tacheEnCours());
        List<Travail> travaux = travailService.findAujourdhuiParEmploye(employe.getId());
        assertEquals(2, travaux.size());
        assertEquals(1, travaux.stream().filter(t -> t.getHeureFin() == null).count());

        PresenceEmploye presence = pointageService.presence(LocalDate.now()).stream()
                .filter(p -> p.employeId().equals(employe.getId())).findFirst().orElseThrow();
        assertEquals("PRESENT", presence.statut());
        assertEquals("Désherbage", presence.tacheEnCours());

        // Le départ termine la tâche en cours
        s = borneService.depart(avecPin("4321"));
        assertFalse(s.present());
        assertNull(s.tacheEnCours());
        assertTrue(travailService.findAujourdhuiParEmploye(employe.getId()).stream().allMatch(t -> t.getHeureFin() != null));
    }

    @Test
    void tacheImpossibleSansArrivee() {
        RuntimeException e = assertThrows(RuntimeException.class, () -> borneService.demarrerTravail(
                new BorneRequest(employe.getId(), "4321", recolte.getId(), parcelle.getId())));
        assertEquals("Pointez d'abord votre arrivée.", e.getMessage());
    }
}
