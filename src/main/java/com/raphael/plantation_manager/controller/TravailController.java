package com.raphael.plantation_manager.controller;

import com.raphael.plantation_manager.entity.Travail;
import com.raphael.plantation_manager.service.TravailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/travaux")
@CrossOrigin(origins = "*")
public class TravailController {

    @Autowired
    private TravailService travailService;

    @GetMapping
    public List<Travail> getAll() {
        return travailService.findAll();
    }

    @GetMapping("/en-cours")
    public List<Travail> getEnCours() {
        return travailService.findEnCours();
    }

    @GetMapping("/date")
    public List<Travail> getByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return travailService.findByDate(date);
    }

    @GetMapping("/employe/{id}")
    public List<Travail> getByEmploye(@PathVariable Long id) {
        return travailService.findByEmploye(id);
    }

    @GetMapping("/employe/{id}/aujourd-hui")
    public List<Travail> getAujourdhuiParEmploye(@PathVariable Long id) {
        return travailService.findAujourdhuiParEmploye(id);
    }

    @GetMapping("/employe/{id}/periode")
    public List<Travail> getByEmployeAndPeriode(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return travailService.findByEmployeAndPeriode(id, debut, fin);
    }

    // ── Endpoints borne ───────────────────────────────────────────────────────

    @PostMapping("/demarrer")
    public ResponseEntity<Travail> demarrer(@RequestBody DemarrerTravailRequest request) {
        Travail travail = travailService.demarrerTravail(
                request.employeId(), request.tacheId(), request.parcelleId());
        return ResponseEntity.ok(travail);
    }

    @PatchMapping("/{id}/terminer")
    public ResponseEntity<Travail> terminer(@PathVariable Long id) {
        return ResponseEntity.ok(travailService.terminerTravail(id));
    }

    // ── Fiches de paie ────────────────────────────────────────────────────────

    @GetMapping("/heures")
    public Map<Long, Map<String, Object>> totalHeures(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return travailService.totalHeuresParEmploye(debut, fin);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        travailService.delete(id);
        return ResponseEntity.noContent().build();
    }

    record DemarrerTravailRequest(Long employeId, Long tacheId, Long parcelleId) {}
}
