package com.raphael.plantation_manager.service;

import com.raphael.plantation_manager.dto.PointageRequest;
import com.raphael.plantation_manager.dto.PresenceEmploye;
import com.raphael.plantation_manager.entity.Employee;
import com.raphael.plantation_manager.entity.Pointage;
import com.raphael.plantation_manager.entity.Travail;
import com.raphael.plantation_manager.repository.PointageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PointageService {

    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    private PointageRepository pointageRepository;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private TravailService travailService;

    // Heure à laquelle la journée commence, pour repérer les retards
    @Value("${plantation.heure-debut-journee:07:00}")
    private LocalTime heureDebutJournee;

    @Value("${plantation.tolerance-retard-minutes:10}")
    private int toleranceRetardMinutes;

    public List<Pointage> findByDate(LocalDate date) {
        return pointageRepository.findByDateOrderByHeureArriveeAsc(date);
    }

    public List<Pointage> findByPeriode(LocalDate debut, LocalDate fin, Long employeId) {
        if (employeId != null) {
            return pointageRepository.findByEmployeIdAndDateBetweenOrderByDateAscHeureArriveeAsc(employeId, debut, fin);
        }
        return pointageRepository.findByDateBetweenOrderByDateAscHeureArriveeAsc(debut, fin);
    }

    public List<Pointage> findIncomplets() {
        return pointageRepository.findByDateBeforeAndHeureDepartIsNullOrderByDateAsc(LocalDate.now());
    }

    public Optional<Pointage> findOuvertAujourdhui(Long employeId) {
        return pointageRepository.findFirstByEmployeIdAndDateAndHeureDepartIsNull(employeId, LocalDate.now());
    }

    // ── Borne ─────────────────────────────────────────────────────────────────

    @Transactional
    public Pointage arrivee(Employee employe) {
        findOuvertAujourdhui(employe.getId()).ifPresent(p -> {
            throw new RuntimeException(employe.getPrenom() + " est déjà pointé(e) présent(e) depuis "
                    + p.getHeureArrivee().format(HEURE) + ".");
        });
        Pointage pointage = new Pointage();
        pointage.setEmploye(employe);
        pointage.setDate(LocalDate.now());
        pointage.setHeureArrivee(maintenant());
        return pointageRepository.save(pointage);
    }

    /** Pointe le départ et termine la tâche en cours s'il y en a une. */
    @Transactional
    public Pointage depart(Employee employe) {
        Pointage pointage = findOuvertAujourdhui(employe.getId())
                .orElseThrow(() -> new RuntimeException(employe.getPrenom() + " n'a pas pointé son arrivée aujourd'hui."));
        travailService.terminerTravauxEnCours(employe.getId());
        pointage.setHeureDepart(maintenant());
        return pointageRepository.save(pointage);
    }

    // ── Corrections par le patron ─────────────────────────────────────────────

    @Transactional
    public Pointage creer(PointageRequest request) {
        Pointage pointage = new Pointage();
        remplir(pointage, request);
        return pointageRepository.save(pointage);
    }

    @Transactional
    public Pointage modifier(Long id, PointageRequest request) {
        Pointage pointage = pointageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pointage introuvable : " + id));
        remplir(pointage, request);
        return pointageRepository.save(pointage);
    }

    public void delete(Long id) {
        pointageRepository.deleteById(id);
    }

    private void remplir(Pointage pointage, PointageRequest request) {
        if (request.employeId() == null || request.date() == null || request.heureArrivee() == null) {
            throw new RuntimeException("Employé, date et heure d'arrivée sont obligatoires.");
        }
        if (request.heureDepart() != null && !request.heureDepart().isAfter(request.heureArrivee())) {
            throw new RuntimeException("L'heure de départ doit être après l'heure d'arrivée.");
        }
        pointage.setEmploye(employeeService.findById(request.employeId()));
        pointage.setDate(request.date());
        pointage.setHeureArrivee(request.heureArrivee());
        pointage.setHeureDepart(request.heureDepart());
    }

    // ── Présence ──────────────────────────────────────────────────────────────

    /** Qui est là, qui est parti, qui est absent pour une journée. */
    @Transactional(readOnly = true)
    public List<PresenceEmploye> presence(LocalDate date) {
        boolean aujourdhui = date.equals(LocalDate.now());

        Map<Long, List<Pointage>> parEmploye = new LinkedHashMap<>();
        for (Pointage p : findByDate(date)) {
            parEmploye.computeIfAbsent(p.getEmploye().getId(), k -> new ArrayList<>()).add(p);
        }

        // Employés actifs + ceux qui ont pointé ce jour-là (même s'ils ont été désactivés depuis)
        Map<Long, Employee> employes = new LinkedHashMap<>();
        employeeService.findActifs().forEach(e -> employes.put(e.getId(), e));
        parEmploye.values().forEach(ps -> employes.putIfAbsent(ps.get(0).getEmploye().getId(), ps.get(0).getEmploye()));

        List<PresenceEmploye> resultat = new ArrayList<>();
        for (Employee e : employes.values()) {
            List<Pointage> pointages = parEmploye.getOrDefault(e.getId(), List.of());
            resultat.add(presenceEmploye(e, pointages, aujourdhui));
        }
        // Présents en premier, absents à la fin
        List<String> ordre = List.of("PRESENT", "INCOMPLET", "PARTI", "ABSENT");
        resultat.sort(Comparator.comparing((PresenceEmploye p) -> ordre.indexOf(p.statut()))
                .thenComparing(PresenceEmploye::nom));
        return resultat;
    }

    private PresenceEmploye presenceEmploye(Employee e, List<Pointage> pointages, boolean aujourdhui) {
        if (pointages.isEmpty()) {
            return new PresenceEmploye(e.getId(), e.getNom(), e.getPrenom(), e.getPhoto(),
                    "ABSENT", null, null, false, 0, null, null);
        }

        LocalTime arrivee = pointages.get(0).getHeureArrivee();
        boolean ouvert = pointages.stream().anyMatch(p -> p.getHeureDepart() == null);
        LocalTime depart = ouvert ? null : pointages.get(pointages.size() - 1).getHeureDepart();

        double heures = 0;
        for (Pointage p : pointages) {
            LocalTime fin = p.getHeureDepart() != null ? p.getHeureDepart() : (aujourdhui ? LocalTime.now() : null);
            heures += Heures.entre(p.getHeureArrivee(), fin);
        }

        String statut = ouvert ? (aujourdhui ? "PRESENT" : "INCOMPLET") : "PARTI";
        boolean enRetard = heureDebutJournee != null
                && arrivee.isAfter(heureDebutJournee.plusMinutes(toleranceRetardMinutes));

        String tache = null;
        String parcelle = null;
        if (aujourdhui && ouvert) {
            Optional<Travail> enCours = travailService.findEnCoursParEmploye(e.getId());
            if (enCours.isPresent()) {
                tache = enCours.get().getTache().getNom();
                parcelle = enCours.get().getParcelle().getNom();
            }
        }

        return new PresenceEmploye(e.getId(), e.getNom(), e.getPrenom(), e.getPhoto(),
                statut, arrivee, depart, enRetard, Heures.arrondi(heures), tache, parcelle);
    }

    private static LocalTime maintenant() {
        return LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
