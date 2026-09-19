package com.lifeos.healthcare;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.auth.dto.AuthResponse;
import com.lifeos.auth.dto.RegisterRequest;
import com.lifeos.auth.service.AuthService;
import com.lifeos.dependent.dto.CreateDependentRequest;
import com.lifeos.dependent.dto.DependentResponse;
import com.lifeos.dependent.entity.RelationshipType;
import com.lifeos.dependent.service.DependentService;
import com.lifeos.document.dto.UploadDocumentRequest;
import com.lifeos.document.entity.DocumentCategory;
import com.lifeos.document.entity.DocumentType;
import com.lifeos.healthcare.dto.CreateAppointmentRequest;
import com.lifeos.healthcare.dto.RescheduleAppointmentRequest;
import com.lifeos.healthcare.dto.UpdateAppointmentRequest;
import com.lifeos.healthcare.dto.UpdateStatusRequest;
import com.lifeos.healthcare.entity.AppointmentStatus;
import com.lifeos.reminder.entity.ReminderEntity;
import com.lifeos.reminder.entity.ReminderStatus;
import com.lifeos.reminder.repository.ReminderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class HealthcareControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private DependentService dependentService;

    @Autowired
    private ReminderRepository reminderRepository;

    private String userAToken;
    private UUID userAId;
    private String userBToken;
    private UUID userBId;
    private UUID dependentAId;
    private UUID dependentBId;

    private static final byte[] VALID_PDF_BYTES = ("%PDF-1.4\n" +
            "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n" +
            "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n" +
            "3 0 obj << /Type /Page /Parent 2 0 R /Resources <<>> /Contents 4 0 R >> endobj\n" +
            "4 0 obj << /Length 55 >> stream\n" +
            "BT /F1 12 Tf 100 700 Td (Doctor Consultation Medical Report) Tj ET\n" +
            "endstream endobj\n" +
            "xref\n0 5\n0000000000 65535 f \n" +
            "trailer << /Size 5 /Root 1 0 R >>\n" +
            "startxref\n300\n%%EOF").getBytes(StandardCharsets.UTF_8);

    @BeforeEach
    void setUp() {
        // Register User A
        RegisterRequest reqA = RegisterRequest.builder()
                .email("hc.userA." + UUID.randomUUID() + "@example.com")
                .password("Password123!@#")
                .firstName("HealthUserA")
                .lastName("Verified")
                .build();
        AuthResponse authA = authService.register(reqA);
        userAToken = authA.getAccessToken();
        userAId = authA.getUser().getId();

        // Register User B
        RegisterRequest reqB = RegisterRequest.builder()
                .email("hc.userB." + UUID.randomUUID() + "@example.com")
                .password("Password123!@#")
                .firstName("HealthUserB")
                .lastName("Verified")
                .build();
        AuthResponse authB = authService.register(reqB);
        userBToken = authB.getAccessToken();
        userBId = authB.getUser().getId();

        // Dependent for User A
        CreateDependentRequest depReqA = CreateDependentRequest.builder()
                .fullName("Emma Verified")
                .relationship(RelationshipType.CHILD)
                .dateOfBirth(LocalDate.of(2018, 5, 10))
                .emergencyPhone("+15551234567")
                .build();
        DependentResponse depA = dependentService.createDependent(userAId, depReqA);
        dependentAId = depA.getId();

        // Dependent for User B
        CreateDependentRequest depReqB = CreateDependentRequest.builder()
                .fullName("Noah Verified")
                .relationship(RelationshipType.CHILD)
                .dateOfBirth(LocalDate.of(2020, 3, 15))
                .build();
        DependentResponse depB = dependentService.createDependent(userBId, depReqB);
        dependentBId = depB.getId();
    }

    private UUID uploadDocument(String token, String title, DocumentType type) throws Exception {
        UploadDocumentRequest meta = UploadDocumentRequest.builder()
                .title(title)
                .category(DocumentCategory.MEDICAL)
                .documentType(type)
                .build();

        MockMultipartFile metaPart = new MockMultipartFile(
                "metadata", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(meta)
        );

        MockMultipartFile filePart = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", VALID_PDF_BYTES
        );

        MvcResult res = mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(filePart)
                        .file(metaPart)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(objectMapper.readTree(res.getResponse().getContentAsString())
                .get("data").get("id").asText());
    }

    @Test
    @DisplayName("Create appointment for self synchronizes core reminder")
    void createAppointmentSuccessForSelf() throws Exception {
        OffsetDateTime apptTime = OffsetDateTime.now().plusDays(5);
        CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                .doctorName("Dr. Sarah Jenkins")
                .specialization("Cardiology")
                .clinicOrHospital("Heart Institute")
                .clinicPhone("+1-555-901-2345")
                .clinicAddress("100 Hospital Way")
                .appointmentTime(apptTime)
                .scheduledEndTime(apptTime.plusMinutes(45))
                .timeZone("UTC")
                .purpose("Routine annual heart checkup")
                .notes("Fast for 4 hours prior")
                .reminderOffsetMinutes(1440)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/healthcare/appointments")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.doctorName").value("Dr. Sarah Jenkins"))
                .andExpect(jsonPath("$.data.specialization").value("Cardiology"))
                .andExpect(jsonPath("$.data.clinicOrHospital").value("Heart Institute"))
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.dependentId").doesNotExist())
                .andExpect(jsonPath("$.data.disclaimer").isNotEmpty())
                .andReturn();

        String apptIdStr = objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText();
        UUID apptId = UUID.fromString(apptIdStr);

        // Verify reminder in core reminders table
        Optional<ReminderEntity> reminderOpt = reminderRepository
                .findByTargetEntityIdAndReminderTypeAndIsDeletedFalse(apptId, "HEALTH_APPOINTMENT");
        assertThat(reminderOpt).isPresent();
        ReminderEntity reminder = reminderOpt.get();
        assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.ACTIVE);
        assertThat(reminder.getTitle()).contains("Dr. Sarah Jenkins");
        assertThat(reminder.getDescription()).contains("Self");
        assertThat(reminder.getDueAt().toEpochSecond()).isEqualTo(apptTime.minusMinutes(1440).toEpochSecond());
    }

    @Test
    @DisplayName("Create appointment for family dependent successfully links profile")
    void createAppointmentSuccessForDependent() throws Exception {
        OffsetDateTime apptTime = OffsetDateTime.now().plusDays(3);
        CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                .doctorName("Dr. Emily Vance")
                .specialization("Pediatrics")
                .clinicOrHospital("Children's Wellness Center")
                .appointmentTime(apptTime)
                .purpose("Annual pediatric checkup & growth assessment")
                .dependentId(dependentAId)
                .build();

        mockMvc.perform(post("/api/v1/healthcare/appointments")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.doctorName").value("Dr. Emily Vance"))
                .andExpect(jsonPath("$.data.dependentId").value(dependentAId.toString()))
                .andExpect(jsonPath("$.data.dependentName").value("Emma Verified"))
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"));
    }

    @Test
    @DisplayName("Reschedule appointment modifies timestamp and updates reminder")
    void rescheduleAppointmentUpdatesReminder() throws Exception {
        OffsetDateTime originalTime = OffsetDateTime.now().plusDays(2);
        CreateAppointmentRequest createReq = CreateAppointmentRequest.builder()
                .doctorName("Dr. Robert Davis")
                .specialization("Dermatology")
                .clinicOrHospital("Metro Skin Clinic")
                .appointmentTime(originalTime)
                .purpose("Skin allergy consultation")
                .reminderOffsetMinutes(720) // 12 hours prior
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/v1/healthcare/appointments")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID apptId = UUID.fromString(objectMapper.readTree(createRes.getResponse().getContentAsString())
                .get("data").get("id").asText());

        // Reschedule to 7 days in future
        OffsetDateTime newTime = OffsetDateTime.now().plusDays(7);
        RescheduleAppointmentRequest reschedReq = RescheduleAppointmentRequest.builder()
                .newAppointmentTime(newTime)
                .reminderOffsetMinutes(720)
                .notes("Rescheduled due to business trip")
                .build();

        mockMvc.perform(post("/api/v1/healthcare/appointments/" + apptId + "/reschedule")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reschedReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESCHEDULED"))
                .andExpect(jsonPath("$.data.notes").value("Rescheduled due to business trip"));

        // Verify reminder updated
        ReminderEntity reminder = reminderRepository
                .findByTargetEntityIdAndReminderTypeAndIsDeletedFalse(apptId, "HEALTH_APPOINTMENT")
                .orElseThrow();
        assertThat(reminder.getDueAt().toEpochSecond()).isEqualTo(newTime.minusMinutes(720).toEpochSecond());
        assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.ACTIVE);
    }

    @Test
    @DisplayName("Completing or cancelling appointment dismisses active reminder")
    void updateStatusDismissesReminder() throws Exception {
        OffsetDateTime apptTime = OffsetDateTime.now().plusDays(4);
        CreateAppointmentRequest createReq = CreateAppointmentRequest.builder()
                .doctorName("Dr. Alan Grant")
                .specialization("Orthopedics")
                .clinicOrHospital("Joint & Bone Clinic")
                .appointmentTime(apptTime)
                .purpose("Knee pain evaluation")
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/v1/healthcare/appointments")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID apptId = UUID.fromString(objectMapper.readTree(createRes.getResponse().getContentAsString())
                .get("data").get("id").asText());

        // Verify reminder is initially ACTIVE
        ReminderEntity reminder = reminderRepository
                .findByTargetEntityIdAndReminderTypeAndIsDeletedFalse(apptId, "HEALTH_APPOINTMENT")
                .orElseThrow();
        assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.ACTIVE);

        // Transition status to COMPLETED
        UpdateStatusRequest statusReq = UpdateStatusRequest.builder()
                .status(AppointmentStatus.COMPLETED)
                .notes("Consultation completed; prescription issued")
                .build();

        mockMvc.perform(post("/api/v1/healthcare/appointments/" + apptId + "/status")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // Verify reminder is now DISMISSED
        ReminderEntity updatedReminder = reminderRepository
                .findByTargetEntityIdAndReminderTypeAndIsDeletedFalse(apptId, "HEALTH_APPOINTMENT")
                .orElseThrow();
        assertThat(updatedReminder.getStatus()).isEqualTo(ReminderStatus.DISMISSED);
    }

    @Test
    @DisplayName("Attach and detach medical document to appointment")
    void attachAndDetachMedicalDocument() throws Exception {
        UUID docId = uploadDocument(userAToken, "Blood_Work_2026.pdf", DocumentType.LAB_REPORT);

        OffsetDateTime apptTime = OffsetDateTime.now().plusDays(3);
        CreateAppointmentRequest createReq = CreateAppointmentRequest.builder()
                .doctorName("Dr. Karen Page")
                .specialization("Internal Medicine")
                .clinicOrHospital("City Clinic")
                .appointmentTime(apptTime)
                .purpose("Lab results review")
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/v1/healthcare/appointments")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID apptId = UUID.fromString(objectMapper.readTree(createRes.getResponse().getContentAsString())
                .get("data").get("id").asText());

        // 1. Attach document
        mockMvc.perform(post("/api/v1/healthcare/appointments/" + apptId + "/documents/" + docId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(docId.toString()))
                .andExpect(jsonPath("$.data.title").value("Blood_Work_2026.pdf"));

        // 2. Verify appointment details include document
        mockMvc.perform(get("/api/v1/healthcare/appointments/" + apptId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.linkedDocuments", hasSize(1)))
                .andExpect(jsonPath("$.data.linkedDocuments[0].id").value(docId.toString()));

        // 3. Detach document
        mockMvc.perform(delete("/api/v1/healthcare/appointments/" + apptId + "/documents/" + docId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk());

        // 4. Verify appointment linkedDocuments is now empty
        mockMvc.perform(get("/api/v1/healthcare/appointments/" + apptId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.linkedDocuments", hasSize(0)));
    }

    @Test
    @DisplayName("Cross-user appointment access is blocked with 404 Not Found")
    void crossUserAppointmentAccessReturns404() throws Exception {
        OffsetDateTime apptTime = OffsetDateTime.now().plusDays(4);
        CreateAppointmentRequest createReq = CreateAppointmentRequest.builder()
                .doctorName("Dr. Private Doctor")
                .specialization("Psychiatry")
                .clinicOrHospital("Private Therapy Suite")
                .appointmentTime(apptTime)
                .purpose("Confidential therapy session")
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/v1/healthcare/appointments")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID userAApptId = UUID.fromString(objectMapper.readTree(createRes.getResponse().getContentAsString())
                .get("data").get("id").asText());

        // User B attempts to read User A's appointment
        mockMvc.perform(get("/api/v1/healthcare/appointments/" + userAApptId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());

        // User B attempts to update User A's appointment
        UpdateAppointmentRequest updateReq = UpdateAppointmentRequest.builder()
                .doctorName("Dr. Hacker")
                .build();
        mockMvc.perform(put("/api/v1/healthcare/appointments/" + userAApptId)
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());

        // User B attempts to delete User A's appointment
        mockMvc.perform(delete("/api/v1/healthcare/appointments/" + userAApptId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Cross-user document attachment is blocked with 404 Not Found")
    void crossUserDocumentAttachmentReturns404() throws Exception {
        // User B uploads document
        UUID userBDocId = uploadDocument(userBToken, "UserB_MRI.pdf", DocumentType.DIAGNOSTIC_REPORT);

        // User A creates appointment
        OffsetDateTime apptTime = OffsetDateTime.now().plusDays(2);
        CreateAppointmentRequest createReq = CreateAppointmentRequest.builder()
                .doctorName("Dr. User A Doctor")
                .specialization("Neurology")
                .clinicOrHospital("Neuro Center")
                .appointmentTime(apptTime)
                .purpose("Headache review")
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/v1/healthcare/appointments")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID userAApptId = UUID.fromString(objectMapper.readTree(createRes.getResponse().getContentAsString())
                .get("data").get("id").asText());

        // User A attempts to attach User B's document to User A's appointment
        mockMvc.perform(post("/api/v1/healthcare/appointments/" + userAApptId + "/documents/" + userBDocId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Cross-user dependent linkage is rejected with 404 Not Found")
    void crossUserDependentReturns404() throws Exception {
        // User A attempts to create an appointment referencing User B's dependent
        OffsetDateTime apptTime = OffsetDateTime.now().plusDays(1);
        CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                .doctorName("Dr. Intruder")
                .specialization("General")
                .clinicOrHospital("General Hospital")
                .appointmentTime(apptTime)
                .purpose("Checkup")
                .dependentId(dependentBId) // Belongs to User B!
                .build();

        mockMvc.perform(post("/api/v1/healthcare/appointments")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Validation rejects invalid appointment end times and blank fields")
    void validationRejectsInvalidTimes() throws Exception {
        OffsetDateTime start = OffsetDateTime.now().plusDays(2);
        OffsetDateTime endBeforeStart = start.minusHours(1);

        CreateAppointmentRequest invalidTimeReq = CreateAppointmentRequest.builder()
                .doctorName("Dr. Time Traveler")
                .specialization("Physics")
                .clinicOrHospital("Clinic")
                .appointmentTime(start)
                .scheduledEndTime(endBeforeStart) // Invalid!
                .purpose("Time test")
                .build();

        mockMvc.perform(post("/api/v1/healthcare/appointments")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidTimeReq)))
                .andExpect(status().isBadRequest());

        CreateAppointmentRequest blankDoctorReq = CreateAppointmentRequest.builder()
                .doctorName("") // Blank!
                .specialization("Cardiology")
                .clinicOrHospital("Hospital")
                .appointmentTime(start)
                .purpose("Checkup")
                .build();

        mockMvc.perform(post("/api/v1/healthcare/appointments")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankDoctorReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Upcoming appointments query filters by time window correctly")
    void upcomingAppointmentsQuery() throws Exception {
        // Create appointment in 3 days
        OffsetDateTime soonAppt = OffsetDateTime.now().plusDays(3);
        CreateAppointmentRequest soonReq = CreateAppointmentRequest.builder()
                .doctorName("Dr. Soon")
                .specialization("Dentistry")
                .clinicOrHospital("Dental Spa")
                .appointmentTime(soonAppt)
                .purpose("Teeth cleaning")
                .build();

        mockMvc.perform(post("/api/v1/healthcare/appointments")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(soonReq)))
                .andExpect(status().isCreated());

        // Query upcoming with window of 7 days
        mockMvc.perform(get("/api/v1/healthcare/appointments/upcoming?windowDays=7")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.upcomingCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.appointments[0].doctorName").value("Dr. Soon"));
    }
}
