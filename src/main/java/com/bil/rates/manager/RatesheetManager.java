package com.bil.rates.manager;

import com.bil.rates.domain.*;
import com.bil.rates.ingest.CanonicalRateLineDto;
import com.bil.rates.ingest.CanonicalRatesheetDto;
import com.bil.rates.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Persistence-oriented domain manager.
 *
 * Sits between the {@code service} layer (orchestration / use-cases) and Spring Data
 * {@code repository} interfaces. All entity ↔ DB write logic for the rates aggregate
 * is centralised here so services contain only orchestration & policy.
 */
@Component
@RequiredArgsConstructor
public class RatesheetManager {

    private final CarrierRepository carrierRepo;
    private final PortRepository portRepo;
    private final RatesheetRepository ratesheetRepo;
    private final RateLineRepository rateLineRepo;

    // ---------- write side ----------

    @Transactional
    public Ratesheet persist(CanonicalRatesheetDto dto) {
        Carrier carrier = upsertCarrier(dto.getCarrierScac(), dto.getCarrierName());
        upsertPortsFromDto(dto);

        LocalDate from = Optional.ofNullable(dto.getValidFrom()).orElse(LocalDate.now());
        LocalDate to   = Optional.ofNullable(dto.getValidTo()).orElse(from.plusDays(30));

        Ratesheet rs = Ratesheet.builder()
                .carrier(carrier)
                .source(dto.getSource())
                .sourceFile(dto.getSourceFile())
                .contractNo(dto.getContractNo())
                .type(dto.getType() != null ? dto.getType() : RatesheetType.FAK)
                .currency(dto.getCurrency())
                .validFrom(from)
                .validTo(to)
                .status(LocalDate.now().isAfter(to) ? RatesheetStatus.EXPIRED : RatesheetStatus.ACTIVE)
                .version(dto.getVersion())
                .uploadedAt(Instant.now())
                .tenantId("default")
                .build();
        rs = ratesheetRepo.save(rs);

        if (dto.getLines() != null && !dto.getLines().isEmpty()) {
            List<RateLine> batch = new ArrayList<>(dto.getLines().size());
            for (CanonicalRateLineDto l : dto.getLines()) {
                batch.add(RateLine.builder()
                        .ratesheet(rs)
                        .pol(l.getPol())
                        .pod(l.getPod())
                        .via(l.getVia())
                        .service(null)
                        .equipment(l.getEquipment())
                        .commodity(l.getCommodity() != null ? l.getCommodity() : "FAK")
                        .baseAmount(l.getBaseAmount())
                        .currency(l.getCurrency() != null ? l.getCurrency() : dto.getCurrency())
                        .transitDays(l.getTransitDays())
                        .build());
            }
            rateLineRepo.saveAll(batch);
        }
        return rs;
    }

    private Carrier upsertCarrier(String scac, String name) {
        return carrierRepo.findByScac(scac)
                .orElseGet(() -> carrierRepo.save(Carrier.builder()
                        .scac(scac)
                        .name(name != null ? name : scac)
                        .build()));
    }

    private void upsertPortsFromDto(CanonicalRatesheetDto dto) {
        if (dto.getLines() == null) return;
        for (CanonicalRateLineDto l : dto.getLines()) {
            upsertPort(l.getPol(), l.getPolName());
            upsertPort(l.getPod(), l.getPodName());
        }
    }

    private void upsertPort(String unloc, String name) {
        if (unloc == null || unloc.length() != 5) return;
        if (portRepo.existsById(unloc)) return;
        portRepo.save(Port.builder()
                .unloc(unloc)
                .name(name != null ? name : unloc)
                .country(unloc.substring(0, 2))
                .build());
    }

    // ---------- read side ----------

    @Transactional(readOnly = true)
    public List<Ratesheet> findAllRatesheets() {
        return ratesheetRepo.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Ratesheet> findRatesheet(Long id) {
        return ratesheetRepo.findById(id);
    }

    @Transactional(readOnly = true)
    public long countRateLines(Long ratesheetId) {
        return rateLineRepo.countByRatesheet_Id(ratesheetId);
    }

    @Transactional(readOnly = true)
    public Page<RateLine> findLinesByRatesheet(Long ratesheetId, Pageable pageable) {
        return rateLineRepo.findByRatesheet_Id(ratesheetId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<RateLine> searchLines(String pol, String pod, Equipment equipment, Pageable pageable) {
        String polFilter = (pol == null || pol.isBlank()) ? null : pol.trim();
        String podFilter = (pod == null || pod.isBlank()) ? null : pod.trim();
        return rateLineRepo.search(polFilter, podFilter, equipment, pageable);
    }

    @Transactional(readOnly = true)
    public long countCarriers()    { return carrierRepo.count(); }
    @Transactional(readOnly = true)
    public long countRatesheets()  { return ratesheetRepo.count(); }
    @Transactional(readOnly = true)
    public long countAllRateLines(){ return rateLineRepo.count(); }
    @Transactional(readOnly = true)
    public long countPorts()       { return portRepo.count(); }
    @Transactional(readOnly = true)
    public long countActiveRatesheets() {
        return ratesheetRepo.findAll().stream()
                .filter(r -> r.getStatus() == RatesheetStatus.ACTIVE)
                .count();
    }
}

