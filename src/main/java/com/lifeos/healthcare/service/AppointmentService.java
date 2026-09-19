package com.lifeos.healthcare.service;

import com.lifeos.common.exception.ResourceNotFoundException;
import com.lifeos.dependent.entity.DependentEntity;
import com.lifeos.dependent.repository.DependentRepository;
import com.lifeos.document.entity.DocumentEntity;
import com.lifeos.document.entity.DocumentEntityLinkEntity;
import com.lifeos.document.repository.DocumentEntityLinkRepository;
import com.lifeos.document.repository.DocumentRepository;
import com.lifeos.healthcare.dto.*;
import com.lifeos.healthcare.entity.AppointmentEntity;
import com.lifeos.healthcare.entity.AppointmentStatus;
import com.lifeos.healthcare.repository.AppointmentRepository;
import com.lifeos.reminder.entity.ReminderEntity;
import com.lifeos.reminder.entity.ReminderStatus;
import com.lifeos.reminder.repository.ReminderRepository;
import com.lifeos.user.entity.UserEntity;
import com.lifeos.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentService {

    public static final String REMINDER_TYPE_HEALTH_APPOINTMENT = "HEALTH_APPOINTMENT";
    public static final String ENTITY_TYPE_HEALTH_APPOINTMENT = "HEALTH_APPOINTMENT";

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final DependentRepository dependentRepository;
    private final DocumentRepository documentRepository;
    private final DocumentEntityLinkRepository documentEntityLinkRepository;
    private final ReminderRepository reminderRepository;

    @Transactional
    public AppointmentResponse createAppointment(CreateAppointmentRequest request, UUID userId) {
        UserEntity user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (request.getScheduledEndTime() != null && !request.getScheduledEndTime().isAfter(request.getAppointmentTime())) {
            throw new IllegalArgumentException("Scheduled end time must be after appointment start time");
        }

        DependentEntity dependent = null;
        if (request.getDependentId() != null) {
            dependent = dependentRepository.findByIdAndUserIdAndIsDeletedFalse(request.getDependentId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Dependent not found with id: " + request.getDependentId()));
        }

        DocumentEntity primaryDoc = null;
        if (request.getPrimaryDocumentId() != null) {
            primaryDoc = documentRepository.findByIdAndUserIdAndIsDeletedFalse(request.getPrimaryDocumentId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + request.getPrimaryDocumentId()));
        }

        AppointmentEntity followUpTo = null;
        if (request.getFollowUpToId() != null) {
            followUpTo = appointmentRepository.findByIdAndUserIdAndIsDeletedFalse(request.getFollowUpToId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Follow-up appointment not found with id: " + request.getFollowUpToId()));
        }

        int reminderOffset = request.getReminderOffsetMinutes() != null ? request.getReminderOffsetMinutes() : 1440;
        String timeZone = request.getTimeZone() != null && !request.getTimeZone().isBlank() ? request.getTimeZone().trim() : "UTC";
        Map<String, Object> metadata = request.getMetadata() != null ? request.getMetadata() : new java.util.HashMap<>();

        AppointmentEntity appointment = AppointmentEntity.builder()
                .user(user)
                .dependent(dependent)
                .primaryDocument(primaryDoc)
                .followUpTo(followUpTo)
                .doctorName(request.getDoctorName().trim())
                .specialization(request.getSpecialization().trim())
                .clinicOrHospital(request.getClinicOrHospital().trim())
                .clinicPhone(request.getClinicPhone())
                .clinicAddress(request.getClinicAddress())
                .appointmentTime(request.getAppointmentTime())
                .scheduledEndTime(request.getScheduledEndTime())
                .timeZone(timeZone)
                .purpose(request.getPurpose().trim())
                .notes(request.getNotes())
                .status(AppointmentStatus.SCHEDULED)
                .reminderOffsetMinutes(reminderOffset)
                .metadata(metadata)
                .build();

        AppointmentEntity saved = appointmentRepository.save(appointment);

        // Synchronize reminder
        syncAppointmentReminder(saved, user);

        log.info("User [{}] created healthcare appointment [{}] with Dr. {} on {}",
                userId, saved.getId(), saved.getDoctorName(), saved.getAppointmentTime());

        return AppointmentResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getAppointment(UUID id, UUID userId) {
        AppointmentEntity appointment = appointmentRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Healthcare appointment not found with id: " + id));

        List<MedicalDocumentLinkResponse> linkedDocs = getLinkedDocuments(appointment.getId());
        return AppointmentResponse.fromEntity(appointment, linkedDocs);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> listAppointments(
            UUID userId, UUID dependentId, AppointmentStatus status, 
            LocalDate startDate, LocalDate endDate, String doctorName, Pageable pageable) {

        Specification<AppointmentEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user").get("id"), userId));
            predicates.add(cb.isFalse(root.get("isDeleted")));

            if (dependentId != null) {
                predicates.add(cb.equal(root.get("dependent").get("id"), dependentId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("appointmentTime"), startDate.atStartOfDay().atOffset(ZoneOffset.UTC)));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("appointmentTime"), endDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)));
            }
            if (doctorName != null && !doctorName.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("doctorName")), "%" + doctorName.toLowerCase().trim() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return appointmentRepository.findAll(spec, pageable).map(this::mapWithLinkedDocs);
    }

    @Transactional
    public AppointmentResponse updateAppointment(UUID id, UpdateAppointmentRequest request, UUID userId) {
        AppointmentEntity appointment = appointmentRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Healthcare appointment not found with id: " + id));

        if (request.getDoctorName() != null && !request.getDoctorName().isBlank()) {
            appointment.setDoctorName(request.getDoctorName().trim());
        }
        if (request.getSpecialization() != null && !request.getSpecialization().isBlank()) {
            appointment.setSpecialization(request.getSpecialization().trim());
        }
        if (request.getClinicOrHospital() != null && !request.getClinicOrHospital().isBlank()) {
            appointment.setClinicOrHospital(request.getClinicOrHospital().trim());
        }
        if (request.getClinicPhone() != null) {
            appointment.setClinicPhone(request.getClinicPhone());
        }
        if (request.getClinicAddress() != null) {
            appointment.setClinicAddress(request.getClinicAddress());
        }
        if (request.getPurpose() != null && !request.getPurpose().isBlank()) {
            appointment.setPurpose(request.getPurpose().trim());
        }
        if (request.getNotes() != null) {
            appointment.setNotes(request.getNotes());
        }
        if (request.getPrimaryDocumentId() != null) {
            DocumentEntity doc = documentRepository.findByIdAndUserIdAndIsDeletedFalse(request.getPrimaryDocumentId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + request.getPrimaryDocumentId()));
            appointment.setPrimaryDocument(doc);
        }
        if (request.getMetadata() != null) {
            appointment.setMetadata(request.getMetadata());
        }

        AppointmentEntity updated = appointmentRepository.save(appointment);
        syncAppointmentReminder(updated, updated.getUser());

        log.info("User [{}] updated appointment [{}]", userId, id);
        return mapWithLinkedDocs(updated);
    }

    @Transactional
    public AppointmentResponse rescheduleAppointment(UUID id, RescheduleAppointmentRequest request, UUID userId) {
        AppointmentEntity appointment = appointmentRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Healthcare appointment not found with id: " + id));

        if (request.getNewScheduledEndTime() != null && !request.getNewScheduledEndTime().isAfter(request.getNewAppointmentTime())) {
            throw new IllegalArgumentException("Scheduled end time must be after appointment start time");
        }

        appointment.setAppointmentTime(request.getNewAppointmentTime());
        if (request.getNewScheduledEndTime() != null) {
            appointment.setScheduledEndTime(request.getNewScheduledEndTime());
        }
        if (request.getTimeZone() != null && !request.getTimeZone().isBlank()) {
            appointment.setTimeZone(request.getTimeZone().trim());
        }
        if (request.getReminderOffsetMinutes() != null) {
            appointment.setReminderOffsetMinutes(request.getReminderOffsetMinutes());
        }
        if (request.getNotes() != null) {
            appointment.setNotes(request.getNotes());
        }
        appointment.setStatus(AppointmentStatus.RESCHEDULED);

        AppointmentEntity updated = appointmentRepository.save(appointment);
        syncAppointmentReminder(updated, updated.getUser());

        log.info("User [{}] rescheduled appointment [{}] to {}", userId, id, request.getNewAppointmentTime());
        return mapWithLinkedDocs(updated);
    }

    @Transactional
    public AppointmentResponse updateStatus(UUID id, UpdateStatusRequest request, UUID userId) {
        AppointmentEntity appointment = appointmentRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Healthcare appointment not found with id: " + id));

        appointment.setStatus(request.getStatus());
        if (request.getNotes() != null) {
            appointment.setNotes(request.getNotes());
        }

        AppointmentEntity updated = appointmentRepository.save(appointment);

        // Update or dismiss reminder depending on status
        if (request.getStatus() == AppointmentStatus.COMPLETED 
                || request.getStatus() == AppointmentStatus.CANCELLED 
                || request.getStatus() == AppointmentStatus.NO_SHOW) {
            dismissAppointmentReminder(updated.getId());
        } else if (request.getStatus() == AppointmentStatus.SCHEDULED || request.getStatus() == AppointmentStatus.RESCHEDULED) {
            syncAppointmentReminder(updated, updated.getUser());
        }

        log.info("User [{}] updated appointment [{}] status to {}", userId, id, request.getStatus());
        return mapWithLinkedDocs(updated);
    }

    @Transactional
    public void deleteAppointment(UUID id, UUID userId) {
        AppointmentEntity appointment = appointmentRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Healthcare appointment not found with id: " + id));

        appointment.setDeleted(true);
        appointmentRepository.save(appointment);

        dismissAppointmentReminder(id);
        log.info("User [{}] soft-deleted healthcare appointment [{}]", userId, id);
    }

    @Transactional(readOnly = true)
    public UpcomingAppointmentsResponse getUpcomingAppointments(UUID userId, int windowDays, UUID dependentId) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime cutoff = now.plusDays(windowDays);

        List<AppointmentEntity> upcoming = appointmentRepository
                .findAllByUserIdAndStatusAndAppointmentTimeBetweenAndIsDeletedFalse(
                        userId, AppointmentStatus.SCHEDULED, now, cutoff);

        if (dependentId != null) {
            upcoming = upcoming.stream()
                    .filter(a -> a.getDependent() != null && a.getDependent().getId().equals(dependentId))
                    .toList();
        }

        List<AppointmentResponse> dtos = upcoming.stream()
                .map(this::mapWithLinkedDocs)
                .toList();

        return UpcomingAppointmentsResponse.builder()
                .windowDays(windowDays)
                .upcomingCount(dtos.size())
                .appointments(dtos)
                .build();
    }

    // --- Helper Methods ---

    private AppointmentResponse mapWithLinkedDocs(AppointmentEntity entity) {
        List<MedicalDocumentLinkResponse> linkedDocs = getLinkedDocuments(entity.getId());
        return AppointmentResponse.fromEntity(entity, linkedDocs);
    }

    private List<MedicalDocumentLinkResponse> getLinkedDocuments(UUID appointmentId) {
        return documentEntityLinkRepository.findAllByEntityTypeAndEntityId(ENTITY_TYPE_HEALTH_APPOINTMENT, appointmentId)
                .stream()
                .map(link -> MedicalDocumentLinkResponse.fromEntity(link.getDocument()))
                .toList();
    }

    private void syncAppointmentReminder(AppointmentEntity appointment, UserEntity user) {
        // Only schedule active reminders for future scheduled/rescheduled appointments
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED && appointment.getStatus() != AppointmentStatus.RESCHEDULED) {
            return;
        }

        int offsetMinutes = appointment.getReminderOffsetMinutes() != null ? appointment.getReminderOffsetMinutes() : 1440;
        OffsetDateTime dueAt = appointment.getAppointmentTime().minusMinutes(offsetMinutes);

        String title = "Doctor Appointment: Dr. " + appointment.getDoctorName() + " (" + appointment.getSpecialization() + ")";
        String patient = appointment.getDependent() != null ? appointment.getDependent().getFullName() : "Self";
        String desc = "Patient: " + patient + " | Clinic: " + appointment.getClinicOrHospital() 
                + " | Scheduled for " + appointment.getAppointmentTime();

        Optional<ReminderEntity> existing = reminderRepository
                .findByTargetEntityIdAndReminderTypeAndIsDeletedFalse(appointment.getId(), REMINDER_TYPE_HEALTH_APPOINTMENT);

        ReminderEntity reminder;
        if (existing.isPresent()) {
            reminder = existing.get();
            reminder.setTitle(title);
            reminder.setDescription(desc);
            reminder.setDueAt(dueAt);
            reminder.setStatus(ReminderStatus.ACTIVE);
        } else {
            reminder = ReminderEntity.builder()
                    .user(user)
                    .title(title)
                    .description(desc)
                    .dueAt(dueAt)
                    .reminderType(REMINDER_TYPE_HEALTH_APPOINTMENT)
                    .status(ReminderStatus.ACTIVE)
                    .targetEntityId(appointment.getId())
                    .build();
        }

        reminderRepository.save(reminder);
        log.debug("Synchronized appointment reminder for appointment [{}] with due date {}", appointment.getId(), dueAt);
    }

    private void dismissAppointmentReminder(UUID appointmentId) {
        reminderRepository.findByTargetEntityIdAndReminderTypeAndIsDeletedFalse(appointmentId, REMINDER_TYPE_HEALTH_APPOINTMENT)
                .ifPresent(reminder -> {
                    reminder.setStatus(ReminderStatus.DISMISSED);
                    reminderRepository.save(reminder);
                    log.debug("Dismissed appointment reminder for appointment [{}]", appointmentId);
                });
    }
}
