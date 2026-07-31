package br.com.rockgustavo.imobiliaria.shared;

import com.github.f4b6a3.uuid.UuidCreator;

import java.util.UUID;

public final class IdGenerator {

    private IdGenerator() {
    }

    public static UUID novoId() {
        return UuidCreator.getTimeOrderedEpoch();
    }
}
