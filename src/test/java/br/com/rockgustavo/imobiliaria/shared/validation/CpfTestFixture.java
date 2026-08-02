package br.com.rockgustavo.imobiliaria.shared.validation;

import java.util.concurrent.atomic.AtomicInteger;

public final class CpfTestFixture {

    private static final AtomicInteger SEQUENCIA = new AtomicInteger(1);
    private static final int[] PESOS_DV1 = {10, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_DV2 = {11, 10, 9, 8, 7, 6, 5, 4, 3, 2};

    private CpfTestFixture() {
    }

    public static String novoCpfValido() {
        String base = "%09d".formatted(SEQUENCIA.getAndIncrement());
        int dv1 = digitoVerificador(base, PESOS_DV1);
        int dv2 = digitoVerificador(base + dv1, PESOS_DV2);
        return base + dv1 + dv2;
    }

    private static int digitoVerificador(String numero, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += Character.getNumericValue(numero.charAt(i)) * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
