package br.com.rockgustavo.imobiliaria.shared.exception;

import org.springframework.http.HttpStatus;

public abstract class DomainException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String codigo;
    private final HttpStatus status;

    protected DomainException(String codigo, HttpStatus status, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
        this.status = status;
    }

    protected DomainException(String codigo, HttpStatus status, String mensagem, Throwable causa) {
        super(mensagem, causa);
        this.codigo = codigo;
        this.status = status;
    }

    public String getCodigo() {
        return codigo;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
