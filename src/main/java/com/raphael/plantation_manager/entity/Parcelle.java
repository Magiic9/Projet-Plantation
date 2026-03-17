package com.raphael.plantation_manager.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

@Entity
@Table(name = "parcelle")
public class Parcelle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "parcelle_id")
    private Long id;

    @Column(nullable = false)
    private String nom;

    private Double surface;

    @Column(nullable = false)
    private Boolean actif = true;

    @JsonIgnore
    @OneToMany(mappedBy = "parcelle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Travail> travaux;

    public Parcelle() {}

    public Long getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public Double getSurface() { return surface; }
    public void setSurface(Double surface) { this.surface = surface; }
    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }
    public List<Travail> getTravaux() { return travaux; }
    public void setTravaux(List<Travail> travaux) { this.travaux = travaux; }
}
