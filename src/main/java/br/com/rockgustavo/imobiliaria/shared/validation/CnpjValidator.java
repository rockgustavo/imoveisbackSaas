package br.com.rockgustavo.imobiliaria.shared.validation;

public final class CnpjValidator {

    private static final int[] PESOS_DV1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_DV2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private CnpjValidator() {
    }

    public static boolean valido(String cnpj) {
        if (cnpj == null || !cnpj.matches("\\d{14}") || todosDigitosIguais(cnpj)) {
            return false;
        }
        int dv1 = digitoVerificador(cnpj, PESOS_DV1);
        int dv2 = digitoVerificador(cnpj.substring(0, 12) + dv1, PESOS_DV2);
        return dv1 == Character.getNumericValue(cnpj.charAt(12))
                && dv2 == Character.getNumericValue(cnpj.charAt(13));
    }

    private static boolean todosDigitosIguais(String cnpj) {
        return cnpj.chars().distinct().count() == 1;
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
