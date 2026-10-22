package com.raphael.plantation_manager.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "employe")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employe_id")
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    private String photo;

    @Column(nullable = false)
    private Boolean actif = true;

    @Column(name = "taux_horaire")
    private Double tauxHoraire;

    // Hash BCrypt du code PIN de la borne : reçu en clair à la création, jamais renvoyé dans le JSON
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "code_pin")
    private String codePin;

    @JsonIgnore
    @OneToMany(mappedBy = "employe", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Travail> travaux;

    @JsonIgnore
    @OneToMany(mappedBy = "employe", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Pointage> pointages;

    public Employee() {}

    public Long getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }
    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }
    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }
    public Double getTauxHoraire() { return tauxHoraire; }
    public void setTauxHoraire(Double tauxHoraire) { this.tauxHoraire = tauxHoraire; }
    public String getCodePin() { return codePin; }
    public void setCodePin(String codePin) { this.codePin = codePin; }
    public boolean isPinDefini() { return codePin != null && !codePin.isBlank(); }
    public List<Travail> getTravaux() { return travaux; }
    public void setTravaux(List<Travail> travaux) { this.travaux = travaux; }
    public List<Pointage> getPointages() { return pointages; }
    public void setPointages(List<Pointage> pointages) { this.pointages = pointages; }
}
