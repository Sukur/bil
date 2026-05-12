package com.bil.rates.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "import_audit")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ImportAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_file", nullable = false, length = 512)
    private String sourceFile;

    @Column(length = 128)
    private String parser;

    @Column(name = "ratesheet_id")
    private Long ratesheetId;

    @Column(name = "lines_imported")
    private Integer linesImported;

    @Column(nullable = false, length = 16)
    private String status; // OK / FAILED

    @Column(length = 2000)
    private String message;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;
}

