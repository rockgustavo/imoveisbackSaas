package br.com.rockgustavo.imobiliaria;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

    @Test
    void verificaFronteirasDeModulo() {
        ApplicationModules modules = ApplicationModules.of(ImobiliariaApplication.class);
        modules.verify();
    }
}
