package com.bil.rates.service;

import com.bil.rates.api.dto.*;
import com.bil.rates.domain.Equipment;
import com.bil.rates.domain.RateLine;
import com.bil.rates.domain.Ratesheet;
import com.bil.rates.ingest.RatesheetParser;
import com.bil.rates.ingest.RatesheetParserResolver;
import com.bil.rates.manager.DtoAssembler;
import com.bil.rates.manager.RatesheetManager;
import com.bil.rates.repository.ImportAuditRepository;
import com.bil.rates.domain.ImportAudit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;

/**
 * Use-case orchestration layer.
 *
 * Delegates persistence to {@link RatesheetManager} and uses
 * {@link DtoAssembler} for entity→DTO conversion. Never returns raw entities.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RatesheetService {

    private final RatesheetParserResolver resolver;
    private final RatesheetManager manager;
    private final ImportAuditRepository auditRepo;

    // ─── write ───────────────────────────────────────────────────────────────

    @Transactional
    public ImportResultDto importExcel(String fileName, InputStream content) {
        ImportAudit audit = ImportAudit.builder()
                .sourceFile(fileName)
                .importedAt(Instant.now())
                .status("FAILED")
                .build();
        try (Workbook wb = WorkbookFactory.create(content)) {
            RatesheetParser parser = resolver.resolve(fileName, wb);
            audit.setParser(parser.name());

            var dto = parser.parse(fileName, wb);
            Ratesheet saved = manager.persist(dto);

            int lines = dto.getLines() == null ? 0 : dto.getLines().size();
            audit.setRatesheetId(saved.getId());
            audit.setLinesImported(lines);
            audit.setStatus("OK");
            audit.setMessage("Imported " + lines + " lines via " + parser.name());
            auditRepo.save(audit);

            log.info("Ratesheet {} imported ({} lines) via {}", saved.getId(), lines, parser.name());
            return new ImportResultDto(saved.getId(), parser.name(), lines,
                    "Successfully imported " + lines + " rate lines");
        } catch (IOException ioe) {
            audit.setMessage("IO: " + ioe.getMessage());
            auditRepo.save(audit);
            throw new IllegalStateException("Workbook read failed: " + fileName, ioe);
        } catch (RuntimeException ex) {
            String msg = ex.getMessage();
            audit.setMessage(msg != null && msg.length() > 1900 ? msg.substring(0, 1900) : msg);
            auditRepo.save(audit);
            throw ex;
        }
    }

    // ─── read ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RatesheetSummaryDto> listRatesheets() {
        return manager.findAllRatesheets().stream()
                .map(rs -> DtoAssembler.toSummary(rs, manager.countRateLines(rs.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public RatesheetSummaryDto getRatesheet(Long id) {
        Ratesheet rs = manager.findRatesheet(id)
                .orElseThrow(() -> new IllegalArgumentException("Ratesheet not found: " + id));
        return DtoAssembler.toSummary(rs, manager.countRateLines(id));
    }

    @Transactional(readOnly = true)
    public PageDto<RateLineDto> getRatesheetLines(Long id, int page, int size) {
        Page<RateLine> p = manager.findLinesByRatesheet(id,
                PageRequest.of(page, size, Sort.by("pol", "pod")));
        return new PageDto<>(
                p.getContent().stream().map(DtoAssembler::toLineDto).toList(),
                p.getTotalElements(), p.getTotalPages(), page, size);
    }

    @Transactional(readOnly = true)
    public PageDto<RateLineDto> searchRates(String pol, String pod, String equipment,
                                             int page, int size) {
        Equipment eq = null;
        if (equipment != null && !equipment.isBlank()) {
            try { eq = Equipment.valueOf(equipment.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }
        Page<RateLine> p = manager.searchLines(pol, pod, eq,
                PageRequest.of(page, size, Sort.by("baseAmount")));
        return new PageDto<>(
                p.getContent().stream().map(DtoAssembler::toLineDto).toList(),
                p.getTotalElements(), p.getTotalPages(), page, size);
    }

    @Transactional(readOnly = true)
    public DashboardStatsDto stats() {
        return new DashboardStatsDto(
                manager.countCarriers(),
                manager.countActiveRatesheets(),
                manager.countRatesheets(),
                manager.countAllRateLines(),
                manager.countPorts());
    }
}

