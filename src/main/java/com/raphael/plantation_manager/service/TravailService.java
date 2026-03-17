package com.raphael.plantation_manager.service;

import com.raphael.plantation_manager.entity.Employee;
import com.raphael.plantation_manager.entity.Parcelle;
import com.raphael.plantation_manager.entity.Tache;
import com.raphael.plantation_manager.entity.Travail;
import com.raphael.plantation_manager.repository.TravailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TravailService {

    @Autowired
    private TravailRepository travailRepository;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private TacheService tacheService;

    @Autowired
    private ParcelleService parcelleService;

    public List<Travail> findAll() {
        return travailRepository.findAll();
    }

    public Travail findById(Long id) {
        return travailRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Travail introuvable : " + id));
    }

    public List<Travail> findByEmploye(Long employeId) {
        return travailRepository.findByEmployeId(employeId);
    }

    public List<Travail> findByEmployeAndPeriode(Long employeId, LocalDate debut, LocalDate fin) {
        return travailRepository.findByEmployeIdAndDateBetween(employeId, debut, fin);
    }

    public List<Travail> findByDate(LocalDate date) {
        return travailRepository.findByDate(date);
    }

    public List<Travail> findAujourdhuiParEmploye(Long employeId) {
        return travailRepository.findByEmployeIdAndDate(employeId, LocalDate.now());
    }

    public Travail demarrerTravail(Long employeId, Long tacheId, Long parcelleId) {
        Employee employe = employeeService.findById(employeId);
        Tache tache = tacheService.findById(tacheId);
        Parcelle parcelle = parcelleService.findById(parcelleId);

        Travail travail = new Travail();
        travail.setEmploye(employe);
        travail.setTache(tache);
        travail.setParcelle(parcelle);
        travail.setDate(LocalDate.now());
        travail.setHeureDebut(LocalTime.now());

        return travailRepository.save(travail);
    }

    public Travail terminerTravail(Long travailId) {
        Travail travail = findById(travailId);
        if (travail.getHeureFin() != null) {
            throw new RuntimeException("Ce travail est déjà terminé.");
        }
        travail.setHeureFin(LocalTime.now());
        return travailRepository.save(travail);
    }

    public List<Travail> findEnCours() {
        return travailRepository.findAll().stream()
                .filter(t -> t.getDate().equals(LocalDate.now()) && t.getHeureFin() == null)
                .toList();
    }

    /**
     * Calcule le total d'heures par employé sur une période.
     * Retourne une Map : employeId -> { nom, prenom, totalHeures }
     */
    public Map<Long, Map<String, Object>> totalHeuresParEmploye(LocalDate debut, LocalDate fin) {
        List<Travail> travaux = travailRepository.findTravailTerminesSurPeriode(debut, fin);

        Map<Long, Map<String, Object>> result = new LinkedHashMap<>();

        for (Travail t : travaux) {
            Long employeId = t.getEmploye().getId();
            double heures = Duration.between(t.getHeureDebut(), t.getHeureFin()).toMinutes() / 60.0;

            result.computeIfAbsent(employeId, k -> {
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("nom", t.getEmploye().getNom());
                info.put("prenom", t.getEmploye().getPrenom());
                info.put("totalHeures", 0.0);
                return info;
            });

            double current = (double) result.get(employeId).get("totalHeures");
            result.get(employeId).put("totalHeures", Math.round((current + heures) * 100.0) / 100.0);
        }

        return result;
    }

    public void delete(Long id) {
        travailRepository.deleteById(id);
    }
}
