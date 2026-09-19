package com.lifeos.healthcare.entity;

import com.lifeos.common.entity.BaseEntity;
import com.lifeos.dependent.entity.DependentEntity;
import com.lifeos.document.entity.DocumentEntity;
import com.lifeos.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "health_appointments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dependent_id")
    private DependentEntity dependent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private DocumentEntity primaryDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follow_up_to_id")
    private AppointmentEntity followUpTo;

    @Column(name = "doctor_name", nullable = false, length = 150)
    private String doctorName;

    @Column(name = "specialization", nullable = false, length = 100)
    private String specialization;

    @Column(name = "clinic_or_hospital", nullable = false, length = 200)
    private String clinicOrHospital;

    @Column(name = "clinic_phone", length = 30)
    private String clinicPhone;

    @Column(name = "clinic_address", length = 255)
    private String clinicAddress;

    @Column(name = "appointment_time", nullable = false)
    private OffsetDateTime appointmentTime;

    @Column(name = "scheduled_end_time")
    private OffsetDateTime scheduledEndTime;

    @Column(name = "time_zone", nullable = false, length = 50)
    @Builder.Default
    private String timeZone = "UTC";

    @Column(name = "purpose", nullable = false, length = 255)
    private String purpose;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    @Column(name = "reminder_offset_minutes", nullable = false)
    @Builder.Default
    private Integer reminderOffsetMinutes = 1440;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    @PrePersist
    @PreUpdate
    protected void ensureMetadata() {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
    }
}
