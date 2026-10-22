package com.raphael.plantation_manager.controller;

import com.raphael.plantation_manager.dto.PointageRequest;
import com.raphael.plantation_manager.dto.PresenceEmploye;
import com.raphael.plantation_manager.entity.Pointage;
import com.raphael.plantation_manager.service.PointageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class PointageController {

    @Autowired
    private PointageService pointageService;

    /** Présence du jour (ou d'une autre date) : présents, partis, absents, retards. */
    @GetMapping("/presence")
    public List<PresenceEmploye> presence(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return pointageService.presence(date != null ? date : LocalDate.now());
    }

    /** Pointages d'une date, ou d'une période (optionnellement pour un seul employé). */
    @GetMapping("/pointages")
    public List<Pointage> getPointages(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) Long employeId) {
        if (debut != null && fin != null) {
            return pointageService.findByPeriode(debut, fin, employeId);
        }
        return pointageService.findByDate(date != null ? date : LocalDate.now());
    }

    /** Jours passés où un employé a oublié de pointer son départ. */
    @GetMapping("/pointages/incomplets")
    public List<Pointage> getIncomplets() {
        return pointageService.findIncomplets();
    }

    @PostMapping("/pointages")
    public ResponseEntity<Pointage> create(@RequestBody PointageRequest request) {
        return ResponseEntity.ok(pointageService.creer(request));
    }

    @PutMapping("/pointages/{id}")
    public ResponseEntity<Pointage> update(@PathVariable Long id, @RequestBody PointageRequest request) {
        return ResponseEntity.ok(pointageService.modifier(id, request));
    }

    @DeleteMapping("/pointages/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        pointageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
