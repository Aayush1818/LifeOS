package com.lifeos.healthcare.dto;

import com.lifeos.healthcare.entity.AppointmentEntity;
import com.lifeos.healthcare.entity.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponse {

    public static final String NON_DIAGNOSTIC_DISCLAIMER = 
            "Strictly organizational & non-diagnostic. LifeOS does not provide medical diagnosis, clinical evaluation, or treatment advice.";

    private UUID id;
    private String doctorName;
    private String specialization;
    private String clinicOrHospital;
    private String clinicPhone;
    private String clinicAddress;
    private OffsetDateTime appointmentTime;
    private OffsetDateTime scheduledEndTime;
    private String timeZone;
    private String purpose;
    private String notes;
    private AppointmentStatus status;

    private UUID dependentId;
    private String dependentName;

    private UUID primaryDocumentId;
    private String primaryDocumentTitle;

    private UUID followUpToId;
    private Integer reminderOffsetMinutes;

    @Builder.Default
    private List<MedicalDocumentLinkResponse> linkedDocuments = new ArrayList<>();

    private Map<String, Object> metadata;

    @Builder.Default
    private String disclaimer = NON_DIAGNOSTIC_DISCLAIMER;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static AppointmentResponse fromEntity(AppointmentEntity entity, List<MedicalDocumentLinkResponse> linkedDocuments) {
        return AppointmentResponse.builder()
                .id(entity.getId())
                .doctorName(entity.getDoctorName())
                .specialization(entity.getSpecialization())
                .clinicOrHospital(entity.getClinicOrHospital())
                .clinicPhone(entity.getClinicPhone())
                .clinicAddress(entity.getClinicAddress())
                .appointmentTime(entity.getAppointmentTime())
                .scheduledEndTime(entity.getScheduledEndTime())
                .timeZone(entity.getTimeZone())
                .purpose(entity.getPurpose())
                .notes(entity.getNotes())
                .status(entity.getStatus())
                .dependentId(entity.getDependent() != null ? entity.getDependent().getId() : null)
                .dependentName(entity.getDependent() != null ? entity.getDependent().getFullName() : null)
                .primaryDocumentId(entity.getPrimaryDocument() != null ? entity.getPrimaryDocument().getId() : null)
                .primaryDocumentTitle(entity.getPrimaryDocument() != null ? entity.getPrimaryDocument().getTitle() : null)
                .followUpToId(entity.getFollowUpTo() != null ? entity.getFollowUpTo().getId() : null)
                .reminderOffsetMinutes(entity.getReminderOffsetMinutes())
                .linkedDocuments(linkedDocuments != null ? linkedDocuments : new ArrayList<>())
                .metadata(entity.getMetadata())
                .disclaimer(NON_DIAGNOSTIC_DISCLAIMER)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static AppointmentResponse fromEntity(AppointmentEntity entity) {
        return fromEntity(entity, new ArrayList<>());
    }
}
