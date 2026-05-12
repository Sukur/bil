package com.bil.rates.repository;

import com.bil.rates.domain.ImportAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportAuditRepository extends JpaRepository<ImportAudit, Long> {
}

