package com.raphael.plantation_manager.service;

import com.raphael.plantation_manager.entity.Parcelle;
import com.raphael.plantation_manager.repository.ParcelleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ParcelleService {

    @Autowired
    private ParcelleRepository parcelleRepository;

    public List<Parcelle> findAll() {
        return parcelleRepository.findAll();
    }

    public List<Parcelle> findActives() {
        return parcelleRepository.findByActifTrue();
    }

    public Parcelle findById(Long id) {
        return parcelleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Parcelle introuvable : " + id));
    }

    public Parcelle save(Parcelle parcelle) {
        return parcelleRepository.save(parcelle);
    }

    public Parcelle update(Long id, Parcelle updated) {
        Parcelle existing = findById(id);
        existing.setNom(updated.getNom());
        existing.setSurface(updated.getSurface());
        existing.setActif(updated.getActif());
        return parcelleRepository.save(existing);
    }

    public void desactiver(Long id) {
        Parcelle parcelle = findById(id);
        parcelle.setActif(false);
        parcelleRepository.save(parcelle);
    }

    public void delete(Long id) {
        parcelleRepository.deleteById(id);
    }
}
