package com.raphael.plantation_manager.repository;

import com.raphael.plantation_manager.entity.Travail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TravailRepository extends JpaRepository<Travail, Long> {

    List<Travail> findByEmployeId(Long employeId);

    List<Travail> findByEmployeIdAndDateBetween(Long employeId, LocalDate debut, LocalDate fin);

    List<Travail> findByEmployeIdAndDate(Long employeId, LocalDate date);

    List<Travail> findByDate(LocalDate date);

    List<Travail> findByParcelleId(Long parcelleId);

    List<Travail> findByTacheId(Long tacheId);

    // Récupère tous les travaux terminés sur une période — le calcul des heures se fait côté Java
    @Query("SELECT t FROM Travail t WHERE t.date BETWEEN :debut AND :fin AND t.heureFin IS NOT NULL")
    List<Travail> findTravailTerminesSurPeriode(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);
}
