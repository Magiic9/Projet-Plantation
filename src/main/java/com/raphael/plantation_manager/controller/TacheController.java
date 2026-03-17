package com.raphael.plantation_manager.controller;

import com.raphael.plantation_manager.entity.Tache;
import com.raphael.plantation_manager.service.TacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/taches")
@CrossOrigin(origins = "*")
public class TacheController {

    @Autowired
    private TacheService tacheService;

    @GetMapping
    public List<Tache> getAll() {
        return tacheService.findAll();
    }

    @GetMapping("/actives")
    public List<Tache> getActives() {
        return tacheService.findActives();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tache> getById(@PathVariable Long id) {
        return ResponseEntity.ok(tacheService.findById(id));
    }

    @PostMapping
    public ResponseEntity<Tache> create(@RequestBody Tache tache) {
        return ResponseEntity.ok(tacheService.save(tache));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Tache> update(@PathVariable Long id, @RequestBody Tache tache) {
        return ResponseEntity.ok(tacheService.update(id, tache));
    }

    @PatchMapping("/{id}/desactiver")
    public ResponseEntity<Void> desactiver(@PathVariable Long id) {
        tacheService.desactiver(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tacheService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
