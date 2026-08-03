-- Épico 04 — RN-04-03. Ver docs/modelo-de-dados.md.
-- Sem tenant_id: dado público compartilhado entre tenants (ADR-06).

CREATE TABLE cep_cache (
    cep            char(8) PRIMARY KEY,
    logradouro     varchar(200),
    bairro         varchar(100),
    localidade     varchar(100),
    uf             char(2),
    latitude       numeric(9, 6),
    longitude      numeric(9, 6),
    encontrado     boolean NOT NULL,
    consultado_em  timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_cep_cache_consultado_em ON cep_cache (consultado_em);
