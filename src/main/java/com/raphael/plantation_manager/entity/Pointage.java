package com.raphael.plantation_manager.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Présence d'un employé sur la plantation : une arrivée et un départ.
 * Un employé peut avoir plusieurs pointages le même jour (ex. : pause du midi),
 * mais un seul ouvert (sans départ) à la fois.
 */
@Entity
@Table(name = "pointage")
public class Pointage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pointage_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employe_id", nullable = false)
    @JsonIgnoreProperties({"travaux", "pointages", "hibernateLazyInitializer"})
    private Employee employe;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "heure_arrivee", nullable = false)
    private LocalTime heureArrivee;

    @Column(name = "heure_depart")
    private LocalTime heureDepart;

    public Pointage() {}

    public Long getId() { return id; }
    public Employee getEmploye() { return employe; }
    public void setEmploye(Employee employe) { this.employe = employe; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public LocalTime getHeureArrivee() { return heureArrivee; }
    public void setHeureArrivee(LocalTime heureArrivee) { this.heureArrivee = heureArrivee; }
    public LocalTime getHeureDepart() { return heureDepart; }
    public void setHeureDepart(LocalTime heureDepart) { this.heureDepart = heureDepart; }
}
