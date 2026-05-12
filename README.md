# BIL — Digital Freight Forwarder Platform

3rd-party freight-forwarder platforması — müştəri ilə daşıyıcı arasında vasitəçilik
edərək door-to-door yükdaşımalarını idarə edir. **Faza 1: dəniz konteyner tarif idarəetməsi və quotation engine.**

Tam plan: [`PLAN.md`](./PLAN.md).

## Stack

- Java 21, Spring Boot 3.3.5, Gradle 9
- PostgreSQL (prod) / H2 (dev) + Liquibase
- Apache POI 5.3 (Excel ingestion)
- MapStruct, Lombok
- Testcontainers, JUnit 5

## Quick start

```bash
# build + test
./gradlew test

# run (H2 dev mode, port 8080)
./gradlew bootRun

# Swagger UI
open http://localhost:8080/swagger
# H2 console
open http://localhost:8080/h2          # JDBC URL: jdbc:h2:file:./data/bil
```

## End-to-end nümunə (NCPE ratesheet idxalı)

```bash
# 1) Excel ratesheet upload
curl -X POST -F 'file=@docs/NCPE - 2026.04.01-2026.05.31 - DRY FAK DE.NL.BE Tariff (V.2026.03.04).xlsx' \
  http://localhost:8080/api/v1/ratesheets/import
# → {"ratesheetId":1,"parser":"NcpeFakParser","linesImported":1413}

# 2) Siyahı
curl http://localhost:8080/api/v1/ratesheets

# 3) Detal (POL/POD/equipment səviyyəsində 1413 rate line)
curl http://localhost:8080/api/v1/ratesheets/1
```

## Layihə strukturu

```
src/main/java/com/bil/
├── BilApplication.java
└── rates/
    ├── api/                    REST controllers
    ├── domain/                 JPA entities + enums (Equipment, RatesheetType, ...)
    ├── repository/             Spring Data repos
    └── ingest/
        ├── RatesheetParser     interface (adapter pattern)
        ├── RatesheetParserResolver
        ├── RatesheetIngestionService
        ├── CellReader          POI helpers
        ├── CanonicalRatesheetDto / CanonicalRateLineDto
        └── ncpe/NcpeFakParser  pilot parser

src/main/resources/
├── application.properties
└── db/changelog/
    ├── db.changelog-master.yaml
    └── changes/001-initial-schema.yaml

docs/                  carrier ratesheet (.xlsx) seed data
docs/_introspection/   sheet structure JSON (parser fixture mənbəyi)
tools/                 Python utility (excel introspection, schema writer)
PLAN.md                detallı yol xəritəsi və arxitektura
```

## Yeni carrier üçün parser əlavə etmə resepti

1. `tools/excel_introspect.py` icra et (artıq edilib) və `docs/_introspection/<file>.json`-u oxu.
2. `com.bil.rates.ingest.<carrier>` paketi yarat və `RatesheetParser`-i implementasiya et.
3. `@Component` annotasiyası ilə qeydiyyatdan keçir — `RatesheetParserResolver` avtomatik götürür.
4. `supports(filename, workbook)` predikatını dəqiq yaz (ad pattern + sheet adı yoxlaması).
5. Real fayl ilə test əlavə et (NcpeFakParserTest pattern-inə baxın).

## Növbəti addımlar (PLAN.md §6)

- `HmmSpecialOogParser` (1 sheet, asan)
- `OneOceanRatesheetParser` (AET EB / IET / ZFS — paylaşılan şablon, + PUDO inland data)
- `POST /api/v1/quotes` quotation engine (matching + surcharge aggregation + markup)
- UN/LOCODE seed (UNECE dataset)
- Postgres profile (`application-prod.properties`)
- Keycloak + Spring Security

