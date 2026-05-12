package com.bil.rates.repository;

import com.bil.rates.domain.Equipment;
import com.bil.rates.domain.RateLine;
import com.bil.rates.domain.RatesheetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RateLineRepository extends JpaRepository<RateLine, Long> {

    List<RateLine> findByRatesheet_Id(Long ratesheetId);

    Page<RateLine> findByRatesheet_Id(Long ratesheetId, Pageable pageable);

    /** Fuzzy search across active rate lines for the dashboard. */
    @EntityGraph(attributePaths = "ratesheet")
    @Query("""
            select rl from RateLine rl
            where (:pol is null or upper(rl.pol) like upper(concat('%', :pol, '%')))
              and (:pod is null or upper(rl.pod) like upper(concat('%', :pod, '%')))
              and (:equipment is null or rl.equipment = :equipment)
            order by rl.baseAmount asc
            """)
    Page<RateLine> search(@Param("pol") String pol,
                          @Param("pod") String pod,
                          @Param("equipment") Equipment equipment,
                          Pageable pageable);

    long countByRatesheet_Id(Long ratesheetId);

    /**
     * Quotation engine candidate lookup.
     * Returns lines where pol/pod/equipment match and the ratesheet validity window covers readyDate.
     * Ordered cheapest-first so the first result is the best candidate.
     */
    @EntityGraph(attributePaths = {"ratesheet", "ratesheet.carrier"})
    @Query("""
            select rl from RateLine rl
            join rl.ratesheet rs
            where rl.pol = :pol
              and rl.pod = :pod
              and rl.equipment = :equipment
              and rs.validFrom <= :readyDate
              and rs.validTo   >= :readyDate
              and rs.status = :status
            order by rl.baseAmount asc
            """)
    List<RateLine> findCandidates(@Param("pol") String pol,
                                  @Param("pod") String pod,
                                  @Param("equipment") Equipment equipment,
                                  @Param("readyDate") LocalDate readyDate,
                                  @Param("status") RatesheetStatus status);
}

