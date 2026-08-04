-- Épico 05 — RN-05-01 a RN-05-09. Ver docs/modelo-de-dados.md.

CREATE TABLE orcamento (
    id             uuid PRIMARY KEY,
    tenant_id      uuid NOT NULL REFERENCES imobiliaria (id),
    pessoa_id      uuid NOT NULL REFERENCES pessoa (id),
    status         varchar(20) NOT NULL DEFAULT 'RASCUNHO'
                   CHECK (status IN ('RASCUNHO', 'ENVIADO', 'ACEITO', 'RECUSADO', 'EXPIRADO')),
    validade       date NOT NULL,
    origem_id      uuid REFERENCES orcamento (id),
    versao         integer NOT NULL DEFAULT 1,
    criado_em      timestamptz NOT NULL DEFAULT now(),
    criado_por     uuid NOT NULL,
    alterado_em    timestamptz NOT NULL DEFAULT now(),
    alterado_por   uuid NOT NULL,
    CONSTRAINT ck_orcamento_origem_diferente CHECK (origem_id IS NULL OR origem_id <> id)
);

CREATE INDEX idx_orcamento_pessoa ON orcamento (tenant_id, pessoa_id);
CREATE INDEX idx_orcamento_status_validade ON orcamento (tenant_id, status, validade);
CREATE INDEX idx_orcamento_origem ON orcamento (tenant_id, origem_id);

CREATE TABLE orcamento_item (
    id                   uuid PRIMARY KEY,
    tenant_id            uuid NOT NULL REFERENCES imobiliaria (id),
    orcamento_id         uuid NOT NULL REFERENCES orcamento (id),
    propriedade_id       uuid NOT NULL REFERENCES propriedade (id),
    comissao_percentual  numeric(5, 2) NOT NULL CHECK (comissao_percentual > 0),
    valor_pedido         numeric(15, 2) NOT NULL CHECK (valor_pedido > 0),
    CONSTRAINT uq_orcamento_item_propriedade UNIQUE (orcamento_id, propriedade_id)
);

CREATE INDEX idx_orcamento_item_orcamento ON orcamento_item (tenant_id, orcamento_id);
CREATE INDEX idx_orcamento_item_propriedade ON orcamento_item (tenant_id, propriedade_id);
