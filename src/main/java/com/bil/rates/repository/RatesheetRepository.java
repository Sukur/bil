package com.bil.rates.repository;

import com.bil.rates.domain.Ratesheet;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RatesheetRepository extends JpaRepository<Ratesheet, Long> {

    @EntityGraph(attributePaths = "carrier")
    List<Ratesheet> findByCarrier_Scac(String scac);

    @Override
    @EntityGraph(attributePaths = "carrier")
    List<Ratesheet> findAll();

    @Override
    @EntityGraph(attributePaths = "carrier")
    Optional<Ratesheet> findById(Long id);
}

