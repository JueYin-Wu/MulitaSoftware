package uy.edu.um.porytic2026.validation;

import org.junit.jupiter.api.Test;
import uy.edu.um.porytic2026.entities.DocumentType;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

// Tests unitarios de DocumentValidator: las reglas de formato que usan los registros (CU01 y CU03)
// y las reservas (edad mínima). No necesitan mocks porque son métodos estáticos sin base de datos.
class DocumentValidatorTest {

    // ===================== Cédula =====================

    @Test
    void ciWithTheRightCheckDigitIsValid() {
        // 1*2 + 2*9 + 3*8 + 4*7 + 5*6 + 6*3 + 7*4 = 148 -> le faltan 2 para 150 -> verificador 2
        assertTrue(DocumentValidator.isValidCI("1.234.567-2"));
        assertTrue(DocumentValidator.isValidCI("12345672"));
    }

    @Test
    void ciWithAWrongCheckDigitIsNotValid() {
        assertFalse(DocumentValidator.isValidCI("1.234.567-3"));
    }

    @Test
    void ciOfSevenDigitsIsCompletedWithAZeroOnTheLeft() {
        // 0*2 + 1*9 + 2*8 + 3*7 + 4*6 + 5*3 + 6*4 = 109 -> verificador 1
        assertTrue(DocumentValidator.isValidCI("123.456-1"));
        assertFalse(DocumentValidator.isValidCI("123.456-2"));
    }

    @Test
    void ciWithTooFewOrTooManyDigitsIsNotValid() {
        assertFalse(DocumentValidator.isValidCI("12345"));
        assertFalse(DocumentValidator.isValidCI("123456789"));
        assertFalse(DocumentValidator.isValidCI(null));
    }

    // ===================== RUT y pasaporte =====================

    @Test
    void rutNeedsTwelveDigits() {
        assertTrue(DocumentValidator.isValidRUT("21 123456 0018"));
        assertTrue(DocumentValidator.isValidRUT("221234560012"));
        assertFalse(DocumentValidator.isValidRUT("2112345600"));
        assertFalse(DocumentValidator.isValidRUT("000000000000"));
    }

    @Test
    void passportIsNormalizedAndNeedsAtLeastOneNumber() {
        assertEquals("AB1234567", DocumentValidator.normalizePassport("ab-123 4567"));
        assertTrue(DocumentValidator.isValidPassport("ab1234567"));
        assertFalse(DocumentValidator.isValidPassport("ABCDEFG"));  // sin números
        assertFalse(DocumentValidator.isValidPassport("A123"));     // muy corto
    }

    @Test
    void documentProblemDependsOnTheType() {
        assertNull(DocumentValidator.documentProblem(DocumentType.CI, "1.234.567-2"));
        assertNotNull(DocumentValidator.documentProblem(DocumentType.CI, "1.234.567-9"));
        assertEquals("Ingresá tu número de cédula", DocumentValidator.documentProblem(DocumentType.CI, " "));
        assertNull(DocumentValidator.documentProblem(DocumentType.PASSPORT, "AB1234567"));
        assertEquals("Ingresá tu número de pasaporte", DocumentValidator.documentProblem(DocumentType.PASSPORT, ""));
    }

    @Test
    void documentsAreSavedNormalized() {
        assertEquals("12345672", DocumentValidator.normalizeDocument(DocumentType.CI, "1.234.567-2"));
        assertEquals("AB1234567", DocumentValidator.normalizeDocument(DocumentType.PASSPORT, "ab 1234567"));
    }

    // ===================== Contacto y contraseña =====================

    @Test
    void uruguayanPhonesAreAccepted() {
        assertTrue(DocumentValidator.isValidPhone("099 123 456"));  // celular
        assertTrue(DocumentValidator.isValidPhone("2901 2345"));    // fijo de Montevideo
        assertTrue(DocumentValidator.isValidPhone("4222 1234"));    // fijo del interior
        assertFalse(DocumentValidator.isValidPhone("12345"));
        assertFalse(DocumentValidator.isValidPhone("3901 2345"));
    }

    @Test
    void emailsNeedADomainWithAnExtension() {
        assertTrue(DocumentValidator.isValidEmail("sofia.gomez@correo.com.uy"));
        assertFalse(DocumentValidator.isValidEmail("sofia@correo"));
        assertFalse(DocumentValidator.isValidEmail("sofia gomez@correo.com"));
        assertFalse(DocumentValidator.isValidEmail(null));
    }

    @Test
    void namesOnlyAcceptLetters() {
        assertTrue(DocumentValidator.isValidName("María José"));
        assertTrue(DocumentValidator.isValidName("O'Neill"));
        assertFalse(DocumentValidator.isValidName("Juan2"));
        assertFalse(DocumentValidator.isValidName(" "));
    }

    @Test
    void passwordNeedsUppercaseLowercaseAndASymbol() {
        assertNull(DocumentValidator.passwordProblem("Turista1234!"));
        assertEquals("La contraseña debe tener al menos 8 caracteres", DocumentValidator.passwordProblem("Ab!1"));
        assertTrue(DocumentValidator.passwordProblem("turista1234!").contains("una mayúscula"));
        assertTrue(DocumentValidator.passwordProblem("TURISTA1234!").contains("una minúscula"));
        assertTrue(DocumentValidator.passwordProblem("Turista1234").contains("un símbolo"));
    }

    // ===================== Fechas de nacimiento y edad =====================

    @Test
    void birthDateCannotBeEmptyInTheFutureOrTooOld() {
        LocalDate today = LocalDate.of(2026, 11, 16);
        assertNull(DocumentValidator.birthDateProblem("1999-05-10", today));
        assertNotNull(DocumentValidator.birthDateProblem("", today));
        assertNotNull(DocumentValidator.birthDateProblem("2027-01-01", today));
        assertNotNull(DocumentValidator.birthDateProblem("1890-01-01", today));
        assertNotNull(DocumentValidator.birthDateProblem("10/05/1999", today)); // formato equivocado
    }

    @Test
    void ageIsCalculatedOnTheDayOfTheActivity() {
        LocalDate birth = LocalDate.of(2014, 11, 20);
        assertEquals(11, DocumentValidator.ageOn(birth, LocalDate.of(2026, 11, 19)));
        assertEquals(12, DocumentValidator.ageOn(birth, LocalDate.of(2026, 11, 20))); // cumple ese día
    }
}
