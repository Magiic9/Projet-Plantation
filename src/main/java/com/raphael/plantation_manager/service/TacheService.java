package com.raphael.plantation_manager.service;

import com.raphael.plantation_manager.entity.Tache;
import com.raphael.plantation_manager.repository.TacheRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TacheService {

    @Autowired
    private TacheRepository tacheRepository;

    public List<Tache> findAll() {
        return tacheRepository.findAll();
    }

    public List<Tache> findActives() {
        return tacheRepository.findByActifTrue();
    }

    public Tache findById(Long id) {
        return tacheRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tâche introuvable : " + id));
    }

    public Tache save(Tache tache) {
        return tacheRepository.save(tache);
    }

    public Tache update(Long id, Tache updated) {
        Tache existing = findById(id);
        existing.setNom(updated.getNom());
        existing.setDescription(updated.getDescription());
        existing.setActif(updated.getActif());
        return tacheRepository.save(existing);
    }

    public void desactiver(Long id) {
        Tache tache = findById(id);
        tache.setActif(false);
        tacheRepository.save(tache);
    }

    public void delete(Long id) {
        tacheRepository.deleteById(id);
    }
}
