INSERT INTO imobiliaria (id, razao_social, cnpj, slug, status, criado_por, alterado_por)
VALUES (
    '00000000-0000-7000-8000-000000000001',
    'Imobiliária Demo',
    '11444777000161',
    'imobiliaria-demo',
    'ATIVA',
    '00000000-0000-0000-0000-000000000000',
    '00000000-0000-0000-0000-000000000000'
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO imobiliaria_parametro (
    tenant_id, comissao_percentual_teto, orcamento_validade_dias_padrao,
    geocodificacao_tentativas_max, cep_cache_janela_dias, fuso_horario,
    criado_por, alterado_por
)
VALUES (
    '00000000-0000-7000-8000-000000000001',
    6.00,
    15,
    5,
    30,
    'America/Sao_Paulo',
    '00000000-0000-0000-0000-000000000000',
    '00000000-0000-0000-0000-000000000000'
)
ON CONFLICT (tenant_id) DO NOTHING;
