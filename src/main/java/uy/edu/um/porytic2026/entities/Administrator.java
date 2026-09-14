package uy.edu.um.porytic2026.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "administrators")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder

public class Administrator extends User {

}