-- Épico 09 — RN-09-03, ADR-09. Snapshot temporal do agregado Contrato.

CREATE TABLE contrato_historico (
    id           uuid PRIMARY KEY,
    tenant_id    uuid NOT NULL REFERENCES imobiliaria (id),
    contrato_id  uuid NOT NULL REFERENCES contrato (id),
    versao       integer NOT NULL,
    snapshot     jsonb NOT NULL,
    autor        uuid NOT NULL,
    ocorrido_em  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_contrato_historico_versao UNIQUE (contrato_id, versao)
);

CREATE INDEX idx_contrato_historico_contrato ON contrato_historico (tenant_id, contrato_id, ocorrido_em);
