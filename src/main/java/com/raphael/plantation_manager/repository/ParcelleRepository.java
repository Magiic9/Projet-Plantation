package com.raphael.plantation_manager.repository;

import com.raphael.plantation_manager.entity.Parcelle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ParcelleRepository extends JpaRepository<Parcelle, Long> {

    List<Parcelle> findByActifTrue();
}
