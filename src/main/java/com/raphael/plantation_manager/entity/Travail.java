package com.raphael.plantation_manager.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "travail")
public class Travail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "travail_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employe_id", nullable = false)
    @JsonIgnoreProperties({"travaux", "hibernateLazyInitializer"})
    private Employee employe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tache_id", nullable = false)
    @JsonIgnoreProperties({"travaux", "hibernateLazyInitializer"})
    private Tache tache;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parcelle_id", nullable = false)
    @JsonIgnoreProperties({"travaux", "hibernateLazyInitializer"})
    private Parcelle parcelle;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "heure_debut")
    private LocalTime heureDebut;

    @Column(name = "heure_fin")
    private LocalTime heureFin;

    public Travail() {}

    public Long getId() { return id; }
    public Employee getEmploye() { return employe; }
    public void setEmploye(Employee employe) { this.employe = employe; }
    public Tache getTache() { return tache; }
    public void setTache(Tache tache) { this.tache = tache; }
    public Parcelle getParcelle() { return parcelle; }
    public void setParcelle(Parcelle parcelle) { this.parcelle = parcelle; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public LocalTime getHeureDebut() { return heureDebut; }
    public void setHeureDebut(LocalTime heureDebut) { this.heureDebut = heureDebut; }
    public LocalTime getHeureFin() { return heureFin; }
    public void setHeureFin(LocalTime heureFin) { this.heureFin = heureFin; }
}
