package com.bil.rates.service;

import com.bil.rates.api.dto.*;
import com.bil.rates.domain.*;
import com.bil.rates.repository.QuoteRepository;
import com.bil.rates.repository.RateLineRepository;
import com.bil.rates.repository.CarrierRepository;
import com.bil.rates.repository.PortRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MVP Quotation Engine.
 *
 * Algorithm (see PLAN.md §5):
 *  1. Find candidate RateLines matching pol/pod/equipment/validity.
 *  2. Pick cheapest (candidates ordered ASC by baseAmount).
 *  3. Apply fixed 10 % markup.
 *  4. Persist Quote + QuoteCharge rows.
 *  5. Return QuoteResponse DTO.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuoteService {

    private static final BigDecimal MARKUP_PCT = new BigDecimal("0.10");
    private static final int QUOTE_VALIDITY_DAYS = 7;

    private final RateLineRepository rateLineRepo;
    private final QuoteRepository quoteRepo;
    private final CarrierRepository carrierRepo;
    private final PortRepository portRepo;

    // ─── quote creation ──────────────────────────────────────────────────────

    @Transactional
    public QuoteResponse quote(QuoteRequest req) {
        if (req.pol() == null || req.pod() == null || req.equipment() == null) {
            throw new IllegalArgumentException("pol, pod and equipment are required");
        }

        Equipment equipment = Equipment.fromLabel(req.equipment());
        if (equipment == Equipment.OTHER) {
            throw new IllegalArgumentException("Unknown equipment type: " + req.equipment());
        }

        LocalDate readyDate = Optional.ofNullable(req.readyDate()).orElse(LocalDate.now());
        String pol = req.pol().trim().toUpperCase();
        String pod = req.pod().trim().toUpperCase();
        String commodity = (req.commodity() != null && !req.commodity().isBlank())
                ? req.commodity().trim() : "FAK";

        List<RateLine> candidates = rateLineRepo.findCandidates(
                pol, pod, equipment, readyDate, RatesheetStatus.ACTIVE);

        if (candidates.isEmpty()) {
            log.info("No rates found for {} → {} [{}] on {}", pol, pod, equipment, readyDate);
            return noRate(pol, pod, equipment.name(), commodity, readyDate,
                    "No active rates found for " + pol + " → " + pod
                    + " [" + equipment + "] on " + readyDate);
        }

        RateLine best = candidates.getFirst();
        Ratesheet rs = best.getRatesheet();

        BigDecimal ocean  = best.getBaseAmount();
        BigDecimal markup = ocean.multiply(MARKUP_PCT).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total  = ocean.add(markup).setScale(2, RoundingMode.HALF_UP);

        LocalDate ratesheetValidTo = rs.getValidTo();
        LocalDate quoteValidUntil  = LocalDate.now().plusDays(QUOTE_VALIDITY_DAYS);
        if (quoteValidUntil.isAfter(ratesheetValidTo)) quoteValidUntil = ratesheetValidTo;

        Quote quote = Quote.builder()
                .tenantId("default")
                .pol(pol).pod(pod)
                .equipment(equipment).commodity(commodity)
                .readyDate(readyDate).status(QuoteStatus.OPEN)
                .validUntil(quoteValidUntil)
                .carrierScac(rs.getCarrier().getScac())
                .carrierName(rs.getCarrier().getName())
                .rateLineId(best.getId())
                .ratesheetId(rs.getId())
                .oceanFreight(ocean).markupAmount(markup).totalAmount(total)
                .currency(best.getCurrency())
                .transitDays(best.getTransitDays())
                .weightKg(req.weightKg())
                .candidateCount(candidates.size())
                .createdAt(Instant.now())
                .build();

        List<QuoteCharge> charges = new ArrayList<>();

        QuoteCharge c1 = QuoteCharge.builder()
                .quote(quote).code("OCEAN_FREIGHT").description("Ocean Freight")
                .amount(ocean).currency(best.getCurrency()).basis("PER_CNTR")
                .build();
        QuoteCharge c2 = QuoteCharge.builder()
                .quote(quote).code("MARKUP").description("Service Margin (10%)")
                .amount(markup).currency(best.getCurrency()).basis("PER_CNTR")
                .build();
        charges.add(c1);
        charges.add(c2);
        quote.setCharges(charges);

        Quote saved = quoteRepo.save(quote);
        log.info("Quote {} created: {} → {} [{}] {} {} (from {} candidates)",
                saved.getId(), pol, pod, equipment, total, best.getCurrency(), candidates.size());

        return toResponse(saved);
    }

    // ─── read ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public QuoteResponse getQuote(Long id) {
        Quote q = quoteRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quote not found: " + id));
        return toResponse(q);
    }

    @Transactional(readOnly = true)
    public PageDto<QuoteResponse> listQuotes(int page, int size) {
        Page<Quote> p = quoteRepo.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        return new PageDto<>(
                p.getContent().stream().map(this::toResponse).toList(),
                p.getTotalElements(), p.getTotalPages(), page, size);
    }

    // ─── port / carrier helpers ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PortDto> searchPorts(String q) {
        if (q == null || q.isBlank()) return List.of();
        return portRepo.search(q.trim()).stream()
                .limit(20)
                .map(p -> new PortDto(p.getUnloc(), p.getName(), p.getCountry()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CarrierDto> listCarriers() {
        return carrierRepo.findAll().stream()
                .map(c -> new CarrierDto(c.getId(), c.getScac(), c.getName()))
                .toList();
    }

    // ─── compare (multi-carrier options) ────────────────────────────────────

    @Transactional(readOnly = true)
    public QuoteCompareResponse compare(QuoteRequest req) {
        Equipment equipment = Equipment.fromLabel(req.equipment());
        if (equipment == Equipment.OTHER)
            throw new IllegalArgumentException("Unknown equipment type: " + req.equipment());

        LocalDate readyDate = Optional.ofNullable(req.readyDate()).orElse(LocalDate.now());
        String pol      = req.pol().trim().toUpperCase();
        String pod      = req.pod().trim().toUpperCase();
        String commodity = Optional.ofNullable(req.commodity()).filter(s -> !s.isBlank()).orElse("FAK");

        List<RateLine> all = rateLineRepo.findCandidates(pol, pod, equipment, readyDate, RatesheetStatus.ACTIVE);

        // Best (cheapest) per carrier
        Map<String, RateLine> bestPerCarrier = new LinkedHashMap<>();
        for (RateLine rl : all) {
            String scac = rl.getRatesheet().getCarrier().getScac();
            bestPerCarrier.merge(scac, rl, (a, b) -> a.getBaseAmount().compareTo(b.getBaseAmount()) <= 0 ? a : b);
        }

        // Sort: cheapest first
        List<RateLine> ranked = bestPerCarrier.values().stream()
                .sorted(Comparator.comparing(RateLine::getBaseAmount))
                .toList();

        // Identify "fastest" = min transitDays (only if not null)
        RateLine fastest = ranked.stream()
                .filter(r -> r.getTransitDays() != null)
                .min(Comparator.comparingInt(RateLine::getTransitDays))
                .orElse(null);

        List<QuoteOption> options = new ArrayList<>();
        for (int i = 0; i < ranked.size(); i++) {
            RateLine rl = ranked.get(i);
            BigDecimal ocean  = rl.getBaseAmount();
            BigDecimal markup = ocean.multiply(MARKUP_PCT).setScale(2, RoundingMode.HALF_UP);
            BigDecimal total  = ocean.add(markup).setScale(2, RoundingMode.HALF_UP);

            List<String> tags = new ArrayList<>();
            if (i == 0) tags.add("CHEAPEST");
            if (rl == fastest) tags.add("FASTEST");
            if (i == 0 && rl == fastest) { tags.clear(); tags.add("BEST VALUE"); }
            // "BEST VALUE" if good price + reasonable transit
            if (tags.isEmpty() && rl.getTransitDays() != null && i <= 1) tags.add("RECOMMENDED");

            String primaryTag = tags.isEmpty() ? "" : tags.get(0);

            options.add(new QuoteOption(
                    primaryTag,
                    rl.getRatesheet().getCarrier().getScac(),
                    rl.getRatesheet().getCarrier().getName(),
                    ocean, markup, total,
                    rl.getCurrency(),
                    rl.getTransitDays(),
                    rl.getId(),
                    tags
            ));
        }

        return new QuoteCompareResponse(pol, pod, equipment.name(), commodity,
                readyDate.toString(), all.size(), options);
    }

    // ─── mapping ─────────────────────────────────────────────────────────────

    private QuoteResponse toResponse(Quote q) {
        List<QuoteChargeDto> chargeDtos = q.getCharges().stream()
                .map(c -> new QuoteChargeDto(c.getCode(), c.getDescription(),
                        c.getAmount(), c.getCurrency(), c.getBasis()))
                .toList();

        return new QuoteResponse(
                q.getId(), true,
                q.getPol(), q.getPod(),
                q.getEquipment().name(), q.getCommodity(),
                q.getReadyDate(),
                q.getCarrierScac(), q.getCarrierName(),
                q.getOceanFreight(), q.getMarkupAmount(), q.getTotalAmount(),
                q.getCurrency(), q.getValidUntil(), q.getTransitDays(),
                q.getCandidateCount(), chargeDtos,
                q.getCreatedAt(), null);
    }

    private static QuoteResponse noRate(String pol, String pod, String eq,
                                         String commodity, LocalDate readyDate,
                                         String message) {
        return new QuoteResponse(
                null, false, pol, pod, eq, commodity, readyDate,
                null, null, null, null, null, null, null, null, 0,
                List.of(), null, message);
    }
}
