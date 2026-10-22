package com.raphael.plantation_manager.repository;

import com.raphael.plantation_manager.entity.Pointage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PointageRepository extends JpaRepository<Pointage, Long> {

    List<Pointage> findByDateOrderByHeureArriveeAsc(LocalDate date);

    List<Pointage> findByDateBetweenOrderByDateAscHeureArriveeAsc(LocalDate debut, LocalDate fin);

    List<Pointage> findByEmployeIdAndDateBetweenOrderByDateAscHeureArriveeAsc(Long employeId, LocalDate debut, LocalDate fin);

    Optional<Pointage> findFirstByEmployeIdAndDateAndHeureDepartIsNull(Long employeId, LocalDate date);

    // Pointages des jours passés sans départ : oubli à corriger par le patron
    List<Pointage> findByDateBeforeAndHeureDepartIsNullOrderByDateAsc(LocalDate date);
}
