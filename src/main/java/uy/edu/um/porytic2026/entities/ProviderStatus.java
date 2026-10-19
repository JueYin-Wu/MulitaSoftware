package uy.edu.um.porytic2026.entities;

public enum ProviderStatus {

    PENDING,   // se registró y espera la revisión del administrador
    APPROVED,  // el administrador lo aceptó: puede iniciar sesión y publicar actividades
    REJECTED   // el administrador lo rechazó (con un motivo opcional)

}
