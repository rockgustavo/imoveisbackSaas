package br.com.rockgustavo.imobiliaria.propriedade;

import br.com.rockgustavo.imobiliaria.propriedade.domain.Endereco;
import br.com.rockgustavo.imobiliaria.propriedade.domain.Propriedade;
import br.com.rockgustavo.imobiliaria.propriedade.domain.PropriedadeTestBuilder;
import br.com.rockgustavo.imobiliaria.propriedade.domain.SituacaoPropriedade;
import br.com.rockgustavo.imobiliaria.propriedade.infra.PropriedadeRepository;
import br.com.rockgustavo.imobiliaria.shared.tenant.TenantContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MapaTestFixture {

    private static final BigDecimal VALOR_REFERENCIA_PADRAO = new BigDecimal("450000.00");

    private final PropriedadeRepository propriedadeRepository;

    public MapaTestFixture(PropriedadeRepository propriedadeRepository) {
        this.propriedadeRepository = propriedadeRepository;
    }

    public UUID criarGeocodificada(UUID tenantId, UUID proprietarioId, BigDecimal latitude, BigDecimal longitude) {
        return criarGeocodificada(tenantId, proprietarioId, latitude, longitude, SituacaoPropriedade.AGENCIADA,
                PropriedadeTestBuilder.enderecoPadrao(), VALOR_REFERENCIA_PADRAO);
    }

    public UUID criarGeocodificada(UUID tenantId, UUID proprietarioId, BigDecimal latitude, BigDecimal longitude,
                                    SituacaoPropriedade situacao) {
        return criarGeocodificada(tenantId, proprietarioId, latitude, longitude, situacao,
                PropriedadeTestBuilder.enderecoPadrao(), VALOR_REFERENCIA_PADRAO);
    }

    public UUID criarGeocodificada(UUID tenantId, UUID proprietarioId, BigDecimal latitude, BigDecimal longitude,
                                    SituacaoPropriedade situacao, Endereco endereco, BigDecimal valorReferencia) {
        TenantContext.definir(tenantId);
        try {
            Propriedade propriedade = PropriedadeTestBuilder.umaPropriedade()
                    .comProprietarioId(proprietarioId)
                    .comEndereco(endereco)
                    .comValorReferencia(valorReferencia)
                    .build();
            propriedade.concluirGeolocalizacao(latitude, longitude);
            transicionarPara(propriedade, situacao);
            propriedadeRepository.save(propriedade);
            return propriedade.getId();
        } finally {
            TenantContext.limpar();
        }
    }

    public void geocodificarExistente(UUID tenantId, UUID propriedadeId, BigDecimal latitude, BigDecimal longitude) {
        TenantContext.definir(tenantId);
        try {
            Propriedade propriedade = propriedadeRepository.buscarPorId(propriedadeId).orElseThrow();
            propriedade.concluirGeolocalizacao(latitude, longitude);
            propriedadeRepository.save(propriedade);
        } finally {
            TenantContext.limpar();
        }
    }

    public List<UUID> criarVariasGeocodificadas(UUID tenantId, UUID proprietarioId, int quantidade,
                                                 BigDecimal latitude, BigDecimal longitude) {
        TenantContext.definir(tenantId);
        try {
            List<Propriedade> propriedades = new ArrayList<>(quantidade);
            for (int i = 0; i < quantidade; i++) {
                Propriedade propriedade = PropriedadeTestBuilder.umaPropriedade()
                        .comProprietarioId(proprietarioId)
                        .build();
                propriedade.concluirGeolocalizacao(latitude, longitude);
                propriedade.agenciar();
                propriedades.add(propriedade);
            }
            propriedadeRepository.saveAll(propriedades);
            return propriedades.stream().map(Propriedade::getId).toList();
        } finally {
            TenantContext.limpar();
        }
    }

    private static void transicionarPara(Propriedade propriedade, SituacaoPropriedade situacao) {
        switch (situacao) {
            case DISPONIVEL -> {
            }
            case AGENCIADA -> propriedade.agenciar();
            case RESERVADA -> {
                propriedade.agenciar();
                propriedade.reservar();
            }
            case VENDIDA -> {
                propriedade.agenciar();
                propriedade.reservar();
                propriedade.vender();
            }
            case RETIRADA -> propriedade.retirar();
        }
    }
}
