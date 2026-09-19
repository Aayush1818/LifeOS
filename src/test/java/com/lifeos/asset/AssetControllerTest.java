package com.lifeos.asset;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.asset.dto.*;
import com.lifeos.asset.entity.*;
import com.lifeos.asset.repository.*;
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
import com.lifeos.document.service.DocumentService;
import com.lifeos.finance.dto.CreateTransactionRequest;
import com.lifeos.finance.dto.TransactionResponse;
import com.lifeos.finance.entity.PaymentMethod;
import com.lifeos.finance.entity.TransactionCategory;
import com.lifeos.finance.entity.TransactionType;
import com.lifeos.finance.service.TransactionService;
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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AssetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private DependentService dependentService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private WarrantyRepository warrantyRepository;

    private String userAToken;
    private UUID userAId;

    private String userBToken;
    private UUID userBId;

    @BeforeEach
    void setUp() {
        String suffixA = UUID.randomUUID().toString().substring(0, 8);
        RegisterRequest regA = RegisterRequest.builder()
                .email("asset.alice." + suffixA + "@example.com")
                .password("SecurePass123!@#")
                .firstName("Alice")
                .lastName("Asset")
                .build();
        AuthResponse authA = authService.register(regA);
        userAToken = authA.getAccessToken();
        userAId = authA.getUser().getId();

        String suffixB = UUID.randomUUID().toString().substring(0, 8);
        RegisterRequest regB = RegisterRequest.builder()
                .email("asset.bob." + suffixB + "@example.com")
                .password("SecurePass123!@#")
                .firstName("Bob")
                .lastName("Builder")
                .build();
        AuthResponse authB = authService.register(regB);
        userBToken = authB.getAccessToken();
        userBId = authB.getUser().getId();
    }

    @Test
    @DisplayName("1. Create Asset successfully and query by ID")
    void createAsset_success() throws Exception {
        CreateAssetRequest request = CreateAssetRequest.builder()
                .name("MacBook Pro 16")
                .category(AssetCategory.ELECTRONICS)
                .brand("Apple")
                .modelNumber("M3 Max 64GB")
                .serialNumber("C02G1234XYZ")
                .purchaseDate(LocalDate.now().minusMonths(1))
                .purchasePrice(new BigDecimal("3499.00"))
                .currency("USD")
                .location("Home Office")
                .notes("Work laptop")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/assets")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("MacBook Pro 16")))
                .andExpect(jsonPath("$.data.category", is("ELECTRONICS")))
                .andExpect(jsonPath("$.data.status", is("ACTIVE")))
                .andExpect(jsonPath("$.data.purchasePrice", is(3499.00)))
                .andReturn();

        String json = result.getResponse().getContentAsString();
        UUID assetId = UUID.fromString(objectMapper.readTree(json).path("data").path("id").asText());

        // Verify GET by ID
        mockMvc.perform(get("/api/v1/assets/" + assetId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(assetId.toString())))
                .andExpect(jsonPath("$.data.brand", is("Apple")));
    }

    @Test
    @DisplayName("2. Create Asset linked to family dependent")
    void createAsset_withDependent() throws Exception {
        CreateDependentRequest depReq = CreateDependentRequest.builder()
                .fullName("Charlie Asset")
                .relationship(RelationshipType.CHILD)
                .dateOfBirth(LocalDate.of(2015, 6, 15))
                .build();
        DependentResponse dep = dependentService.createDependent(userAId, depReq);

        CreateAssetRequest request = CreateAssetRequest.builder()
                .name("iPad Air 11-inch")
                .category(AssetCategory.ELECTRONICS)
                .brand("Apple")
                .dependentId(dep.getId())
                .purchasePrice(new BigDecimal("599.00"))
                .currency("USD")
                .build();

        mockMvc.perform(post("/api/v1/assets")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.dependentId", is(dep.getId().toString())))
                .andExpect(jsonPath("$.data.dependentName", is("Charlie Asset")));
    }

    @Test
    @DisplayName("3. Create Asset with cross-user dependent returns 404 Not Found")
    void createAsset_crossUserDependent_rejected() throws Exception {
        CreateDependentRequest depReq = CreateDependentRequest.builder()
                .fullName("Dave Builder")
                .relationship(RelationshipType.CHILD)
                .dateOfBirth(LocalDate.of(2018, 1, 1))
                .build();
        DependentResponse depB = dependentService.createDependent(userBId, depReq);

        CreateAssetRequest request = CreateAssetRequest.builder()
                .name("Bicycle")
                .category(AssetCategory.OTHER)
                .dependentId(depB.getId()) // Owned by Bob!
                .build();

        mockMvc.perform(post("/api/v1/assets")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Resource Not Found")));
    }

    @Test
    @DisplayName("4. Asset status transition updates status and writes immutable audit log")
    void updateAssetStatus_validTransition_withAuditHistory() throws Exception {
        CreateAssetRequest request = CreateAssetRequest.builder()
                .name("Sony WH-1000XM5")
                .category(AssetCategory.ELECTRONICS)
                .purchasePrice(new BigDecimal("399.99"))
                .currency("USD")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/assets")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID assetId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString()).path("data").path("id").asText());

        // Transition from ACTIVE to UNDER_REPAIR
        UpdateAssetStatusRequest statusReq = UpdateAssetStatusRequest.builder()
                .status(AssetStatus.UNDER_REPAIR)
                .reason("Headband hinge broke; sent to Sony repair center")
                .build();

        mockMvc.perform(patch("/api/v1/assets/" + assetId + "/status")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("UNDER_REPAIR")));

        // Query status audit history
        mockMvc.perform(get("/api/v1/assets/" + assetId + "/status-history")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2))) // Initial + transition
                .andExpect(jsonPath("$.data[0].fromStatus", is("ACTIVE")))
                .andExpect(jsonPath("$.data[0].toStatus", is("UNDER_REPAIR")))
                .andExpect(jsonPath("$.data[0].reason", containsString("Headband hinge broke")));
    }

    @Test
    @DisplayName("5. Invalid asset status transition returns 409 Conflict")
    void updateAssetStatus_invalidTransition_rejected() throws Exception {
        CreateAssetRequest request = CreateAssetRequest.builder()
                .name("LG OLED TV")
                .category(AssetCategory.ELECTRONICS)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/assets")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID assetId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString()).path("data").path("id").asText());

        // First transition to UNDER_REPAIR
        mockMvc.perform(patch("/api/v1/assets/" + assetId + "/status")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateAssetStatusRequest(AssetStatus.UNDER_REPAIR, "Repair"))))
                .andExpect(status().isOk());

        // Invalid transition: UNDER_REPAIR cannot transition directly to GIFTED
        mockMvc.perform(patch("/api/v1/assets/" + assetId + "/status")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateAssetStatusRequest(AssetStatus.GIFTED, "Gift to friend"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title", is("State Transition Conflict")));
    }

    @Test
    @DisplayName("6. Create Invoice with multiple line items and asset references")
    void createInvoice_withMultipleLineItems() throws Exception {
        // Create an asset first
        CreateAssetRequest assetReq = CreateAssetRequest.builder()
                .name("Dell UltraSharp Monitor 27")
                .category(AssetCategory.ELECTRONICS)
                .purchasePrice(new BigDecimal("499.00"))
                .currency("USD")
                .build();

        MvcResult assetRes = mockMvc.perform(post("/api/v1/assets")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assetReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID assetId = UUID.fromString(objectMapper.readTree(assetRes.getResponse().getContentAsString()).path("data").path("id").asText());

        // Create Invoice with 2 line items: item 1 linked to asset, item 2 non-asset cable
        InvoiceItemDto item1 = InvoiceItemDto.builder()
                .assetId(assetId)
                .itemDescription("Dell U2723QE 4K Hub Monitor")
                .quantity(1)
                .unitPrice(new BigDecimal("499.00"))
                .totalPrice(new BigDecimal("499.00"))
                .build();

        InvoiceItemDto item2 = InvoiceItemDto.builder()
                .itemDescription("Thunderbolt 4 Pro Cable 2m")
                .quantity(2)
                .unitPrice(new BigDecimal("69.00"))
                .totalPrice(new BigDecimal("138.00"))
                .build();

        CreateInvoiceRequest invReq = CreateInvoiceRequest.builder()
                .invoiceNumber("AMZN-2026-98124")
                .vendorName("Amazon.com")
                .invoiceDate(LocalDate.now().minusDays(5))
                .returnDeadline(LocalDate.now().plusDays(25))
                .currency("USD")
                .subtotal(new BigDecimal("637.00"))
                .taxAmount(new BigDecimal("50.96"))
                .shippingAmount(new BigDecimal("15.00"))
                .discountAmount(new BigDecimal("20.00"))
                .totalAmount(new BigDecimal("682.96"))
                .paymentStatus(InvoicePaymentStatus.PAID)
                .paymentMethod("CREDIT_CARD")
                .items(List.of(item1, item2))
                .build();

        mockMvc.perform(post("/api/v1/assets/invoices")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.invoiceNumber", is("AMZN-2026-98124")))
                .andExpect(jsonPath("$.data.vendorName", is("Amazon.com")))
                .andExpect(jsonPath("$.data.subtotal", is(637.00)))
                .andExpect(jsonPath("$.data.totalAmount", is(682.96)))
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.items[0].assetId", is(assetId.toString())))
                .andExpect(jsonPath("$.data.items[0].assetName", is("Dell UltraSharp Monitor 27")))
                .andExpect(jsonPath("$.data.items[1].assetId").doesNotExist());
    }

    @Test
    @DisplayName("7. Multi-currency deterministic acquisition summary (null consolidated total for mixed currencies)")
    void getAcquisitionSummary_multiCurrency() throws Exception {
        // Asset 1 in USD
        mockMvc.perform(post("/api/v1/assets")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateAssetRequest.builder()
                                .name("iPhone 16 Pro")
                                .category(AssetCategory.ELECTRONICS)
                                .purchasePrice(new BigDecimal("1199.00"))
                                .currency("USD")
                                .build())))
                .andExpect(status().isCreated());

        // Asset 2 in USD
        mockMvc.perform(post("/api/v1/assets")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateAssetRequest.builder()
                                .name("AirPods Pro 2")
                                .category(AssetCategory.ELECTRONICS)
                                .purchasePrice(new BigDecimal("249.00"))
                                .currency("USD")
                                .build())))
                .andExpect(status().isCreated());

        // Asset 3 in EUR
        mockMvc.perform(post("/api/v1/assets")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateAssetRequest.builder()
                                .name("German Coffee Machine")
                                .category(AssetCategory.APPLIANCE)
                                .purchasePrice(new BigDecimal("450.00"))
                                .currency("EUR")
                                .build())))
                .andExpect(status().isCreated());

        // Asset 4 with unrecorded cost
        mockMvc.perform(post("/api/v1/assets")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateAssetRequest.builder()
                                .name("Vintage Family Ring")
                                .category(AssetCategory.JEWELRY_LUXURY)
                                .purchasePrice(null)
                                .currency("USD")
                                .build())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/assets/acquisition-summary")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalsByCurrency.USD", is(1448.00)))
                .andExpect(jsonPath("$.data.totalsByCurrency.EUR", is(450.00)))
                .andExpect(jsonPath("$.data.assetCountByCurrency.USD", is(2)))
                .andExpect(jsonPath("$.data.assetCountByCurrency.EUR", is(1)))
                .andExpect(jsonPath("$.data.assetsWithUnrecordedCost", is(1)))
                .andExpect(jsonPath("$.data.consolidatedTotal").doesNotExist()); // Multi-currency must be null / omitted
    }

    @Test
    @DisplayName("8. Link Invoice to existing Finance Transaction with same-user validation")
    void linkTransaction_success_andCrossTenantRejection() throws Exception {
        // User A creates a Finance transaction
        CreateTransactionRequest txReqA = CreateTransactionRequest.builder()
                .amount(new BigDecimal("89.99"))
                .transactionType(TransactionType.EXPENSE)
                .category(TransactionCategory.SHOPPING)
                .transactionDate(LocalDate.now())
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .description("Logitech MX Master 3S")
                .build();
        TransactionResponse txA = transactionService.createTransaction(txReqA, userAId);

        // User A creates an invoice
        CreateInvoiceRequest invReq = CreateInvoiceRequest.builder()
                .invoiceNumber("INV-8831")
                .vendorName("Best Buy")
                .invoiceDate(LocalDate.now())
                .totalAmount(new BigDecimal("89.99"))
                .items(List.of(InvoiceItemDto.builder()
                        .itemDescription("Wireless Mouse")
                        .unitPrice(new BigDecimal("89.99"))
                        .build()))
                .build();

        MvcResult invRes = mockMvc.perform(post("/api/v1/assets/invoices")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID invoiceId = UUID.fromString(objectMapper.readTree(invRes.getResponse().getContentAsString()).path("data").path("id").asText());

        // User B creates a transaction
        CreateTransactionRequest txReqB = CreateTransactionRequest.builder()
                .amount(new BigDecimal("50.00"))
                .transactionType(TransactionType.EXPENSE)
                .category(TransactionCategory.SHOPPING)
                .transactionDate(LocalDate.now())
                .paymentMethod(PaymentMethod.CASH)
                .description("Bob transaction")
                .build();
        TransactionResponse txB = transactionService.createTransaction(txReqB, userBId);

        // Attempt to link User A's invoice to User B's transaction -> 404 Not Found
        mockMvc.perform(post("/api/v1/assets/invoices/" + invoiceId + "/link-transaction")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LinkTransactionRequest(txB.getId()))))
                .andExpect(status().isNotFound());

        // Successfully link User A's invoice to User A's transaction
        mockMvc.perform(post("/api/v1/assets/invoices/" + invoiceId + "/link-transaction")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LinkTransactionRequest(txA.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transactionId", is(txA.getId().toString())));

        // Idempotency: re-linking same transaction returns 200 OK
        mockMvc.perform(post("/api/v1/assets/invoices/" + invoiceId + "/link-transaction")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LinkTransactionRequest(txA.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transactionId", is(txA.getId().toString())));
    }

    @Test
    @DisplayName("9. Convert Invoice to Transaction atomically and prevent duplicate conversion")
    void convertInvoiceToTransaction_duplicatePrevention() throws Exception {
        CreateInvoiceRequest invReq = CreateInvoiceRequest.builder()
                .invoiceNumber("IKEA-4491")
                .vendorName("IKEA")
                .invoiceDate(LocalDate.now().minusDays(2))
                .totalAmount(new BigDecimal("249.50"))
                .currency("USD")
                .items(List.of(InvoiceItemDto.builder()
                        .itemDescription("Desk Standing Converter")
                        .unitPrice(new BigDecimal("249.50"))
                        .build()))
                .build();

        MvcResult invRes = mockMvc.perform(post("/api/v1/assets/invoices")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID invoiceId = UUID.fromString(objectMapper.readTree(invRes.getResponse().getContentAsString()).path("data").path("id").asText());

        // Convert invoice to transaction
        ConvertInvoiceToTransactionRequest convReq = ConvertInvoiceToTransactionRequest.builder()
                .category(TransactionCategory.SHOPPING)
                .paymentMethod(PaymentMethod.DEBIT_CARD)
                .build();

        MvcResult convRes = mockMvc.perform(post("/api/v1/assets/invoices/" + invoiceId + "/convert-to-transaction")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(convReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.transactionId", notNullValue()))
                .andReturn();

        // Attempting second conversion must return 409 Conflict
        mockMvc.perform(post("/api/v1/assets/invoices/" + invoiceId + "/convert-to-transaction")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(convReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title", is("Conflict")));
    }

    @Test
    @DisplayName("10. Add Warranty with timezone reminder and verify LIFETIME warranty never creates reminder")
    void warrantyReminders_andLifetimePolicy() throws Exception {
        // Create asset
        CreateAssetRequest assetReq = CreateAssetRequest.builder()
                .name("Sony A7 IV Camera")
                .category(AssetCategory.ELECTRONICS)
                .purchasePrice(new BigDecimal("2498.00"))
                .build();

        MvcResult assetRes = mockMvc.perform(post("/api/v1/assets")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assetReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID assetId = UUID.fromString(objectMapper.readTree(assetRes.getResponse().getContentAsString()).path("data").path("id").asText());

        // 10a. Standard manufacturer warranty with future expiry -> reminder created
        CreateWarrantyRequest w1 = CreateWarrantyRequest.builder()
                .provider("Sony Electronics")
                .warrantyType(WarrantyType.MANUFACTURER)
                .policyNumber("SNY-W-991")
                .startDate(LocalDate.now().minusMonths(6))
                .expiryDate(LocalDate.now().plusMonths(6))
                .reminderOffsetDays(30)
                .build();

        MvcResult w1Res = mockMvc.perform(post("/api/v1/assets/" + assetId + "/warranties")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(w1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.reminderId", notNullValue()))
                .andReturn();

        UUID w1ReminderId = UUID.fromString(objectMapper.readTree(w1Res.getResponse().getContentAsString()).path("data").path("reminderId").asText());
        Optional<ReminderEntity> reminder = reminderRepository.findById(w1ReminderId);
        assertThat(reminder).isPresent();
        assertThat(reminder.get().getStatus()).isEqualTo(ReminderStatus.ACTIVE);
        assertThat(reminder.get().getReminderType()).isEqualTo("WARRANTY_EXPIRY");

        // 10b. LIFETIME warranty -> reminderId MUST be null
        CreateWarrantyRequest wLifetime = CreateWarrantyRequest.builder()
                .provider("Craftsman / Stanley")
                .warrantyType(WarrantyType.LIFETIME)
                .startDate(LocalDate.now())
                .coverageDetails("Full lifetime replacement guarantee")
                .build();

        mockMvc.perform(post("/api/v1/assets/" + assetId + "/warranties")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wLifetime)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.warrantyType", is("LIFETIME")))
                .andExpect(jsonPath("$.data.expiryDate").doesNotExist())
                .andExpect(jsonPath("$.data.reminderId").doesNotExist());
    }

    @Test
    @DisplayName("11. Warranty status transition to VOID dismisses active reminder")
    void voidWarranty_dismissesReminder() throws Exception {
        CreateAssetRequest assetReq = CreateAssetRequest.builder()
                .name("Samsung Refrigerator")
                .category(AssetCategory.APPLIANCE)
                .build();

        MvcResult assetRes = mockMvc.perform(post("/api/v1/assets")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assetReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID assetId = UUID.fromString(objectMapper.readTree(assetRes.getResponse().getContentAsString()).path("data").path("id").asText());

        CreateWarrantyRequest wReq = CreateWarrantyRequest.builder()
                .provider("Samsung")
                .warrantyType(WarrantyType.EXTENDED)
                .startDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusYears(1))
                .build();

        MvcResult wRes = mockMvc.perform(post("/api/v1/assets/" + assetId + "/warranties")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID warrantyId = UUID.fromString(objectMapper.readTree(wRes.getResponse().getContentAsString()).path("data").path("id").asText());
        UUID reminderId = UUID.fromString(objectMapper.readTree(wRes.getResponse().getContentAsString()).path("data").path("reminderId").asText());

        // Transition warranty to VOID
        mockMvc.perform(patch("/api/v1/assets/warranties/" + warrantyId + "/status")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateWarrantyStatusRequest(WarrantyStatus.VOID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("VOID")));

        // Reminder should now be DISMISSED
        Optional<ReminderEntity> rem = reminderRepository.findById(reminderId);
        assertThat(rem).isPresent();
        assertThat(rem.get().getStatus()).isEqualTo(ReminderStatus.DISMISSED);
    }

    @Test
    @DisplayName("12. Warranty Claim lifecycle, resolution, costs, and queryability after warranty expiry")
    void warrantyClaimLifecycle_andQueryabilityPostExpiry() throws Exception {
        CreateAssetRequest assetReq = CreateAssetRequest.builder()
                .name("Dyson V15 Detect Vacuum")
                .category(AssetCategory.APPLIANCE)
                .build();

        MvcResult assetRes = mockMvc.perform(post("/api/v1/assets")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assetReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID assetId = UUID.fromString(objectMapper.readTree(assetRes.getResponse().getContentAsString()).path("data").path("id").asText());

        CreateWarrantyRequest wReq = CreateWarrantyRequest.builder()
                .provider("Dyson Care")
                .warrantyType(WarrantyType.MANUFACTURER)
                .startDate(LocalDate.now().minusMonths(11))
                .expiryDate(LocalDate.now().plusMonths(1))
                .build();

        MvcResult wRes = mockMvc.perform(post("/api/v1/assets/" + assetId + "/warranties")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID warrantyId = UUID.fromString(objectMapper.readTree(wRes.getResponse().getContentAsString()).path("data").path("id").asText());

        // File a claim
        CreateWarrantyClaimRequest claimReq = CreateWarrantyClaimRequest.builder()
                .claimNumber("DYS-CLM-1002")
                .claimDate(LocalDate.now().minusDays(5))
                .claimType(ClaimType.REPAIR)
                .description("Motor pulsation error and laser optic failure")
                .build();

        MvcResult claimRes = mockMvc.perform(post("/api/v1/assets/warranties/" + warrantyId + "/claims")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(claimReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status", is("FILED")))
                .andReturn();

        UUID claimId = UUID.fromString(objectMapper.readTree(claimRes.getResponse().getContentAsString()).path("data").path("id").asText());

        // Transition: FILED -> UNDER_REVIEW
        mockMvc.perform(patch("/api/v1/assets/warranties/claims/" + claimId + "/status")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateClaimStatusRequest.builder()
                                .status(ClaimStatus.UNDER_REVIEW)
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("UNDER_REVIEW")));

        // Transition: UNDER_REVIEW -> APPROVED
        mockMvc.perform(patch("/api/v1/assets/warranties/claims/" + claimId + "/status")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateClaimStatusRequest.builder()
                                .status(ClaimStatus.APPROVED)
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("APPROVED")));

        // Transition: APPROVED -> RESOLVED with costs
        mockMvc.perform(patch("/api/v1/assets/warranties/claims/" + claimId + "/status")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateClaimStatusRequest.builder()
                                .status(ClaimStatus.RESOLVED)
                                .resolution("Motor replaced under warranty, returned via UPS")
                                .resolvedDate(LocalDate.now())
                                .claimCostCovered(new BigDecimal("185.00"))
                                .outOfPocketCost(BigDecimal.ZERO)
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("RESOLVED")))
                .andExpect(jsonPath("$.data.claimCostCovered", is(185.00)));

        // Expire the warranty
        mockMvc.perform(patch("/api/v1/assets/warranties/" + warrantyId + "/status")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateWarrantyStatusRequest(WarrantyStatus.EXPIRED))))
                .andExpect(status().isOk());

        // Historical claim remains queryable by asset ID even after warranty expiry
        mockMvc.perform(get("/api/v1/assets/" + assetId + "/claims")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].claimNumber", is("DYS-CLM-1002")))
                .andExpect(jsonPath("$.data[0].status", is("RESOLVED")));
    }

    @Test
    @DisplayName("13. Record Asset Service and Maintenance")
    void createServiceRecord_success() throws Exception {
        CreateAssetRequest assetReq = CreateAssetRequest.builder()
                .name("Tesla Model 3")
                .category(AssetCategory.VEHICLE)
                .build();

        MvcResult assetRes = mockMvc.perform(post("/api/v1/assets")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assetReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID assetId = UUID.fromString(objectMapper.readTree(assetRes.getResponse().getContentAsString()).path("data").path("id").asText());

        CreateAssetServiceRecordRequest sReq = CreateAssetServiceRecordRequest.builder()
                .serviceDate(LocalDate.now())
                .serviceType(ServiceType.ROUTINE_MAINTENANCE)
                .serviceProvider("Tesla Service Center Fremont")
                .description("Brake fluid flush and cabin HEPA filter replacement")
                .cost(new BigDecimal("120.00"))
                .currency("USD")
                .status(ServiceStatus.COMPLETED)
                .build();

        mockMvc.perform(post("/api/v1/assets/" + assetId + "/services")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.serviceProvider", is("Tesla Service Center Fremont")))
                .andExpect(jsonPath("$.data.cost", is(120.00)))
                .andExpect(jsonPath("$.data.serviceType", is("ROUTINE_MAINTENANCE")));
    }

    @Test
    @DisplayName("14. Link Phase 4 Document to Asset and Invoice")
    void documentLinking_assetAndInvoice() throws Exception {
        // Upload mock document
        MockMultipartFile file = new MockMultipartFile(
                "file", "laptop_receipt.pdf", "application/pdf", "receipt content".getBytes(StandardCharsets.UTF_8));
        UploadDocumentRequest docReq = UploadDocumentRequest.builder()
                .title("MacBook Purchase Receipt")
                .documentType(DocumentType.RECEIPT)
                .category(DocumentCategory.FINANCIAL)
                .build();

        var docRes = documentService.uploadDocument(file, docReq, userAId);
        UUID docId = docRes.getId();

        // Create asset
        CreateAssetRequest assetReq = CreateAssetRequest.builder()
                .name("Mac Studio M2")
                .category(AssetCategory.ELECTRONICS)
                .build();

        MvcResult assetRes = mockMvc.perform(post("/api/v1/assets")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assetReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID assetId = UUID.fromString(objectMapper.readTree(assetRes.getResponse().getContentAsString()).path("data").path("id").asText());

        // Attach document to asset
        mockMvc.perform(post("/api/v1/assets/" + assetId + "/documents/" + docId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(docId.toString())))
                .andExpect(jsonPath("$.data.title", is("MacBook Purchase Receipt")));

        // List asset documents
        mockMvc.perform(get("/api/v1/assets/" + assetId + "/documents")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id", is(docId.toString())));
    }

    @Test
    @DisplayName("15. Strict cross-tenant isolation on all asset endpoints (404 Not Found)")
    void crossTenantIsolation_enforced() throws Exception {
        // User A creates an asset
        CreateAssetRequest assetReq = CreateAssetRequest.builder()
                .name("Alice Gold Watch")
                .category(AssetCategory.JEWELRY_LUXURY)
                .purchasePrice(new BigDecimal("5000.00"))
                .build();

        MvcResult assetRes = mockMvc.perform(post("/api/v1/assets")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assetReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID assetAId = UUID.fromString(objectMapper.readTree(assetRes.getResponse().getContentAsString()).path("data").path("id").asText());

        // User B attempts to GET User A's asset -> 404 Not Found
        mockMvc.perform(get("/api/v1/assets/" + assetAId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());

        // User B attempts to update User A's asset -> 404 Not Found
        mockMvc.perform(put("/api/v1/assets/" + assetAId)
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateAssetRequest.builder().name("Hacked").build())))
                .andExpect(status().isNotFound());

        // User B attempts to delete User A's asset -> 404 Not Found
        mockMvc.perform(delete("/api/v1/assets/" + assetAId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());
    }
}
