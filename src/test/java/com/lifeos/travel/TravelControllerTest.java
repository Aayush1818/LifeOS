package com.lifeos.travel;

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
import com.lifeos.reminder.entity.ReminderEntity;
import com.lifeos.reminder.entity.ReminderStatus;
import com.lifeos.reminder.repository.ReminderRepository;
import com.lifeos.travel.dto.AddTravelerRequest;
import com.lifeos.travel.dto.CreateItineraryItemRequest;
import com.lifeos.travel.dto.CreateTripRequest;
import com.lifeos.travel.dto.UpdateBookingStatusRequest;
import com.lifeos.travel.dto.UpdateItineraryItemRequest;
import com.lifeos.travel.entity.BookingStatus;
import com.lifeos.travel.entity.ItineraryItemType;
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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TravelControllerTest {

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

    @BeforeEach
    void setUp() {
        String suffixA = UUID.randomUUID().toString().substring(0, 8);
        RegisterRequest regA = RegisterRequest.builder()
                .email("travel.userA." + suffixA + "@example.com")
                .password("SecurePass123!@#")
                .firstName("Traveler")
                .lastName("Alice")
                .build();
        AuthResponse authA = authService.register(regA);
        userAToken = authA.getAccessToken();
        userAId = authA.getUser().getId();

        String suffixB = UUID.randomUUID().toString().substring(0, 8);
        RegisterRequest regB = RegisterRequest.builder()
                .email("travel.userB." + suffixB + "@example.com")
                .password("SecurePass123!@#")
                .firstName("Traveler")
                .lastName("Bob")
                .build();
        AuthResponse authB = authService.register(regB);
        userBToken = authB.getAccessToken();
        userBId = authB.getUser().getId();
    }

    @Test
    @DisplayName("1. Create Trip successfully and verify organizer auto-registered")
    void createTrip_success() throws Exception {
        CreateTripRequest request = CreateTripRequest.builder()
                .destination("Tokyo, Japan")
                .tripTitle("Tokyo Autumn Expedition")
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(20))
                .totalBudget(new BigDecimal("4500.00"))
                .currency("USD")
                .notes("Explore Shibuya, Shinjuku, and Mt. Fuji")
                .build();

        mockMvc.perform(post("/api/v1/travel/trips")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.destination", is("Tokyo, Japan")))
                .andExpect(jsonPath("$.data.tripTitle", is("Tokyo Autumn Expedition")))
                .andExpect(jsonPath("$.data.totalBudget", is(4500.00)))
                .andExpect(jsonPath("$.data.currency", is("USD")))
                .andExpect(jsonPath("$.data.status", is("PLANNED")))
                .andExpect(jsonPath("$.data.travelersCount", is(1)));
    }

    @Test
    @DisplayName("2. Create Trip with end date before start date throws BadRequest")
    void createTrip_endDateBeforeStartDate_throwsBadRequest() throws Exception {
        CreateTripRequest request = CreateTripRequest.builder()
                .destination("Paris, France")
                .tripTitle("Time Travel Trip")
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(5)) // invalid
                .totalBudget(new BigDecimal("2000.00"))
                .build();

        mockMvc.perform(post("/api/v1/travel/trips")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("3. Add Family Dependent to Trip and verify traveler list")
    void addTraveler_dependent_success() throws Exception {
        UUID tripId = createTestTrip(userAToken, "London, UK", "Family Vacation", LocalDate.now().plusDays(14), LocalDate.now().plusDays(21));

        CreateDependentRequest depReq = CreateDependentRequest.builder()
                .fullName("Emma Alice")
                .relationship(RelationshipType.CHILD)
                .dateOfBirth(LocalDate.of(2018, 5, 12))
                .build();
        DependentResponse depRes = dependentService.createDependent(userAId, depReq);

        AddTravelerRequest travReq = AddTravelerRequest.builder()
                .travelerName("Emma Alice")
                .dependentId(depRes.getId())
                .notes("Child traveler")
                .build();

        mockMvc.perform(post("/api/v1/travel/trips/" + tripId + "/travelers")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(travReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.travelerName", is("Emma Alice")))
                .andExpect(jsonPath("$.data.dependentId", is(depRes.getId().toString())));

        // Verify trip details includes 2 travelers
        mockMvc.perform(get("/api/v1/travel/trips/" + tripId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.travelers", hasSize(2)));
    }

    @Test
    @DisplayName("4. Add User B's Dependent to User A's Trip throws NotFound (Cross-Tenant Security)")
    void addTraveler_crossUserDependent_throwsNotFound() throws Exception {
        UUID tripId = createTestTrip(userAToken, "Rome, Italy", "Italian Summer", LocalDate.now().plusDays(5), LocalDate.now().plusDays(12));

        // Create dependent under User B
        CreateDependentRequest depReq = CreateDependentRequest.builder()
                .fullName("Bob Jr")
                .relationship(RelationshipType.CHILD)
                .dateOfBirth(LocalDate.of(2019, 3, 20))
                .build();
        DependentResponse depResB = dependentService.createDependent(userBId, depReq);

        AddTravelerRequest travReq = AddTravelerRequest.builder()
                .travelerName("Bob Jr")
                .dependentId(depResB.getId()) // belongs to User B!
                .build();

        mockMvc.perform(post("/api/v1/travel/trips/" + tripId + "/travelers")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(travReq)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("5. Multi-Currency: Single base currency calculates consolidated total")
    void multiCurrency_singleBaseCurrency_consolidatedTotalCalculated() throws Exception {
        UUID tripId = createTestTrip(userAToken, "New York, USA", "Broadway Trip", LocalDate.now().plusDays(5), LocalDate.now().plusDays(10));

        // Add 2 items in USD (matching trip currency)
        createTestFlight(userAToken, tripId, "Flight to NYC", new BigDecimal("450.00"), "USD", null);
        createTestHotel(userAToken, tripId, "Manhattan Hotel", new BigDecimal("850.00"), "USD", null);

        mockMvc.perform(get("/api/v1/travel/trips/" + tripId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.spendSummary.baseCurrency", is("USD")))
                .andExpect(jsonPath("$.data.spendSummary.consolidatedTotal", is(1300.00)))
                .andExpect(jsonPath("$.data.spendSummary.hasUnconvertedCurrencies", is(false)))
                .andExpect(jsonPath("$.data.spendSummary.totalsByCurrency.USD", is(1300.00)));
    }

    @Test
    @DisplayName("6. Multi-Currency: Mixed currencies without rates sets consolidatedTotal to null (No silent mixing)")
    void multiCurrency_mixedCurrencies_withoutRates_consolidatedTotalNull() throws Exception {
        UUID tripId = createTestTrip(userAToken, "Global Tour", "Multi-Nation Tour", LocalDate.now().plusDays(20), LocalDate.now().plusDays(40));

        // $200 flight, €500 hotel, ₹50,000 activity
        createTestFlight(userAToken, tripId, "Flight Transatlantic", new BigDecimal("200.00"), "USD", null);
        createTestHotel(userAToken, tripId, "Parisian Boutique Hotel", new BigDecimal("500.00"), "EUR", null);
        createTestActivity(userAToken, tripId, "Taj Mahal Guided Tour", new BigDecimal("50000.00"), "INR", null);

        mockMvc.perform(get("/api/v1/travel/trips/" + tripId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.spendSummary.baseCurrency", is("USD")))
                .andExpect(jsonPath("$.data.spendSummary.consolidatedTotal", nullValue())) // STRICTLY NULL!
                .andExpect(jsonPath("$.data.spendSummary.hasUnconvertedCurrencies", is(true)))
                .andExpect(jsonPath("$.data.spendSummary.totalsByCurrency.USD", is(200.00)))
                .andExpect(jsonPath("$.data.spendSummary.totalsByCurrency.EUR", is(500.00)))
                .andExpect(jsonPath("$.data.spendSummary.totalsByCurrency.INR", is(50000.00)))
                .andExpect(jsonPath("$.data.spendSummary.conversionPolicyNotice", containsString("without explicit exchange rates")));
    }

    @Test
    @DisplayName("7. Multi-Currency: Mixed currencies with explicit rates computes exact consolidated total")
    void multiCurrency_mixedCurrencies_withExplicitRates_consolidatedTotalCalculated() throws Exception {
        UUID tripId = createTestTrip(userAToken, "Europe Tour", "Euro Trip 2026", LocalDate.now().plusDays(15), LocalDate.now().plusDays(25));

        // Base USD 300.00 + EUR 500.00 @ 1.10 rate (= $550.00) = Total $850.00
        createTestFlight(userAToken, tripId, "Flight to London", new BigDecimal("300.00"), "USD", null);
        createTestHotel(userAToken, tripId, "Hotel Paris", new BigDecimal("500.00"), "EUR", new BigDecimal("1.10"));

        mockMvc.perform(get("/api/v1/travel/trips/" + tripId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.spendSummary.baseCurrency", is("USD")))
                .andExpect(jsonPath("$.data.spendSummary.consolidatedTotal", is(850.00)))
                .andExpect(jsonPath("$.data.spendSummary.hasUnconvertedCurrencies", is(false)))
                .andExpect(jsonPath("$.data.spendSummary.totalsByCurrency.USD", is(300.00)))
                .andExpect(jsonPath("$.data.spendSummary.totalsByCurrency.EUR", is(500.00)));
    }

    @Test
    @DisplayName("8. Timezone: India -> UK flight preserves local zones and syncs UTC reminder")
    void timezone_indiaToUk_localTimesAndReminderSynced() throws Exception {
        UUID tripId = createTestTrip(userAToken, "London, UK", "UK Conference", LocalDate.now().plusDays(5), LocalDate.now().plusDays(10));

        // Departure: 06:00 AM IST (+05:30) = 00:30 UTC
        // Arrival: 11:30 AM GMT/BST (UTC) = 11:30 UTC (11-hour flight)
        OffsetDateTime istDeparture = OffsetDateTime.of(2026, 11, 10, 6, 0, 0, 0, ZoneOffset.ofHoursMinutes(5, 30));
        OffsetDateTime gmtArrival = OffsetDateTime.of(2026, 11, 10, 11, 30, 0, 0, ZoneOffset.UTC);

        CreateItineraryItemRequest flightReq = CreateItineraryItemRequest.builder()
                .itemType(ItineraryItemType.FLIGHT)
                .title("Air India AI 161 Delhi to London")
                .provider("Air India")
                .bookingReference("AI-DEL-LHR-99")
                .startTime(istDeparture)
                .startTimeZone("Asia/Kolkata")
                .startLocation("Indira Gandhi Intl Airport (DEL)")
                .endTime(gmtArrival)
                .endTimeZone("Europe/London")
                .endLocation("London Heathrow Airport (LHR)")
                .cost(new BigDecimal("650.00"))
                .currency("USD")
                .reminderOffsetMinutes(1440) // 24h prior
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/travel/trips/" + tripId + "/itinerary")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(flightReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.startTimeZone", is("Asia/Kolkata")))
                .andExpect(jsonPath("$.data.endTimeZone", is("Europe/London")))
                .andExpect(jsonPath("$.data.bookingReference", is("AI-DEL-LHR-99")))
                .andReturn();

        String itemId = objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asText();

        // Verify reminder due date in UTC: istDeparture (00:30 UTC) minus 1440 min (24 hours) = previous day 00:30 UTC
        Optional<ReminderEntity> reminderOpt = reminderRepository.findByTargetEntityIdAndIsDeletedFalse(UUID.fromString(itemId));
        assertThat(reminderOpt).isPresent();
        ReminderEntity reminder = reminderOpt.get();
        assertThat(reminder.getReminderType()).isEqualTo("TRAVEL_DEPARTURE");
        assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.ACTIVE);
        assertThat(reminder.getDueAt().toInstant()).isEqualTo(istDeparture.minusHours(24).toInstant());
    }

    @Test
    @DisplayName("9. Timezone: US -> Europe flight crossing midnight detects cross-calendar-date arrival")
    void timezone_usToEurope_midnightCrossDateDetection() throws Exception {
        UUID tripId = createTestTrip(userAToken, "Paris, France", "Paris Getaway", LocalDate.now().plusDays(2), LocalDate.now().plusDays(8));

        // Departs NYC at 18:30 EDT (Oct 15)
        // Arrives Paris at 08:00 CEST (Oct 16, next calendar day)
        OffsetDateTime edtDeparture = OffsetDateTime.of(2026, 10, 15, 18, 30, 0, 0, ZoneOffset.ofHours(-4));
        OffsetDateTime cestArrival = OffsetDateTime.of(2026, 10, 16, 8, 0, 0, 0, ZoneOffset.ofHours(2));

        CreateItineraryItemRequest flightReq = CreateItineraryItemRequest.builder()
                .itemType(ItineraryItemType.FLIGHT)
                .title("Air France AF 007 JFK to CDG")
                .provider("Air France")
                .bookingReference("AF-JFK-CDG")
                .startTime(edtDeparture)
                .startTimeZone("America/New_York")
                .startLocation("JFK Terminal 1")
                .endTime(cestArrival)
                .endTimeZone("Europe/Paris")
                .endLocation("CDG Terminal 2E")
                .cost(new BigDecimal("780.00"))
                .currency("USD")
                .build();

        mockMvc.perform(post("/api/v1/travel/trips/" + tripId + "/itinerary")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(flightReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.overnightOrCrossDate", is(true)))
                .andExpect(jsonPath("$.data.startTimeZone", is("America/New_York")))
                .andExpect(jsonPath("$.data.endTimeZone", is("Europe/Paris")));
    }

    @Test
    @DisplayName("10. Itinerary: Rescheduling updates reminder; Cancelling booking dismisses reminder")
    void itinerary_reminderSync_andDismissalOnCancellation() throws Exception {
        UUID tripId = createTestTrip(userAToken, "Berlin, Germany", "Tech Summit", LocalDate.now().plusDays(10), LocalDate.now().plusDays(15));

        OffsetDateTime initialDeparture = OffsetDateTime.now().plusDays(10);
        CreateItineraryItemRequest trainReq = CreateItineraryItemRequest.builder()
                .itemType(ItineraryItemType.TRAIN)
                .title("ICE Express to Berlin")
                .provider("Deutsche Bahn")
                .bookingReference("DB-ICE-88")
                .startTime(initialDeparture)
                .startTimeZone("Europe/Berlin")
                .reminderOffsetMinutes(120) // 2 hours prior
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/travel/trips/" + tripId + "/itinerary")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(trainReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID itemId = UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asText());

        // Verify initial reminder
        ReminderEntity reminder = reminderRepository.findByTargetEntityIdAndIsDeletedFalse(itemId).orElseThrow();
        assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.ACTIVE);
        assertThat(reminder.getDueAt().toEpochSecond()).isCloseTo(initialDeparture.minusHours(2).toEpochSecond(), within(2L));

        // 1. Reschedule train departure by +4 hours
        OffsetDateTime rescheduledTime = initialDeparture.plusHours(4);
        UpdateItineraryItemRequest updateReq = UpdateItineraryItemRequest.builder()
                .startTime(rescheduledTime)
                .reminderOffsetMinutes(120)
                .build();

        mockMvc.perform(put("/api/v1/travel/trips/" + tripId + "/itinerary/" + itemId)
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk());

        // Verify reminder due date updated
        ReminderEntity updatedReminder = reminderRepository.findByTargetEntityIdAndIsDeletedFalse(itemId).orElseThrow();
        assertThat(updatedReminder.getDueAt().toEpochSecond()).isCloseTo(rescheduledTime.minusHours(2).toEpochSecond(), within(2L));

        // 2. Cancel the train booking
        UpdateBookingStatusRequest cancelReq = UpdateBookingStatusRequest.builder()
                .status(BookingStatus.CANCELLED)
                .notes("Train cancelled due to storm")
                .build();

        mockMvc.perform(post("/api/v1/travel/trips/" + tripId + "/itinerary/" + itemId + "/status")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("CANCELLED")));

        // Verify reminder automatically dismissed
        ReminderEntity cancelledReminder = reminderRepository.findByTargetEntityIdAndIsDeletedFalse(itemId).orElseThrow();
        assertThat(cancelledReminder.getStatus()).isEqualTo(ReminderStatus.DISMISSED);
    }

    @Test
    @DisplayName("11. Documents: Attach and detach travel boarding pass to itinerary item")
    void attachAndDetachTravelDocument_success() throws Exception {
        UUID tripId = createTestTrip(userAToken, "Madrid, Spain", "Spanish Holiday", LocalDate.now().plusDays(20), LocalDate.now().plusDays(28));
        UUID flightId = createTestFlight(userAToken, tripId, "Iberia Flight to Madrid", new BigDecimal("400.00"), "USD", null);

        // Upload boarding pass document via Phase 4 API
        UUID docId = uploadTestDocument(userAToken, "BoardingPass.pdf", DocumentCategory.TRAVEL, DocumentType.BOARDING_PASS);

        // Attach document to flight item
        mockMvc.perform(post("/api/v1/travel/trips/" + tripId + "/itinerary/" + flightId + "/documents/" + docId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(docId.toString())))
                .andExpect(jsonPath("$.data.documentType", is("BOARDING_PASS")));

        // Verify item details includes the document
        mockMvc.perform(get("/api/v1/travel/trips/" + tripId + "/itinerary/" + flightId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.linkedDocuments", hasSize(1)))
                .andExpect(jsonPath("$.data.linkedDocuments[0].id", is(docId.toString())));

        // Detach document
        mockMvc.perform(delete("/api/v1/travel/trips/" + tripId + "/itinerary/" + flightId + "/documents/" + docId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk());

        // Verify document detached
        mockMvc.perform(get("/api/v1/travel/trips/" + tripId + "/itinerary/" + flightId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.linkedDocuments", hasSize(0)));
    }

    @Test
    @DisplayName("12. Security: User B accessing User A's trip throws NotFound (404)")
    void crossUser_tripAccess_throwsNotFound() throws Exception {
        UUID tripAId = createTestTrip(userAToken, "Kyoto, Japan", "Secret Zen Retreat", LocalDate.now().plusDays(30), LocalDate.now().plusDays(40));

        mockMvc.perform(get("/api/v1/travel/trips/" + tripAId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("13. Security: User B accessing User A's itinerary throws NotFound (404)")
    void crossUser_itineraryAccess_throwsNotFound() throws Exception {
        UUID tripAId = createTestTrip(userAToken, "Vienna, Austria", "Classical Tour", LocalDate.now().plusDays(10), LocalDate.now().plusDays(15));
        UUID flightId = createTestFlight(userAToken, tripAId, "Flight to Vienna", new BigDecimal("500.00"), "USD", null);

        mockMvc.perform(get("/api/v1/travel/trips/" + tripAId + "/itinerary/" + flightId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("14. Security: User B attaching User A's document throws NotFound (404)")
    void crossUser_attachDocument_throwsNotFound() throws Exception {
        UUID tripAId = createTestTrip(userAToken, "Sydney, Australia", "Harbour Tour", LocalDate.now().plusDays(40), LocalDate.now().plusDays(50));
        UUID docAId = uploadTestDocument(userAToken, "VisaAustralia.pdf", DocumentCategory.TRAVEL, DocumentType.VISA);

        UUID tripBId = createTestTrip(userBToken, "Toronto, Canada", "Canada Trip", LocalDate.now().plusDays(10), LocalDate.now().plusDays(15));

        // User B tries to link User A's document to User B's trip
        mockMvc.perform(post("/api/v1/travel/trips/" + tripBId + "/documents/" + docAId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("15. Upcoming Trips: Correctly filters trips within the specified day window")
    void upcomingTrips_windowFiltering() throws Exception {
        // Trip 1: starts in 10 days (inside 30d window)
        createTestTrip(userAToken, "Denver, USA", "Ski Trip", LocalDate.now().plusDays(10), LocalDate.now().plusDays(15));
        // Trip 2: starts in 60 days (outside 30d window)
        createTestTrip(userAToken, "Cape Town, South Africa", "Safari", LocalDate.now().plusDays(60), LocalDate.now().plusDays(75));

        mockMvc.perform(get("/api/v1/travel/trips/upcoming?windowDays=30")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.upcomingCount", is(1)))
                .andExpect(jsonPath("$.data.trips[0].destination", is("Denver, USA")));
    }

    // --- Helper Methods ---

    private UUID createTestTrip(String token, String destination, String title, LocalDate start, LocalDate end) throws Exception {
        CreateTripRequest req = CreateTripRequest.builder()
                .destination(destination)
                .tripTitle(title)
                .startDate(start)
                .endDate(end)
                .totalBudget(new BigDecimal("3000.00"))
                .currency("USD")
                .build();

        MvcResult res = mockMvc.perform(post("/api/v1/travel/trips")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(objectMapper.readTree(res.getResponse().getContentAsString()).path("data").path("id").asText());
    }

    private UUID createTestFlight(String token, UUID tripId, String title, BigDecimal cost, String currency, BigDecimal exchangeRate) throws Exception {
        CreateItineraryItemRequest req = CreateItineraryItemRequest.builder()
                .itemType(ItineraryItemType.FLIGHT)
                .title(title)
                .provider("Delta Air Lines")
                .bookingReference("DL-REF-" + UUID.randomUUID().toString().substring(0, 5))
                .startTime(OffsetDateTime.now().plusDays(5))
                .startTimeZone("America/New_York")
                .endTime(OffsetDateTime.now().plusDays(5).plusHours(6))
                .endTimeZone("Europe/London")
                .cost(cost)
                .currency(currency)
                .exchangeRateToBase(exchangeRate)
                .reminderOffsetMinutes(1440)
                .build();

        MvcResult res = mockMvc.perform(post("/api/v1/travel/trips/" + tripId + "/itinerary")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(objectMapper.readTree(res.getResponse().getContentAsString()).path("data").path("id").asText());
    }

    private UUID createTestHotel(String token, UUID tripId, String title, BigDecimal cost, String currency, BigDecimal exchangeRate) throws Exception {
        CreateItineraryItemRequest req = CreateItineraryItemRequest.builder()
                .itemType(ItineraryItemType.LODGING)
                .title(title)
                .provider("Marriott Hotels")
                .bookingReference("MAR-REF-" + UUID.randomUUID().toString().substring(0, 5))
                .startTime(OffsetDateTime.now().plusDays(5).plusHours(7))
                .startTimeZone("Europe/Paris")
                .endTime(OffsetDateTime.now().plusDays(8))
                .endTimeZone("Europe/Paris")
                .cost(cost)
                .currency(currency)
                .exchangeRateToBase(exchangeRate)
                .reminderOffsetMinutes(120)
                .build();

        MvcResult res = mockMvc.perform(post("/api/v1/travel/trips/" + tripId + "/itinerary")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(objectMapper.readTree(res.getResponse().getContentAsString()).path("data").path("id").asText());
    }

    private UUID createTestActivity(String token, UUID tripId, String title, BigDecimal cost, String currency, BigDecimal exchangeRate) throws Exception {
        CreateItineraryItemRequest req = CreateItineraryItemRequest.builder()
                .itemType(ItineraryItemType.ACTIVITY)
                .title(title)
                .provider("City Tours")
                .startTime(OffsetDateTime.now().plusDays(6))
                .startTimeZone("Asia/Kolkata")
                .cost(cost)
                .currency(currency)
                .exchangeRateToBase(exchangeRate)
                .build();

        MvcResult res = mockMvc.perform(post("/api/v1/travel/trips/" + tripId + "/itinerary")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(objectMapper.readTree(res.getResponse().getContentAsString()).path("data").path("id").asText());
    }

    private UUID uploadTestDocument(String token, String filename, DocumentCategory category, DocumentType type) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", filename, "application/pdf", "%PDF-1.4 travel dummy content".getBytes(StandardCharsets.UTF_8));
        UploadDocumentRequest meta = UploadDocumentRequest.builder()
                .title(filename)
                .category(category)
                .documentType(type)
                .build();
        MockMultipartFile metaFile = new MockMultipartFile("metadata", "", "application/json", objectMapper.writeValueAsBytes(meta));

        MvcResult res = mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(file)
                        .file(metaFile)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(objectMapper.readTree(res.getResponse().getContentAsString()).path("data").path("id").asText());
    }
}
