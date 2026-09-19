package com.lifeos.dependent.entity;

import com.lifeos.common.entity.BaseEntity;
import com.lifeos.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "dependents")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DependentEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship", nullable = false, length = 50)
    private RelationshipType relationship;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "emergency_phone", length = 30)
    private String emergencyPhone;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "medical_notes", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> medicalNotes = new HashMap<>();
}
