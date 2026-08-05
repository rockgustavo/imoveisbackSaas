package br.com.rockgustavo.imobiliaria;

import br.com.rockgustavo.imobiliaria.shared.validation.CnpjTestFixture;
import br.com.rockgustavo.imobiliaria.shared.validation.CpfTestFixture;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static br.com.rockgustavo.imobiliaria.AutenticacaoTestFixture.administradorDoTenant;
import static br.com.rockgustavo.imobiliaria.AutenticacaoTestFixture.plataformaAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ApiTestFixture {

    public static final ZoneId FUSO_PADRAO_DO_TENANT = ZoneId.of("America/Sao_Paulo");

    private final MockMvc mockMvc;

    public ApiTestFixture(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    public static LocalDate hojeNoFusoDoTenant() {
        return LocalDate.now(FUSO_PADRAO_DO_TENANT);
    }

    public UUID criarTenant() throws Exception {
        return criarTenant("Corretora Teste");
    }

    public UUID criarTenant(String razaoSocial) throws Exception {
        String corpo = """
                {"razaoSocial":"%s","cnpj":"%s","slug":"corretora-%s"}
                """.formatted(razaoSocial, CnpjTestFixture.novoCnpjValido(), UUID.randomUUID());
        return idCriadoEm(post("/api/v1/plataforma/imobiliarias")
                .with(plataformaAdmin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    public UUID criarPessoa(UUID tenantId, String documento, String nome) throws Exception {
        String corpo = """
                {"tipoDocumento":"CPF","documento":"%s","nome":"%s"}
                """.formatted(documento, nome);
        return idCriadoEm(post("/api/v1/pessoas")
                .with(administradorDoTenant(tenantId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    public UUID criarProprietario(UUID tenantId) throws Exception {
        UUID pessoaId = criarPessoa(tenantId, CpfTestFixture.novoCpfValido(), "Proprietário Teste");
        atribuirPapel(tenantId, pessoaId, """
                {"papel":"PROPRIETARIO"}
                """);
        return pessoaId;
    }

    public UUID criarUsuario(UUID tenantId, String email) throws Exception {
        UUID pessoaId = criarPessoa(tenantId, CpfTestFixture.novoCpfValido(), "Usuário Comum");
        atribuirPapel(tenantId, pessoaId, """
                {"papel":"USUARIO","email":"%s"}
                """.formatted(email));
        return pessoaId;
    }

    public void atribuirPapel(UUID tenantId, UUID pessoaId, String corpo) throws Exception {
        mockMvc.perform(post("/api/v1/pessoas/{id}/papeis", pessoaId)
                        .with(administradorDoTenant(tenantId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isOk());
    }

    public static String corpoPropriedade(UUID proprietarioId) {
        return """
                {"proprietarioId":"%s","tipo":"APARTAMENTO","areaPrivativa":85.50,"quartos":3,"vagas":1,
                 "valorReferencia":450000.00,"cep":"01310100","logradouro":"Av. Paulista","numero":"1000",
                 "bairro":"Bela Vista","localidade":"São Paulo","uf":"SP","enderecoValidado":true}
                """.formatted(proprietarioId);
    }

    public UUID criarPropriedade(UUID tenantId, UUID proprietarioId) throws Exception {
        return idCriadoEm(post("/api/v1/propriedades")
                .with(administradorDoTenant(tenantId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoPropriedade(proprietarioId)));
    }

    public static String corpoOrcamento(UUID pessoaId, UUID propriedadeId, String comissaoPercentual) {
        return """
                {"pessoaId":"%s","itens":[{"propriedadeId":"%s","comissaoPercentual":%s,"valorPedido":450000.00}]}
                """.formatted(pessoaId, propriedadeId, comissaoPercentual);
    }

    public UUID criarOrcamento(UUID tenantId, UUID pessoaId, UUID propriedadeId) throws Exception {
        return criarOrcamento(tenantId, pessoaId, propriedadeId, "5.00");
    }

    public UUID criarOrcamento(UUID tenantId, UUID pessoaId, UUID propriedadeId, String comissaoPercentual)
            throws Exception {
        return idCriadoEm(post("/api/v1/orcamentos")
                .with(administradorDoTenant(tenantId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoOrcamento(pessoaId, propriedadeId, comissaoPercentual)));
    }

    public UUID criarOrcamentoAceito(UUID tenantId, UUID pessoaId, UUID propriedadeId) throws Exception {
        UUID orcamentoId = criarOrcamento(tenantId, pessoaId, propriedadeId);
        aceitar(tenantId, orcamentoId);
        return orcamentoId;
    }

    public void aceitar(UUID tenantId, UUID orcamentoId) throws Exception {
        mockMvc.perform(post("/api/v1/orcamentos/{id}/envio", orcamentoId).with(administradorDoTenant(tenantId)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/orcamentos/{id}/aceite", orcamentoId).with(administradorDoTenant(tenantId)))
                .andExpect(status().isOk());
    }

    public static String corpoContrato(UUID orcamentoId, LocalDate inicio, LocalDate fim) {
        return """
                {"orcamentoId":"%s","vigenciaInicio":"%s","vigenciaFim":"%s","regrasContratuais":"regras de teste"}
                """.formatted(orcamentoId, inicio, fim);
    }

    public UUID criarContrato(UUID tenantId, UUID orcamentoId) throws Exception {
        return criarContrato(tenantId, orcamentoId, hojeNoFusoDoTenant(), hojeNoFusoDoTenant().plusYears(1));
    }

    public UUID criarContrato(UUID tenantId, UUID orcamentoId, LocalDate inicio, LocalDate fim) throws Exception {
        return idCriadoEm(post("/api/v1/contratos")
                .with(administradorDoTenant(tenantId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoContrato(orcamentoId, inicio, fim)));
    }

    public UUID criarContratoAtivo(UUID tenantId, UUID pessoaId, UUID propriedadeId) throws Exception {
        UUID contratoId = criarContrato(tenantId, criarOrcamentoAceito(tenantId, pessoaId, propriedadeId));
        ativar(tenantId, contratoId);
        return contratoId;
    }

    public void ativar(UUID tenantId, UUID contratoId) throws Exception {
        mockMvc.perform(post("/api/v1/contratos/{id}/ativacao", contratoId).with(administradorDoTenant(tenantId)))
                .andExpect(status().isOk());
    }

    private UUID idCriadoEm(org.springframework.test.web.servlet.RequestBuilder requisicao) throws Exception {
        ResultActions resultado = mockMvc.perform(requisicao);
        String location = resultado.andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
    }
}
