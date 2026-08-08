-- Épico 09 — RN-09-01/02. Log de transição de status entre propriedade, orçamento e contrato.
-- entidade_id sem FK: referencia linha em módulo variável (ver docs/modelo-de-dados.md).

CREATE TABLE historico_transicao (
    id               uuid PRIMARY KEY,
    tenant_id        uuid NOT NULL REFERENCES imobiliaria (id),
    entidade_tipo    varchar(20) NOT NULL CHECK (entidade_tipo IN ('PROPRIEDADE', 'ORCAMENTO', 'CONTRATO')),
    entidade_id      uuid NOT NULL,
    status_anterior  varchar(20),
    status_novo      varchar(20) NOT NULL,
    autor            uuid NOT NULL,
    ocorrido_em      timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_historico_transicao_entidade ON historico_transicao (tenant_id, entidade_tipo, entidade_id, ocorrido_em);
