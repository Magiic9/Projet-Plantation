package com.raphael.plantation_manager.controller;

import com.raphael.plantation_manager.entity.Parcelle;
import com.raphael.plantation_manager.service.ParcelleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/parcelles")
@CrossOrigin(origins = "*")
public class ParcelleController {

    @Autowired
    private ParcelleService parcelleService;

    @GetMapping
    public List<Parcelle> getAll() {
        return parcelleService.findAll();
    }

    @GetMapping("/actives")
    public List<Parcelle> getActives() {
        return parcelleService.findActives();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Parcelle> getById(@PathVariable Long id) {
        return ResponseEntity.ok(parcelleService.findById(id));
    }

    @PostMapping
    public ResponseEntity<Parcelle> create(@RequestBody Parcelle parcelle) {
        return ResponseEntity.ok(parcelleService.save(parcelle));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Parcelle> update(@PathVariable Long id, @RequestBody Parcelle parcelle) {
        return ResponseEntity.ok(parcelleService.update(id, parcelle));
    }

    @PatchMapping("/{id}/desactiver")
    public ResponseEntity<Void> desactiver(@PathVariable Long id) {
        parcelleService.desactiver(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        parcelleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
