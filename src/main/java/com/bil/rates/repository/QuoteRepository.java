package com.bil.rates.repository;
import com.bil.rates.domain.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
public interface QuoteRepository extends JpaRepository<Quote, Long> {
    @EntityGraph(attributePaths = "charges")
    Page<Quote> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
