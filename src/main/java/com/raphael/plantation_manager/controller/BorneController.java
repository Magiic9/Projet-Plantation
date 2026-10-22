package com.raphael.plantation_manager.controller;

import com.raphael.plantation_manager.dto.BorneRequest;
import com.raphael.plantation_manager.dto.EmployeBorne;
import com.raphael.plantation_manager.dto.StatutBorne;
import com.raphael.plantation_manager.entity.Parcelle;
import com.raphael.plantation_manager.entity.Tache;
import com.raphael.plantation_manager.service.BorneService;
import com.raphael.plantation_manager.service.ParcelleService;
import com.raphael.plantation_manager.service.TacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** Endpoints utilisés par la borne de pointage (compte "borne" ou patron). */
@RestController
@RequestMapping("/api/borne")
public class BorneController {

    @Autowired
    private BorneService borneService;

    @Autowired
    private TacheService tacheService;

    @Autowired
    private ParcelleService parcelleService;

    @GetMapping("/employes")
    public List<EmployeBorne> employes() {
        return borneService.employes();
    }

    @GetMapping("/taches")
    public List<Tache> taches() {
        return tacheService.findActives();
    }

    @GetMapping("/parcelles")
    public List<Parcelle> parcelles() {
        return parcelleService.findActives();
    }

    @PostMapping("/identifier")
    public StatutBorne identifier(@RequestBody BorneRequest request) {
        return borneService.identifier(request);
    }

    @PostMapping("/arrivee")
    public StatutBorne arrivee(@RequestBody BorneRequest request) {
        return borneService.arrivee(request);
    }

    @PostMapping("/depart")
    public StatutBorne depart(@RequestBody BorneRequest request) {
        return borneService.depart(request);
    }

    @PostMapping("/travail/demarrer")
    public StatutBorne demarrerTravail(@RequestBody BorneRequest request) {
        return borneService.demarrerTravail(request);
    }

    @PostMapping("/travail/terminer")
    public StatutBorne terminerTravail(@RequestBody BorneRequest request) {
        return borneService.terminerTravail(request);
    }
}
