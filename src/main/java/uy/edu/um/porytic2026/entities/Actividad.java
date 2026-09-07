package uy.edu.um.porytic2026.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Id;

public class Actividad {

    @Id
    private long activityId;

    @Column(name = "nombre")
    private String activityName;

    @Column(name = "fecha")
    private String activityDate;

    @Column(name = "precio")
    private long activityPrice;

    @Column(name = "requisitos")
    private String activityRequirements;

    @Column(name = "descripción")
    private String activityDescription;

    private int activityCapacity;

    private boolean activityCancelable;  //Si es cancelable o no
    private long activityCancelTime; //Cantidad de minutas antes de que empiece

    private boolean activityRefundable; //Si la actividad es reembolsable o no

    private int activityRefundPercent; //Porcentaje a reembolsar

    private String activityTags; // En realidad llama a una entidad "Etiquetas" para definir los valores
}
