-- Épico 00 — RN-00-06 a RN-00-10. Ver docs/modelo-de-dados.md.

CREATE TABLE imobiliaria (
    id              uuid PRIMARY KEY,
    razao_social    varchar(200) NOT NULL,
    cnpj            varchar(14) NOT NULL,
    slug            varchar(60) NOT NULL,
    status          varchar(20) NOT NULL DEFAULT 'ATIVA' CHECK (status IN ('ATIVA', 'SUSPENSA')),
    criado_em       timestamptz NOT NULL DEFAULT now(),
    criado_por      uuid NOT NULL,
    alterado_em     timestamptz NOT NULL DEFAULT now(),
    alterado_por    uuid NOT NULL,
    CONSTRAINT uq_imobiliaria_cnpj UNIQUE (cnpj),
    CONSTRAINT uq_imobiliaria_slug UNIQUE (slug)
);

CREATE TABLE imobiliaria_parametro (
    tenant_id                          uuid PRIMARY KEY REFERENCES imobiliaria (id),
    comissao_percentual_teto           numeric(5,2) NOT NULL DEFAULT 6.00 CHECK (comissao_percentual_teto > 0),
    orcamento_validade_dias_padrao     integer NOT NULL DEFAULT 15,
    geocodificacao_tentativas_max      smallint NOT NULL DEFAULT 5,
    cep_cache_janela_dias              integer NOT NULL DEFAULT 30,
    fuso_horario                       varchar(50) NOT NULL DEFAULT 'America/Sao_Paulo',
    criado_em                          timestamptz NOT NULL DEFAULT now(),
    criado_por                         uuid NOT NULL,
    alterado_em                        timestamptz NOT NULL DEFAULT now(),
    alterado_por                       uuid NOT NULL
);
