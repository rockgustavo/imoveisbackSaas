-- Épico 01 — RN-01-01 a RN-01-10. Ver docs/modelo-de-dados.md.

CREATE TABLE pessoa (
    id              uuid PRIMARY KEY,
    tenant_id       uuid NOT NULL REFERENCES imobiliaria (id),
    tipo_documento  varchar(4) NOT NULL CHECK (tipo_documento IN ('CPF', 'CNPJ')),
    documento       varchar(14) NOT NULL,
    nome            varchar(200) NOT NULL,
    email           varchar(200),
    subject_idp     varchar(100),
    ativo           boolean NOT NULL DEFAULT true,
    criado_em       timestamptz NOT NULL DEFAULT now(),
    criado_por      uuid NOT NULL,
    alterado_em     timestamptz NOT NULL DEFAULT now(),
    alterado_por    uuid NOT NULL,
    CONSTRAINT uq_pessoa_documento UNIQUE (tenant_id, tipo_documento, documento)
);

CREATE INDEX idx_pessoa_tenant ON pessoa (tenant_id);
CREATE UNIQUE INDEX uq_pessoa_email ON pessoa (tenant_id, email) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX uq_pessoa_subject_idp ON pessoa (tenant_id, subject_idp) WHERE subject_idp IS NOT NULL;

CREATE TABLE pessoa_papel (
    id          uuid PRIMARY KEY,
    tenant_id   uuid NOT NULL REFERENCES imobiliaria (id),
    pessoa_id   uuid NOT NULL REFERENCES pessoa (id),
    papel       varchar(20) NOT NULL CHECK (papel IN ('PROPRIETARIO', 'USUARIO', 'ADMINISTRADOR')),
    criado_em   timestamptz NOT NULL DEFAULT now(),
    criado_por  uuid NOT NULL,
    CONSTRAINT uq_pessoa_papel UNIQUE (tenant_id, pessoa_id, papel)
);

CREATE INDEX idx_pessoa_papel_tenant_pessoa ON pessoa_papel (tenant_id, pessoa_id);
CREATE INDEX idx_pessoa_papel_administrador ON pessoa_papel (tenant_id, papel) WHERE papel = 'ADMINISTRADOR';
