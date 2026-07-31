package br.com.rockgustavo.imobiliaria.shared.validation;

import java.util.concurrent.atomic.AtomicInteger;

public final class CnpjTestFixture {

    private static final AtomicInteger SEQUENCIA = new AtomicInteger(1);
    private static final int[] PESOS_DV1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_DV2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private CnpjTestFixture() {
    }

    public static String novoCnpjValido() {
        String base = "%012d".formatted(SEQUENCIA.getAndIncrement());
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
