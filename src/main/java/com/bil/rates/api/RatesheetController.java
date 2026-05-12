package com.bil.rates.api;

import com.bil.rates.api.dto.*;
import com.bil.rates.service.RatesheetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * REST layer — delegates entirely to {@link RatesheetService}.
 * Never touches entities or repositories directly.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RatesheetController {

    private final RatesheetService service;

    @PostMapping(value = "/ratesheets/import", consumes = "multipart/form-data")
    public ResponseEntity<ImportResultDto> importExcel(
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(service.importExcel(file.getOriginalFilename(), file.getInputStream()));
    }

    @GetMapping("/ratesheets")
    public List<RatesheetSummaryDto> list() {
        return service.listRatesheets();
    }

    @GetMapping("/ratesheets/{id}")
    public ResponseEntity<RatesheetSummaryDto> detail(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.getRatesheet(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/ratesheets/{id}/lines")
    public PageDto<RateLineDto> lines(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return service.getRatesheetLines(id, page, size);
    }

    @GetMapping("/rates/search")
    public PageDto<RateLineDto> search(
            @RequestParam(required = false) String pol,
            @RequestParam(required = false) String pod,
            @RequestParam(required = false) String equipment,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return service.searchRates(pol, pod, equipment, page, size);
    }

    @GetMapping("/dashboard/stats")
    public DashboardStatsDto stats() {
        return service.stats();
    }
}

