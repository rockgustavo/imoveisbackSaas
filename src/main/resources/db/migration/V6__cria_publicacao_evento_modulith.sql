-- Registro de eventos do Spring Modulith (ADR-04/ADR-15) — schema oficial do
-- módulo spring-modulith-events-jdbc 1.2.5 para PostgreSQL. Schema criado pelo
-- Flyway, não pelo Modulith (spring.modulith.events.jdbc.schema-initialization
-- fica no default false — CLAUDE.md §6, Flyway é dono de todo schema).

CREATE TABLE event_publication (
    id                uuid NOT NULL,
    listener_id       text NOT NULL,
    event_type        text NOT NULL,
    serialized_event  text NOT NULL,
    publication_date  timestamptz NOT NULL,
    completion_date   timestamptz,
    PRIMARY KEY (id)
);

CREATE INDEX event_publication_serialized_event_hash_idx ON event_publication USING hash (serialized_event);
CREATE INDEX event_publication_by_completion_date_idx ON event_publication (completion_date);
