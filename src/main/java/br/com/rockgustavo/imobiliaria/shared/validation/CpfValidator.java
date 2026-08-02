package br.com.rockgustavo.imobiliaria.shared.validation;

public final class CpfValidator {

    private static final int[] PESOS_DV1 = {10, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_DV2 = {11, 10, 9, 8, 7, 6, 5, 4, 3, 2};

    private CpfValidator() {
    }

    public static boolean valido(String cpf) {
        if (cpf == null || !cpf.matches("\\d{11}") || todosDigitosIguais(cpf)) {
            return false;
        }
        int dv1 = digitoVerificador(cpf, PESOS_DV1);
        int dv2 = digitoVerificador(cpf.substring(0, 9) + dv1, PESOS_DV2);
        return dv1 == Character.getNumericValue(cpf.charAt(9))
                && dv2 == Character.getNumericValue(cpf.charAt(10));
    }

    private static boolean todosDigitosIguais(String cpf) {
        return cpf.chars().distinct().count() == 1;
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
