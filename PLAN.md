# PLAN.md — BIL Freight Forwarding Platform (Spring Boot)

## TL;DR
`docs/` qovluğundakı 10 ədəd Aprel 2026 dəniz ratesheet-ləri (ONE, HMM, COSCO, OOCL, NCPE) seed data kimi istifadə edilərək, **Spring Boot 3.3.x + PostgreSQL + Apache POI** əsasında modullu monolit kimi NVOCC/digital freight forwarder platforması qurulur. MVP — dəniz yük tarif idarəetməsi və quotation engine; sonra inland trucking, customs, sənədləşmə və tracking əlavə olunur.

---

## 1. Mövcud Excel Ratesheet-lərin Analizi

### 1.1. Faylların inventarı

| # | Fayl | Carrier | Region/Trade | Tip | Validity | Qeyd |
|---|------|---------|--------------|-----|----------|------|
| 1 | AET EB - ONE Germany Ratesheet 01.04.-30.04.2026 | ONE | Germany export, AET (Asia–Europe Trade) Eastbound | DRY FAK | 01–30 Apr 2026 | NVOCC AET EB |
| 2 | CJ ICM Logistics – HMM GER Special Ratesheet APR 2026 | HMM | Germany export, special account | DRY (special) | Aprel 2026 | NAC / named account |
| 3 | CJ ICM Logistics – HMM Ratesheet Germany April 2026 | HMM | Germany export | DRY FAK | Aprel 2026 | Standart |
| 4 | COSCO – IET SB Ratesheets 01.–30.04.2026 | COSCO | IET (Intra-Europe) Southbound | DRY | 01–30 Apr 2026 | Intra-EU |
| 5 | COSCO Germany – FAK OOG 01.–30.04.2026 V2 | COSCO | Germany | **OOG** | Apr 2026 | Special equipment |
| 6 | COSCO Germany – FAK REEFER 01.–30.04.2026 V1 | COSCO | Germany | **REEFER** | Apr 2026 | Soyuducu |
| 7 | IET - ONE Germany Ratesheet 01.04.-30.04.2026 | ONE | Germany, IET | DRY | Apr 2026 | Intra-Europe |
| 8 | NCPE – 2026.04.01–2026.05.31 DRY FAK DE.NL.BE | NCPE | DE/NL/BE export | DRY FAK | 01 Apr – 31 May 2026 | Multi-origin |
| 9 | REVISED OOCL Export FAK FE/ME/India/AU/IntraEU/Africa | OOCL | Global trades | DRY FAK | 01.12.2025–31.01.2026 | Köhnə (referans) |
| 10 | ZFS – ONE Germany Ratesheet 01.04.–30.04.2026 | ONE | Germany ZFS | DRY | Apr 2026 | NAC |

### 1.2. Tipik ratesheet sxemi

**Header:** carrier, contract/quotation no, NAC kodu, validity from/to, free time, allocation, currency.

**Routing & commodity:** POL, POD, via/T-port, service string, transit time, commodity (FAK / HS code), equipment (20'DV, 40'DV, 40'HC, 20/40 RF, OOG-FR/OT/PL).

**Qiymət:** O/F base + surcharge-lər:
- **BAF/LSS/EBS** — bunker / low-sulphur
- **CAF** — currency adjustment
- **PSS / GRI** — peak season / general rate increase
- **THC O/D** — terminal handling
- **ENS** — EU entry summary
- **AMS/ACI** — US/CA manifest
- **ISPS** — security
- **DOC / B/L fee**, **Seal**, **CIC**, **OWS/OOG surcharge**
- **Reefer plug-in / monitoring** (REEFER)
- Free time (D&D origin + dest, məs. 7+7, 14)
- Special remarks (DG, IMO klası, weight cap)

### 1.3. Vahid normalizə model

```
Carrier(id, scac, name)
ServiceContract(id, carrier_id, contract_no, customer_id?, type[FAK/NAC/SPOT], currency)
Ratesheet(id, contract_id, source_file, valid_from, valid_to, version, status)
RateLine(id, ratesheet_id, pol_unloc, pod_unloc, via, service_string,
         equipment[20DV/40DV/40HC/20RF/40RF/OOG-FR/OOG-OT],
         commodity_code, commodity_desc[FAK|HS],
         base_freight_amount, currency, transit_time_days, allocation_teu)
SurchargeDefinition(id, code[BAF,THC,ENS,...], name, scope[ORIGIN/FREIGHT/DEST])
RateSurcharge(id, rate_line_id|ratesheet_id, code, amount, currency,
              basis[PER_CNTR|PER_BL|PER_TEU|PERCENT], equipment_filter, mandatory)
FreeTimeRule(id, rate_line_id, side[ORIGIN/DEST], combined, days)
RateCondition(id, rate_line_id, key, value)   // DG, IMO, weight-cap
Port(unloc, name, country, region)
```

OOG üçün `equipment` "OOG-OT/FR/PL" + `RateCondition` (dimensions/overweight). REEFER üçün `temperature_range`, `ventilation`, `humidity` ya `RateCondition`-da, ya da `reefer_attrs` extension cədvəlində.

---

## 2. Sənaye Tədqiqatı

### 2.1. Əsas oyunçular

| Şirkət | Tip | Güclü tərəf | Bizim üçün dərs |
|--------|-----|-------------|-----------------|
| **Flexport** | Digital FF | UX, visibility | Quote→Book→Track tək axın |
| **Forto** | EU digital FF | Almaniya, multi-modal | Bizim əsas benchmark |
| **Freightos / WebCargo** | Marketplace | Instant rate aggregator | Spot rate API |
| **Maersk Spot** | Carrier-direct | Zəmanətli yer | Instant booking UX |
| **Kuehne+Nagel myKN** | 3PL portal | Geniş portfel | Modullu portal |
| **DSV MyDSV** | 3PL | Quote + tracking | Sadə workflow |
| **Twill / ONE Quote** | Carrier digital | API-first | İnteqrasiya hədəfi |
| **iContainers / xChange** | Marketplace | SME fokus | Self-service quote |
| **Sennder** | Digital trucking | EU FTL | Inland leg partneri |
| **project44 / FourKites** | Visibility | Real-time tracking | Tracking partner |

### 2.2. Standart freight forwarding prosesi

```
1. Lead/RFQ            → müştəri sorğusu
2. Quotation           → tarif uyğunlaşdırma + markup
3. Booking request     → carrier-ə SI/booking; allocation
4. Booking confirmation→ booking no, vessel/voyage, ETD/ETA
5. Pre-carriage        → trucking pickup
6. Origin handling     → gate-in, VGM, customs export
7. Loading & sailing   → loaded on board, MBL/HBL
8. In-transit tracking → vessel events, transhipment
9. Destination handling→ discharge, customs import, T1, release
10. On-carriage        → trucking to consignee, POD
11. Invoicing          → AR / AP
12. Claims & disputes  → cargo damage, demurrage waiver
```

### 2.3. Rate növləri
- **FAK** — Freight All Kinds (açıq tarif)
- **NAC / SPC** — named account (bizim HMM Special, ZFS)
- **Spot / Quote** — ad-hoc, qısa validity
- **Service contract (US-FMC)** — illik MQC həcmi
- **Allocation** — həftəlik TEU kvotası

### 2.4. Door-to-door legs

```
[Shipper door] → [Pre-carriage truck/rail] → [POL/THC]
   → [Ocean main] → [POD/THC] → [On-carriage] → [Consignee]
```

### 2.5. İnteqrasiyalar

| Sahə | Standart/Provayder |
|------|--------------------|
| Booking/SI/BL | INTTRA, GT Nexus, CargoSmart, **DCSA API** |
| Carrier API | Maersk, MSC, CMA, ONE, Hapag (DCSA T&T 2.x) |
| Visibility | project44, FourKites, MarineTraffic |
| Customs | DE **ATLAS**, NL **Portbase**, EU ICS2 (ENS) |
| Port community | dakosy, DBH (HH/BHV), Portbase |
| EDI | EDIFACT IFTMIN, IFTSTA, COPRAR, BAPLIE, INVOIC |
| Ödəniş | Stripe/Adyen B2B, SEPA, factoring (Stenn) |
| Geo | Google Maps, OSM (haulage tariff) |

---

## 3. Platforma Vizionu və Faza Yol Xəritəsi

**Vizion:** Almaniya-mərkəzli SME ixracatçılar üçün "instant quote → book → track" digital freight forwarder; əvvəlcə dəniz, sonra true door-to-door.

### Faza 1 — MVP (8–12 həftə): Ocean Rate & Quotation
- Excel ratesheet ingestion (Apache POI), normalizə RateLine-lara
- Port & carrier master data (UN/LOCODE)
- REST `POST /quotes` — instant quotation
- Booking request flow (manual)
- Admin UI (sadə) — ratesheet upload, audit, override
- Auth (Spring Security + JWT), rol: ADMIN, OPS, CUSTOMER

### Faza 2 (4–6 həftə): Pre-carriage / Inland trucking
- TruckingTariff modeli (zone/postcode → port)
- Trucking partner inteqrasiya (manual + API skelet)
- Door-to-port quotation kombinasiyası
- Address/geocoding

### Faza 3 (4–6 həftə): On-carriage
- Destination trucking tariff
- Port-to-door zone tarifləri
- Full door-to-door qiymət formalaşması

### Faza 4 (8–12 həftə): Customs, Docs, Tracking, Invoicing, Claims
- B/L, SI, VGM, HBL/MBL şablonları
- Customs skelet (ATLAS)
- Tracking aggregator (DCSA T&T + project44)
- Invoicing (AR/AP), DATEV export
- Claims iş axını

---

## 4. Texniki Arxitektura

### 4.1. Bounded contexts (Gradle multi-module)

```
bil/
├── bil-api          (REST controllers, OpenAPI)
├── bil-core         (shared kernel: Money, UnLocode, Equipment)
├── bil-rates        (Carrier, Ratesheet, RateLine, ingestion)
├── bil-quotation    (matching engine, pricing, markup)
├── bil-booking      (booking lifecycle, state machine)
├── bil-customer     (accounts, contacts, KYC, multi-tenancy)
├── bil-carrier      (carrier integration adapters)
├── bil-trucking     (Faza 2/3)
├── bil-tracking     (Faza 4)
├── bil-invoicing    (Faza 4)
├── bil-customs      (Faza 4)
└── bil-infra        (persistence, security, messaging)
```

### 4.2. Stack

| Layer | Texnologiya |
|-------|-------------|
| Runtime | **Java 21**, Spring Boot **3.3.x** (Boot 4.0.3 stable deyil — endir) |
| Web | Spring Web MVC, springdoc-openapi |
| Data | Spring Data JPA, **PostgreSQL 16**, Hibernate 6 |
| Migration | **Liquibase** |
| Mapping | **MapStruct** |
| Excel | **Apache POI 5.3+** (XSSF), **Tika** |
| Validation | Jakarta Validation, problem-spring-web |
| Security | Spring Security + OAuth2 Resource Server (JWT) + Keycloak |
| Async | Spring Events + `@Async`; sonra **Kafka** |
| Observability | Micrometer + Prometheus, OpenTelemetry |
| Test | JUnit 5, Testcontainers, REST Assured, ArchUnit |
| Build | Gradle (Kotlin DSL), Spotless, Jib |
| Multi-tenancy | MVP: `tenant_id` discriminator |

### 4.3. Domain model

**VO:** `Money(amount, currency)`, `UnLocode("DEHAM")`, `Equipment(type, size)`, `DateRange`, `TenantId`.

**Aggregates:**
- `Ratesheet` → `RateLine` → `RateSurcharge`, `FreeTimeRule`
- `Quote` → `QuoteLeg` (pre/main/on) → `QuoteCharge`
- `Booking` (state: DRAFT→REQUESTED→CONFIRMED→DOCS→SAILED→ARRIVED→DELIVERED→CLOSED)
- `Customer`, `Carrier`, `Port`

### 4.4. REST API (MVP)

| Method | Path | Təyinat |
|--------|------|---------|
| POST | `/api/v1/ratesheets/import` | Excel upload (multipart) |
| GET  | `/api/v1/ratesheets?carrier=&validOn=` | Siyahı |
| GET  | `/api/v1/ratesheets/{id}` | Detail |
| POST | `/api/v1/quotes` | Instant quote |
| GET  | `/api/v1/quotes/{id}` | Quote |
| POST | `/api/v1/bookings` | Quote → booking |
| PATCH| `/api/v1/bookings/{id}/status` | State transition |
| GET  | `/api/v1/ports?q=` | UN/LOCODE search |
| GET  | `/api/v1/carriers` | Carrier list |

### 4.5. PostgreSQL sxemi (əsas)

```
carrier(id, scac, name)
port(unloc PK, name, country, region, lat, lon)
service_contract(id, carrier_id, contract_no, type, customer_id, currency)
ratesheet(id, contract_id, tenant_id, source_file, valid_from, valid_to, status, version, uploaded_by, uploaded_at)
rate_line(id, ratesheet_id, pol, pod, via, service, equipment, commodity, base_amount, currency, transit_days, allocation_teu)
rate_surcharge(id, rate_line_id NULL, ratesheet_id NULL, code, name, amount, currency, basis, equipment_filter, mandatory)
free_time_rule(id, rate_line_id, side, combined, days)
rate_condition(id, rate_line_id, key, value)
quote(id, tenant_id, customer_id, status, valid_until, total_amount, currency)
quote_leg(id, quote_id, type[PRE|MAIN|ON], from_loc, to_loc, carrier_id, equipment, transit_days)
quote_charge(id, quote_leg_id, code, amount, currency, basis, source_rate_line_id)
booking(id, quote_id, carrier_booking_no, status, vessel, voyage, etd, eta)
customer(id, tenant_id, name, vat, payment_terms)
user_account(id, tenant_id, email, role)
```

Kritik index: `rate_line(pol, pod, equipment, valid_from, valid_to)`.

### 4.6. Excel ingestion strategiyası

Adapter pattern:
```
RatesheetParser (interface)
├── OneAetEbParser, OneIetParser, OneZfsParser
├── HmmGermanyParser, HmmSpecialParser
├── CoscoIetParser, CoscoOogParser, CoscoReeferParser
├── NcpeDryFakParser
└── OoclExportParser
```
Pipeline: `Upload → Tika → POI XSSF → Parser → CanonicalRatesheetDto → Validator → Persistence`. Hər import üçün `import_audit` JSON saxla.

**İlk addım:** POI əsaslı "schema sniffer" CLI — hər .xlsx-in sheet/header strukturunu JSON-a tökür ki, parser yazılışı asanlaşsın.

### 4.7. Carrier inteqrasiya skeleti

```java
interface CarrierBookingPort {
    BookingResponse submit(BookingRequest req);
    BookingStatus check(String carrierBookingNo);
}
interface CarrierTrackingPort { List<ShipmentEvent> events(String bookingNo); }
interface CarrierRatePort { List<RateLine> fetchRates(RateQuery q); }
```
Adapterlər: `MaerskApiAdapter`, `OneApiAdapter`, `InttraEdiAdapter`, `Project44Adapter`.

### 4.8. Auth & multi-tenancy

- Keycloak (realm: `bil`), OIDC; rollar: `ADMIN`, `OPS`, `SALES`, `CUSTOMER`.
- Tenant ID JWT claim → `TenantContext` ThreadLocal → Hibernate `@Filter` ilə `tenant_id = :current`.
- MVP: tək tenant + multiple `customer`.

---

## 5. Quotation alqoritmi (MVP)

**Input:** `pol, pod, equipment, commodity, readyDate, customerId, weightKg, dgClass?`

1. **Candidate seçimi:** `valid_from <= readyDate <= valid_to`, eyni POL/POD/equipment, customer-spesifik və ya FAK.
2. **Commodity match:** FAK fallback; HS exact; DG/OOG exclusion.
3. **Tie-breaker:** NAC > FAK; daha yeni `version`; aşağı `base_amount`.
4. **Surcharge toplama:** rate-line + ratesheet səviyyə; equipment filter; FX gunluk.
5. **Free time və conditions** quote-a əlavə.
6. **Pre/On-carriage (Faza 2/3):** address → port zone → trucking tariff.
7. **Markup:** customer pricing rule (% / flat per equipment).
8. **Total** customer default valyutasında.
9. **Transit:** `transit_days` + buffer.
10. **Quote saxla** (`valid_until = min(ratesheet.valid_to, now+7d)`).

```
QuoteService.quote(req):
  candidates = rateRepo.findCandidates(req)
  best       = ranker.pick(candidates, req)
  charges    = surchargeAggregator.aggregate(best, req)
  inland     = inlandService.priceLegs(req)         // Faza 2+
  total      = pricingEngine.apply(best, charges, inland, customerRules(req.customerId))
  return QuoteAssembler.build(best, charges, inland, total)
```

---

## 6. Dərhal görüləcək addımlar

1. **`build.gradle`** — Spring Boot **3.3.x** stable; PostgreSQL, JPA, Liquibase, POI 5.3+, MapStruct, Lombok, springdoc, Spring Security, Testcontainers.
2. **Gradle multi-module** struktura keç.
3. **Liquibase baseline** — `db.changelog-master.yaml` + `001-initial-schema.yaml`.
4. **UN/LOCODE seed** — UNECE açıq dataset → `port`.
5. **Excel "schema sniffer" CLI** — `docs/` qovluğunda hər .xlsx-i analiz edib `docs/_introspection/<file>.json` çıxarır.
6. **İlk parser:** `OneIetParser` → canonical DTO.
7. **Quotation MVP** `POST /api/v1/quotes` — yalnız ocean leg, hardcoded markup 10%.
8. **Admin upload** — başlanğıcda Swagger UI / Postman; sonra React/Next.js.
9. **CI/CD** — GitHub Actions: gradle build + Testcontainers + Jib docker.
10. **Auth skelet** — Keycloak docker-compose + Resource Server.

---

## 7. Risklər və açıq suallar

| # | Sual / Risk | Variantlar |
|---|-------------|-----------|
| 1 | Ratesheet formatları heterojendir | (A) Carrier başına dedicated parser ✅ / (B) LLM extraction / (C) Manual mapping UI |
| 2 | Multi-tenancy zəruridir? | (A) Tək tenant / (B) discriminator ✅ MVP / (C) schema-per-tenant |
| 3 | Spring Boot 4.0.3 stable deyil | **3.3.x stable**-a keç |
| 4 | Carrier API kontraktları | DCSA + Maersk/ONE açıq API ilə başla |
| 5 | OOG/REEFER atributları | Extension cədvəl və ya JSONB `attributes` |
| 6 | FX | Daxili FX cədvəli + ECB feed; quote anında snapshot |
| 7 | Hüquqi (GDPR, INCOTERMS, B/L) | Faza 4 hüquqşünas review |

---

## 8. Növbəti immediate aksiya

`docs/` qovluğundakı .xlsx fayllarının faktiki strukturunu (sheet adları, header sətirləri) çıxarmaq üçün introspection skripti `tools/excel_introspect.py` yaradılır və nəticə `docs/_introspection/*.json`-da saxlanılır. Bu nəticələr ilk Java parser-lərin yazılışında hardcoded test fixture kimi istifadə olunacaq.

---

## 9. Appendix — Faktiki Excel struktur tapıntıları (introspection nəticəsi)

`tools/excel_introspect.py` 10 .xlsx-i analiz etdi. Əsas tapıntılar:

### 9.1. Faylların real sheet strukturu

| Carrier / Fayl | Vərəq sayı | Diqqətəlayiq vərəqlər | Tipik header sətri |
|---|---|---|---|
| **ONE AET EB** | 6 | `COVER`, `AET EB - DRY (AEE, WEE, OEE)`, `TAD's - DRY` (terminal addons), `ONE Tariff AET EB + SURCHARGES`, `Freetimes`, `PUDO 1H 2026` | 1 / 3 / 9 |
| **HMM Special (CJ ICM)** | 1 | yalnız `OOG` | 2 |
| **HMM Standard (CJ ICM)** | 14 | `1. Intro`, `1.1 RDA`, `1.2 GMD`, `1.3 EFL`, `2. EES Surcharge`, `3. Main Ports / FIM / TW`, `4. Japan Outports`, `5. China / SE Asia`, `6. Empty Pick Up`, `7-9. Free Times DE/NL/BE`, `10. T&D Surcharges`, `11. Operation Charges` | 24 / 30 / 6 |
| **COSCO IET SB** | 4 | `IET VIP Southbound Main ports`, `…out ports`, `IET REEFER Southbound`, `TT overview` (transit time) | 11 / 6 |
| **COSCO Germany OOG** | 5 trade-region | `FE-OOG`, `ME-IPBC-OOG`, `SA East Coast-OOG`, `SA Westcoast-OOG`, `OCEANIA-OOG` | 8 |
| **COSCO Germany REEFER** | 5 | `Reefer FE`, `Reefer ME & IPBC`, `FE & ME+IPBC Outports`, `Reefer SA East Coast`, `Reefer SA Westcoast` | 8 |
| **ONE IET** | 5 | `COVER`, `IEA - DRY`, `Freetimes`, `ONE Tariff IET SB`, `PUDO 1H 2026` | 1 / 10 |
| **NCPE DE.NL.BE** | 1 | `FAK` | 6 |
| **OOCL Global** | 18 | `Introduction`, `Lookup`, `Rechenblatt neu` (calc-sheet), `Far East+Japan`, `Koper+Trieste`, `Intra Europe`, `Middle East`, `Africa`, `India+Pakistan`, `Australia & NZ`, `Special Equipment`, `Reefer`, `pick up+Inland Tariff`, `Surcharges-Terms`, `Restricted Prohibited Commodity`, `OOG booking check list` | 10 / 11 / 17 |
| **ONE ZFS** | 5 | `COVER`, `ZFS - DRY`, `Freetimes`, `ONE Tariff ZFS + SURCHARGES`, `PUDO 1H 2026` | 1 / 10 |

### 9.2. Plana təsir edən real tapıntılar

1. **ONE-un 3 ratesheet-i** (AET EB / IET / ZFS) **eyni şablonu** paylaşır → tək `OneOceanRatesheetParser` 3 sənədi də idarə edə bilər (sheet-name kontekstindən trade çıxarmaqla). Hər biri `COVER` (metadata) + DRY pricing sheet + `Freetimes` + `Tariff + SURCHARGES` + `PUDO 1H 2026` (pickup/dropoff inland tariff) ehtiva edir.
2. **PUDO sheet** (Pick-Up / Drop-Off) — ONE faylları artıq **inland trucking tarif məlumatını** daxil edir. Bu Faza 2-ni (pre-carriage) Faza 1-də qismən reallaşdırmağa imkan verir — eyni rate sheet-dən ocean + drayage çıxara bilərik.
3. **HMM Standard 14 vərəq** ən mürəkkəbidir: 4 trade region (Main/Japan/China/SEA), region başına ayrı surcharge sheet (`2. EES`, `10. T&D`, `11. Operation`), və 3 ölkə üçün ayrı free-time cədvəlləri. → Dedicated parser zəruridir.
4. **HMM Special yalnız OOG** — tək sheet, sadə parser. Ad uyğunsuzluğu: əvvəl "Special DRY" güman edilirdi, faktiki **OOG**-dur.
5. **COSCO faylları (IET, OOG, REEFER)** trade region başına bir sheet — `RatesheetParser` trade-region həssas dispatcher kimi yazılmalıdır.
6. **OOCL fayl 18 sheet** — production-level FAK tariff: `Lookup` cədvəlləri + `Rechenblatt` (Excel formula calc-sheet) + region başına pricing + `Special Equipment` + `Reefer` + `Surcharges-Terms`. Ən mürəkkəb halda da bütün məlumat `Lookup` və region sheet-lərindən bizim canonical modelə uyğunlaşdırıla bilər. **Special Equipment** sheet sütunu `XFC422`-yə qədər gedir — bu hidden/wide layout, parser üçün column-bound limit lazımdır.
7. **NCPE çox sadə** (tək `FAK` sheet, header row 6) → ilk parser kimi ideal pilot.
8. **TT overview / Freetimes / Surcharges** sheet-ləri demək olar bütün carrier-lərdə ayrı sheet kimi gəlir → bu sheet-lər `RatesheetParserSupport` ortaq utility-də işlənə bilər.

### 9.3. Yenilənmiş ilk parser sırası (pilot-dən mürəkkəbə doğru)

1. `NcpeFakParser` — tək sheet, sadə layout (pilot, end-to-end pipeline-ı tutmaq üçün)
2. `HmmSpecialOogParser` — tək OOG sheet
3. `OneOceanRatesheetParser` — AET EB / IET / ZFS (paylaşılan şablon, + PUDO inland data)
4. `CoscoTradeRegionParser` — IET SB / OOG / REEFER (multi-trade)
5. `HmmStandardParser` — 14-vərəqli kompleks
6. `OoclGlobalParser` — son, ən kompleks (18 vərəq, formula sheet-li)

### 9.4. Reproduce etmək üçün

```bash
pip install openpyxl
python3 tools/excel_introspect.py
# Nəticə: docs/_introspection/<file>.json + _summary.json
```

JSON-larda hər sheet üçün ilk 40 sətrin non-empty hüceyrələri var — bu Java parser-lərin unit testləri üçün fixture kimi birbaşa istifadə oluna bilər.
