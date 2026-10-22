package com.raphael.plantation_manager.service;

import com.raphael.plantation_manager.dto.BorneRequest;
import com.raphael.plantation_manager.dto.EmployeBorne;
import com.raphael.plantation_manager.dto.StatutBorne;
import com.raphael.plantation_manager.entity.Employee;
import com.raphael.plantation_manager.entity.Pointage;
import com.raphael.plantation_manager.entity.Travail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Actions des employés sur la borne de pointage.
 * Chaque action demande le code PIN de l'employé ; après 5 erreurs, l'employé est bloqué 5 minutes.
 */
@Service
public class BorneService {

    private static final int ESSAIS_MAX = 5;
    private static final Duration DUREE_BLOCAGE = Duration.ofMinutes(5);

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private PointageService pointageService;

    @Autowired
    private TravailService travailService;

    private record Essais(int nombre, Instant bloqueJusqua) {}

    private final Map<Long, Essais> essais = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public List<EmployeBorne> employes() {
        return employeeService.findActifs().stream()
                .sorted(Comparator.comparing(Employee::getPrenom).thenComparing(Employee::getNom))
                .map(e -> new EmployeBorne(e.getId(), e.getNom(), e.getPrenom(), e.getPhoto(),
                        pointageService.findOuvertAujourdhui(e.getId()).isPresent(), e.isPinDefini()))
                .toList();
    }

    @Transactional(readOnly = true)
    public StatutBorne identifier(BorneRequest request) {
        return statut(verifier(request));
    }

    @Transactional
    public StatutBorne arrivee(BorneRequest request) {
        Employee employe = verifier(request);
        pointageService.arrivee(employe);
        return statut(employe);
    }

    @Transactional
    public StatutBorne depart(BorneRequest request) {
        Employee employe = verifier(request);
        pointageService.depart(employe);
        return statut(employe);
    }

    /** Démarre une tâche ; si une autre tâche est en cours, elle est terminée d'abord. */
    @Transactional
    public StatutBorne demarrerTravail(BorneRequest request) {
        Employee employe = verifier(request);
        if (request.tacheId() == null || request.parcelleId() == null) {
            throw new RuntimeException("Choisissez une tâche et une parcelle.");
        }
        if (pointageService.findOuvertAujourdhui(employe.getId()).isEmpty()) {
            throw new RuntimeException("Pointez d'abord votre arrivée.");
        }
        travailService.terminerTravauxEnCours(employe.getId());
        travailService.demarrerTravail(employe.getId(), request.tacheId(), request.parcelleId());
        return statut(employe);
    }

    @Transactional
    public StatutBorne terminerTravail(BorneRequest request) {
        Employee employe = verifier(request);
        if (travailService.terminerTravauxEnCours(employe.getId()) == 0) {
            throw new RuntimeException("Aucune tâche en cours.");
        }
        return statut(employe);
    }

    private Employee verifier(BorneRequest request) {
        if (request.employeId() == null) {
            throw new RuntimeException("Choisissez votre nom.");
        }
        Employee employe = employeeService.findById(request.employeId());
        if (!Boolean.TRUE.equals(employe.getActif())) {
            throw new RuntimeException("Cet employé n'est plus actif.");
        }
        if (!employe.isPinDefini()) {
            throw new RuntimeException("Aucun code PIN n'est défini pour vous. Voyez le patron.");
        }

        Essais precedents = essais.get(employe.getId());
        if (precedents != null && precedents.bloqueJusqua() != null && Instant.now().isBefore(precedents.bloqueJusqua())) {
            long minutes = Duration.between(Instant.now(), precedents.bloqueJusqua()).toMinutes() + 1;
            throw new RuntimeException("Trop d'erreurs de code. Réessayez dans " + minutes + " min.");
        }

        if (!employeeService.verifierPin(employe, request.pin())) {
            Essais nouveau = essais.compute(employe.getId(), (id, e) -> {
                int nombre = (e == null || e.bloqueJusqua() != null) ? 1 : e.nombre() + 1;
                return new Essais(nombre, nombre >= ESSAIS_MAX ? Instant.now().plus(DUREE_BLOCAGE) : null);
            });
            if (nouveau.bloqueJusqua() != null) {
                throw new RuntimeException("Trop d'erreurs de code. Réessayez dans " + DUREE_BLOCAGE.toMinutes() + " min.");
            }
            throw new RuntimeException("Code PIN incorrect (" + (ESSAIS_MAX - nouveau.nombre()) + " essai(s) restant(s)).");
        }

        essais.remove(employe.getId());
        return employe;
    }

    private StatutBorne statut(Employee employe) {
        Optional<Pointage> pointage = pointageService.findOuvertAujourdhui(employe.getId());
        Optional<Travail> travail = travailService.findEnCoursParEmploye(employe.getId());
        return new StatutBorne(
                employe.getId(), employe.getNom(), employe.getPrenom(),
                pointage.isPresent(),
                pointage.map(Pointage::getHeureArrivee).orElse(null),
                travail.map(Travail::getId).orElse(null),
                travail.map(t -> t.getTache().getNom()).orElse(null),
                travail.map(t -> t.getParcelle().getNom()).orElse(null),
                travail.map(Travail::getHeureDebut).orElse(null));
    }
}
