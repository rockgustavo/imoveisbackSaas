package br.com.rockgustavo.imobiliaria.propriedade.application;

import br.com.rockgustavo.imobiliaria.imobiliaria.ImobiliariaFacade;
import br.com.rockgustavo.imobiliaria.propriedade.domain.Endereco;
import br.com.rockgustavo.imobiliaria.propriedade.domain.GeoSituacao;
import br.com.rockgustavo.imobiliaria.propriedade.domain.Propriedade;
import br.com.rockgustavo.imobiliaria.propriedade.infra.PropriedadeRepository;
import br.com.rockgustavo.imobiliaria.shared.geo.Coordenada;
import br.com.rockgustavo.imobiliaria.shared.geo.EnderecoParaGeocodificar;
import br.com.rockgustavo.imobiliaria.shared.geo.GeocodificacaoClient;
import br.com.rockgustavo.imobiliaria.shared.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
public class GeocodificacaoService {

    private static final Logger log = LoggerFactory.getLogger(GeocodificacaoService.class);

    private final PropriedadeRepository propriedadeRepository;
    private final GeocodificacaoClient geocodificacaoClient;
    private final ImobiliariaFacade imobiliariaFacade;

    public GeocodificacaoService(PropriedadeRepository propriedadeRepository, GeocodificacaoClient geocodificacaoClient,
                                  ImobiliariaFacade imobiliariaFacade) {
        this.propriedadeRepository = propriedadeRepository;
        this.geocodificacaoClient = geocodificacaoClient;
        this.imobiliariaFacade = imobiliariaFacade;
    }

    @Transactional
    public void tentarGeocodificar(UUID propriedadeId) {
        Propriedade propriedade = propriedadeRepository.buscarPorId(propriedadeId).orElse(null);
        if (propriedade == null || propriedade.getGeoSituacao() != GeoSituacao.PENDENTE) {
            return;
        }
        Optional<Coordenada> coordenada = geocodificacaoClient.geocodificar(paraEnderecoDeGeocodificacao(propriedade))
                .filter(Coordenada::dentroDoTerritorioNacional);
        if (coordenada.isPresent()) {
            propriedade.concluirGeolocalizacao(coordenada.get().latitude(), coordenada.get().longitude());
            log.info("RN-04-02: geolocalização concluída para propriedade {}", propriedadeId);
        } else {
            short tentativasMax = imobiliariaFacade.geocodificacaoTentativasMax(TenantContext.obter());
            propriedade.registrarTentativaFalhaGeo(tentativasMax);
            log.info("RN-04-04/05: tentativa de geolocalização sem sucesso para propriedade {} ({}/{})",
                    propriedadeId, propriedade.getGeoTentativas(), tentativasMax);
        }
    }

    private static EnderecoParaGeocodificar paraEnderecoDeGeocodificacao(Propriedade propriedade) {
        Endereco endereco = propriedade.getEndereco();
        return new EnderecoParaGeocodificar(
                endereco.getCep(), endereco.getLogradouro(), endereco.getNumero(), endereco.getBairro(),
                endereco.getLocalidade(), endereco.getUf());
    }
}
