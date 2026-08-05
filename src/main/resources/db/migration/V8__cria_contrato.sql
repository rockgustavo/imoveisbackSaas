-- Épico 06 — RN-06-01 a RN-06-13. Ver docs/modelo-de-dados.md e ADR-03/ADR-17.

CREATE TABLE contrato (
    id                          uuid PRIMARY KEY,
    tenant_id                   uuid NOT NULL REFERENCES imobiliaria (id),
    pessoa_id                   uuid NOT NULL REFERENCES pessoa (id),
    orcamento_origem_id         uuid NOT NULL REFERENCES orcamento (id),
    status                      varchar(20) NOT NULL DEFAULT 'RASCUNHO'
                                CHECK (status IN ('RASCUNHO', 'ATIVO', 'ENCERRADO', 'CANCELADO', 'EXPIRADO')),
    vigencia_inicio             date NOT NULL,
    vigencia_fim                date NOT NULL,
    vigencia                    daterange GENERATED ALWAYS AS (daterange(vigencia_inicio, vigencia_fim, '[]')) STORED,
    regras_contratuais          text NOT NULL,
    justificativa_encerramento  text,
    criado_em                   timestamptz NOT NULL DEFAULT now(),
    criado_por                  uuid NOT NULL,
    alterado_em                 timestamptz NOT NULL DEFAULT now(),
    alterado_por                uuid NOT NULL,
    CONSTRAINT uq_contrato_orcamento_origem UNIQUE (orcamento_origem_id),
    CONSTRAINT ck_contrato_vigencia CHECK (vigencia_fim > vigencia_inicio),
    CONSTRAINT ck_contrato_justificativa_encerramento
        CHECK (status <> 'ENCERRADO' OR justificativa_encerramento IS NOT NULL)
);

CREATE INDEX idx_contrato_pessoa ON contrato (tenant_id, pessoa_id);
CREATE INDEX idx_contrato_status ON contrato (tenant_id, status);
CREATE INDEX idx_contrato_status_vigencia_fim ON contrato (tenant_id, status, vigencia_fim);

CREATE TABLE agenciamento (
    id                        uuid PRIMARY KEY,
    tenant_id                 uuid NOT NULL REFERENCES imobiliaria (id),
    contrato_id               uuid NOT NULL REFERENCES contrato (id),
    propriedade_id            uuid NOT NULL REFERENCES propriedade (id),
    comissao_percentual       numeric(5, 2) NOT NULL CHECK (comissao_percentual > 0),
    valor_pedido               numeric(15, 2) NOT NULL CHECK (valor_pedido > 0),
    contrato_vigencia_inicio  date NOT NULL,
    contrato_vigencia_fim     date NOT NULL,
    contrato_vigencia         daterange GENERATED ALWAYS AS
                              (daterange(contrato_vigencia_inicio, contrato_vigencia_fim, '[]')) STORED,
    contrato_ativo            boolean NOT NULL DEFAULT false,
    criado_em                 timestamptz NOT NULL DEFAULT now(),
    criado_por                uuid NOT NULL,
    alterado_em               timestamptz NOT NULL DEFAULT now(),
    alterado_por              uuid NOT NULL,
    CONSTRAINT uq_agenciamento_propriedade UNIQUE (contrato_id, propriedade_id),
    CONSTRAINT agenciamento_vigencia_excl EXCLUDE USING gist (
        tenant_id WITH =,
        propriedade_id WITH =,
        contrato_vigencia WITH &&
    ) WHERE (contrato_ativo)
);

CREATE INDEX idx_agenciamento_contrato ON agenciamento (tenant_id, contrato_id);
CREATE INDEX idx_agenciamento_propriedade ON agenciamento (tenant_id, propriedade_id);

CREATE TABLE aditivo (
    id             uuid PRIMARY KEY,
    tenant_id      uuid NOT NULL REFERENCES imobiliaria (id),
    contrato_id    uuid NOT NULL REFERENCES contrato (id),
    propriedade_id uuid NOT NULL REFERENCES propriedade (id),
    tipo           varchar(20) NOT NULL CHECK (tipo IN ('INCLUSAO', 'EXCLUSAO')),
    justificativa  text NOT NULL,
    data           date NOT NULL DEFAULT CURRENT_DATE,
    criado_por     uuid NOT NULL,
    criado_em      timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_aditivo_contrato ON aditivo (tenant_id, contrato_id);
