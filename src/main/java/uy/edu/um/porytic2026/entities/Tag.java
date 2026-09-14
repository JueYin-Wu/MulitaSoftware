package uy.edu.um.porytic2026.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    @NotBlank
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TagStatus status;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @ManyToOne
    @JoinColumn(name = "proposed_by")
    private Provider proposedBy;

    @Column(name = "proposal_date")
    private LocalDateTime proposalDate;

    @ManyToMany(mappedBy = "tags")
    private List<Activity> activities;
}