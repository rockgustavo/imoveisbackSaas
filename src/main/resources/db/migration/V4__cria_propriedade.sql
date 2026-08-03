-- Épico 03 — RN-03-01 a RN-03-11. Ver docs/modelo-de-dados.md.
-- Colunas geo_* pertencem à lógica do Épico 04 (RN-04-*), mas nascem aqui
-- porque o modelo define a tabela inteira de uma vez.

CREATE TABLE propriedade (
    id                 uuid PRIMARY KEY,
    tenant_id          uuid NOT NULL REFERENCES imobiliaria (id),
    proprietario_id    uuid NOT NULL REFERENCES pessoa (id),
    tipo               varchar(20) NOT NULL CHECK (tipo IN ('APARTAMENTO', 'CASA', 'TERRENO', 'COMERCIAL')),
    area_privativa     numeric(10, 2),
    quartos            smallint,
    vagas              smallint,
    valor_referencia   numeric(15, 2) NOT NULL,
    situacao           varchar(20) NOT NULL DEFAULT 'DISPONIVEL'
                       CHECK (situacao IN ('DISPONIVEL', 'AGENCIADA', 'RESERVADA', 'VENDIDA', 'RETIRADA')),
    cep                varchar(8) NOT NULL,
    logradouro         varchar(200) NOT NULL,
    numero             varchar(20) NOT NULL,
    complemento        varchar(100),
    bairro             varchar(100) NOT NULL,
    localidade         varchar(100) NOT NULL,
    uf                 varchar(2) NOT NULL,
    endereco_validado  boolean NOT NULL DEFAULT true,
    latitude           numeric(9, 6),
    longitude          numeric(9, 6),
    geo_situacao       varchar(20) NOT NULL DEFAULT 'PENDENTE' CHECK (geo_situacao IN ('PENDENTE', 'CONCLUIDA', 'MANUAL')),
    geo_tentativas     smallint NOT NULL DEFAULT 0,
    criado_em          timestamptz NOT NULL DEFAULT now(),
    criado_por         uuid NOT NULL,
    alterado_em        timestamptz NOT NULL DEFAULT now(),
    alterado_por       uuid NOT NULL,
    CONSTRAINT ck_propriedade_latitude CHECK (latitude IS NULL OR latitude BETWEEN -33.75 AND 5.27),
    CONSTRAINT ck_propriedade_longitude CHECK (longitude IS NULL OR longitude BETWEEN -73.99 AND -34.79),
    CONSTRAINT ck_propriedade_geo_par CHECK ((latitude IS NULL) = (longitude IS NULL))
);

CREATE INDEX idx_propriedade_tenant ON propriedade (tenant_id);
CREATE INDEX idx_propriedade_proprietario ON propriedade (tenant_id, proprietario_id);
CREATE INDEX idx_propriedade_situacao ON propriedade (tenant_id, situacao);
CREATE INDEX idx_propriedade_localidade ON propriedade (tenant_id, localidade, uf);
CREATE INDEX idx_propriedade_geo ON propriedade (tenant_id, latitude, longitude) WHERE geo_situacao = 'CONCLUIDA';
