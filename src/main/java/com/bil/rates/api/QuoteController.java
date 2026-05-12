package com.bil.rates.api;

import com.bil.rates.api.dto.*;
import com.bil.rates.service.QuoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class QuoteController {

    private final QuoteService quoteService;

    @PostMapping("/quotes")
    public ResponseEntity<QuoteResponse> quote(@RequestBody QuoteRequest req) {
        QuoteResponse resp = quoteService.quote(req);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/quotes/compare")
    public ResponseEntity<QuoteCompareResponse> compare(@RequestBody QuoteRequest req) {
        return ResponseEntity.ok(quoteService.compare(req));
    }

    @GetMapping("/quotes/{id}")
    public ResponseEntity<QuoteResponse> getQuote(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(quoteService.getQuote(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/quotes")
    public PageDto<QuoteResponse> listQuotes(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return quoteService.listQuotes(page, size);
    }

    @GetMapping("/ports")
    public List<PortDto> ports(@RequestParam(required = false, defaultValue = "") String q) {
        return quoteService.searchPorts(q);
    }

    @GetMapping("/carriers")
    public List<CarrierDto> carriers() {
        return quoteService.listCarriers();
    }
}
