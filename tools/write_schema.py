#!/usr/bin/env python3
"""Writes the Liquibase initial schema YAML."""
from pathlib import Path

CONTENT = """databaseChangeLog:
  - changeSet:
      id: 001-initial-schema
      author: bil
      changes:
        - createTable:
            tableName: carrier
            columns:
              - column:
                  name: id
                  type: BIGINT
                  autoIncrement: true
                  constraints:
                    primaryKey: true
                    nullable: false
              - column:
                  name: scac
                  type: VARCHAR(8)
                  constraints:
                    nullable: false
                    unique: true
              - column:
                  name: name
                  type: VARCHAR(128)
                  constraints:
                    nullable: false

        - createTable:
            tableName: port
            columns:
              - column:
                  name: unloc
                  type: VARCHAR(5)
                  constraints:
                    primaryKey: true
                    nullable: false
              - column:
                  name: name
                  type: VARCHAR(128)
                  constraints:
                    nullable: false
              - column: { name: country, type: VARCHAR(64) }
              - column: { name: region,  type: VARCHAR(64) }
              - column: { name: lat,     type: DOUBLE }
              - column: { name: lon,     type: DOUBLE }

        - createTable:
            tableName: ratesheet
            columns:
              - column:
                  name: id
                  type: BIGINT
                  autoIncrement: true
                  constraints:
                    primaryKey: true
                    nullable: false
              - column:
                  name: carrier_id
                  type: BIGINT
                  constraints:
                    nullable: false
                    foreignKeyName: fk_ratesheet_carrier
                    references: carrier(id)
              - column:
                  name: source
                  type: VARCHAR(64)
                  constraints:
                    nullable: false
              - column: { name: source_file, type: VARCHAR(512) }
              - column: { name: contract_no, type: VARCHAR(64) }
              - column:
                  name: type
                  type: VARCHAR(16)
                  constraints:
                    nullable: false
              - column:
                  name: currency
                  type: VARCHAR(3)
                  constraints:
                    nullable: false
              - column:
                  name: valid_from
                  type: DATE
                  constraints:
                    nullable: false
              - column:
                  name: valid_to
                  type: DATE
                  constraints:
                    nullable: false
              - column:
                  name: status
                  type: VARCHAR(16)
                  constraints:
                    nullable: false
              - column: { name: version, type: VARCHAR(32) }
              - column:
                  name: uploaded_at
                  type: TIMESTAMP
                  constraints:
                    nullable: false
              - column:
                  name: tenant_id
                  type: VARCHAR(64)
                  defaultValue: default

        - createIndex:
            tableName: ratesheet
            indexName: ix_ratesheet_validity
            columns:
              - column: { name: valid_from }
              - column: { name: valid_to }

        - createTable:
            tableName: rate_line
            columns:
              - column:
                  name: id
                  type: BIGINT
                  autoIncrement: true
                  constraints:
                    primaryKey: true
                    nullable: false
              - column:
                  name: ratesheet_id
                  type: BIGINT
                  constraints:
                    nullable: false
                    foreignKeyName: fk_rateline_ratesheet
                    references: ratesheet(id)
              - column:
                  name: pol
                  type: VARCHAR(5)
                  constraints:
                    nullable: false
              - column:
                  name: pod
                  type: VARCHAR(5)
                  constraints:
                    nullable: false
              - column: { name: via,     type: VARCHAR(64) }
              - column: { name: service, type: VARCHAR(64) }
              - column:
                  name: equipment
                  type: VARCHAR(16)
                  constraints:
                    nullable: false
              - column:
                  name: commodity
                  type: VARCHAR(64)
                  defaultValue: FAK
              - column:
                  name: base_amount
                  type: "DECIMAL(12,2)"
                  constraints:
                    nullable: false
              - column:
                  name: currency
                  type: VARCHAR(3)
                  constraints:
                    nullable: false
              - column: { name: transit_days,   type: INT }
              - column: { name: allocation_teu, type: INT }

        - createIndex:
            tableName: rate_line
            indexName: ix_rateline_lookup
            columns:
              - column: { name: pol }
              - column: { name: pod }
              - column: { name: equipment }

        - createTable:
            tableName: import_audit
            columns:
              - column:
                  name: id
                  type: BIGINT
                  autoIncrement: true
                  constraints:
                    primaryKey: true
                    nullable: false
              - column:
                  name: source_file
                  type: VARCHAR(512)
                  constraints:
                    nullable: false
              - column: { name: parser,         type: VARCHAR(128) }
              - column: { name: ratesheet_id,   type: BIGINT }
              - column: { name: lines_imported, type: INT }
              - column:
                  name: status
                  type: VARCHAR(16)
                  constraints:
                    nullable: false
              - column: { name: message, type: VARCHAR(2000) }
              - column:
                  name: imported_at
                  type: TIMESTAMP
                  constraints:
                    nullable: false
"""

target = Path("/Users/shukurrzayev/Documents/bil/bil/src/main/resources/db/changelog/changes/001-initial-schema.yaml")
target.write_text(CONTENT)
print(f"wrote {len(CONTENT.splitlines())} lines to {target}")

