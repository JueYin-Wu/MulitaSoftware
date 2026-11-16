package uy.edu.um.porytic2026.entities;

// Tipos de sanción que un administrador puede aplicarle a un prestador
public enum SanctionType {

    WARNING("Advertencia"),      // queda registrada y el prestador la ve en su portal, pero sigue operando
    SUSPENSION("Suspensión");    // no puede iniciar sesión y sus actividades no se muestran hasta que se levante

    private final String label;

    SanctionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
