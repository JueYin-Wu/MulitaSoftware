package uy.edu.um.porytic2026.validation;

import uy.edu.um.porytic2026.entities.DocumentType;

// Reglas de formato de los datos de los formularios: cédula (con dígito verificador), RUT,
// teléfono, correo, nombres y contraseña. Las usan los registros y la recuperación de contraseña.
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

    // Contraseña: al menos 8 caracteres, una mayúscula, una minúscula y un símbolo (!"·$%&/()=? etc.).
    // Devuelve qué le falta, o null si cumple todo.
    public static String passwordProblem(String value) {
        if (value == null || value.length() < 8) {
            return "La contraseña debe tener al menos 8 caracteres";
        }
        java.util.List<String> missing = new java.util.ArrayList<>();
        if (!value.matches(".*\\p{Lu}.*")) {
            missing.add("una mayúscula");
        }
        if (!value.matches(".*\\p{Ll}.*")) {
            missing.add("una minúscula");
        }
        // Símbolo = cualquier carácter que no sea letra, número ni espacio
        if (!value.matches(".*[^\\p{L}\\p{N}\\s].*")) {
            missing.add("un símbolo, por ejemplo ! \" · $ % & / ( ) = ?");
        }
        if (missing.isEmpty()) {
            return null;
        }
        return "A la contraseña le falta " + String.join(", ", missing);
    }

    // Pasaporte: el formato cambia según el país, así que se acepta entre 6 y 12 letras o números,
    // con al menos un número. Se guarda en mayúsculas y sin espacios ni guiones.
    public static String normalizePassport(String value) {
        return value == null ? "" : value.replaceAll("[\\s-]", "").toUpperCase();
    }

    public static boolean isValidPassport(String value) {
        String passport = normalizePassport(value);
        return passport.matches("[A-Z0-9]{6,12}") && passport.matches(".*\\d.*");
    }

    // Documento de identidad según su tipo: devuelve qué está mal, o null si es válido
    public static String documentProblem(DocumentType type, String value) {
        boolean blank = value == null || value.isBlank();
        if (type == DocumentType.PASSPORT) {
            if (blank) {
                return "Ingresá tu número de pasaporte";
            }
            return isValidPassport(value) ? null
                    : "Pasaporte no válido. Debe tener entre 6 y 12 letras o números (ej: AB1234567)";
        }
        if (blank) {
            return "Ingresá tu número de cédula";
        }
        return isValidCI(value) ? null
                : "Cédula no válida. Revisá los números y el dígito verificador (ej: 1.234.567-2)";
    }

    // Deja el documento como se guarda en la base: la cédula solo con números, el pasaporte en mayúsculas
    public static String normalizeDocument(DocumentType type, String value) {
        return type == DocumentType.PASSPORT ? normalizePassport(value) : onlyDigits(value);
    }
}
