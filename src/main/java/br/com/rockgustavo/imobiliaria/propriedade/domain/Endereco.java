package br.com.rockgustavo.imobiliaria.propriedade.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class Endereco {

    @Column(nullable = false, length = 8)
    private String cep;

    @Column(nullable = false, length = 200)
    private String logradouro;

    @Column(nullable = false, length = 20)
    private String numero;

    @Column(length = 100)
    private String complemento;

    @Column(nullable = false, length = 100)
    private String bairro;

    @Column(nullable = false, length = 100)
    private String localidade;

    @Column(nullable = false, length = 2)
    private String uf;

    @Column(name = "endereco_validado", nullable = false)
    private boolean validado;

    protected Endereco() {
    }

    public Endereco(String cep, String logradouro, String numero, String complemento, String bairro,
                     String localidade, String uf, boolean validado) {
        this.cep = validarCep(cep);
        this.logradouro = exigirNaoVazio(logradouro, "logradouro");
        this.numero = exigirNaoVazio(numero, "numero");
        this.complemento = complemento;
        this.bairro = exigirNaoVazio(bairro, "bairro");
        this.localidade = exigirNaoVazio(localidade, "localidade");
        this.uf = validarUf(uf);
        this.validado = validado;
    }

    private static String exigirNaoVazio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " é obrigatório");
        }
        return valor;
    }

    private static String validarCep(String cep) {
        String digitos = cep == null ? null : cep.replaceAll("\\D", "");
        if (digitos == null || digitos.length() != 8) {
            throw new IllegalArgumentException("cep deve ter 8 dígitos");
        }
        return digitos;
    }

    private static String validarUf(String uf) {
        if (uf == null || !uf.matches("(?i)[A-Z]{2}")) {
            throw new IllegalArgumentException("uf deve ter 2 letras");
        }
        return uf.toUpperCase();
    }

    public String getCep() {
        return cep;
    }

    public String getLogradouro() {
        return logradouro;
    }

    public String getNumero() {
        return numero;
    }

    public String getComplemento() {
        return complemento;
    }

    public String getBairro() {
        return bairro;
    }

    public String getLocalidade() {
        return localidade;
    }

    public String getUf() {
        return uf;
    }

    public boolean isValidado() {
        return validado;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Endereco endereco)) {
            return false;
        }
        return validado == endereco.validado
                && Objects.equals(cep, endereco.cep)
                && Objects.equals(logradouro, endereco.logradouro)
                && Objects.equals(numero, endereco.numero)
                && Objects.equals(complemento, endereco.complemento)
                && Objects.equals(bairro, endereco.bairro)
                && Objects.equals(localidade, endereco.localidade)
                && Objects.equals(uf, endereco.uf);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cep, logradouro, numero, complemento, bairro, localidade, uf, validado);
    }
}
