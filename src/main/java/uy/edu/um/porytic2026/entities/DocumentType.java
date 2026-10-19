package uy.edu.um.porytic2026.entities;

// Tipo de documento de identidad de una persona: cédula uruguaya o pasaporte (MER: "Cédula / Pasaporte")
public enum DocumentType {

    CI("Cédula"),
    PASSPORT("Pasaporte");

    private final String label;

    DocumentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    // Convierte el valor que llega del formulario ("CI" o "PASSPORT"). Devuelve null si no es ninguno.
    public static DocumentType fromValue(String value) {
        for (DocumentType type : values()) {
            if (type.name().equals(value)) {
                return type;
            }
        }
        return null;
    }
}
