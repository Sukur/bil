package com.bil.rates.repository;

import com.bil.rates.domain.Carrier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CarrierRepository extends JpaRepository<Carrier, Long> {
    Optional<Carrier> findByScac(String scac);
}

