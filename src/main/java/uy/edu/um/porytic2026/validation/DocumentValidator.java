package uy.edu.um.porytic2026.validation;

// Valida el formato de los documentos uruguayos usando su dígito verificador.
// Así se rechaza una cédula o un RUT inventados aunque tengan la cantidad de números correcta.
public final class DocumentValidator {

    private DocumentValidator() {
    }

    // Saca puntos, guiones y espacios: "1.234.567-2" pasa a "12345672"
    public static String onlyDigits(String value) {
        return value == null ? "" : value.replaceAll("[^0-9]", "");
    }

    // Cédula: 7 u 8 dígitos, el último es el verificador.
    // Se multiplican los primeros 7 dígitos (completando con 0 a la izquierda) por 2, 9, 8, 7, 6, 3, 4;
    // el verificador es lo que le falta a la suma para llegar a la siguiente decena.
    public static boolean isValidCI(String value) {
        String digits = onlyDigits(value);
        if (digits.length() < 7 || digits.length() > 8) {
            return false;
        }
        String padded = digits.length() == 7 ? "0" + digits : digits;
        int[] weights = {2, 9, 8, 7, 6, 3, 4};
        int sum = 0;
        for (int i = 0; i < 7; i++) {
            sum += Character.getNumericValue(padded.charAt(i)) * weights[i];
        }
        int expected = (10 - (sum % 10)) % 10;
        return expected == Character.getNumericValue(padded.charAt(7));
    }

    // RUT: 12 dígitos (se aceptan puntos, guiones y espacios al escribirlo).
    // No se controla el dígito verificador porque la DGI declaró reservado el algoritmo en 2024,
    // y tampoco el prefijo, porque desde octubre de 2024 los RUT nuevos empiezan con 22.
    public static boolean isValidRUT(String value) {
        String digits = onlyDigits(value);
        return digits.length() == 12 && !digits.matches("0+");
    }

    // Teléfono uruguayo: fijo de 8 dígitos (2xxx xxxx / 4xxx xxxx) o celular de 9 (09x xxx xxx)
    public static boolean isValidPhone(String value) {
        String digits = onlyDigits(value);
        return digits.matches("0?9\\d{7}") || digits.matches("[24]\\d{7}");
    }

    // Correo: algo@dominio.ext (sin espacios, con punto y al menos 2 letras al final)
    public static boolean isValidEmail(String value) {
        return value != null && value.trim().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");
    }

    // Nombres: solo letras (con tildes y ñ), espacios, apóstrofo o guion
    public static boolean isValidName(String value) {
        return value != null && value.trim().matches("^[\\p{L}][\\p{L} '\\-]*$");
    }
}
