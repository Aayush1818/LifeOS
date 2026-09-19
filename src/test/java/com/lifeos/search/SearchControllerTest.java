package com.lifeos.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.asset.dto.CreateAssetRequest;
import com.lifeos.asset.dto.CreateInvoiceRequest;
import com.lifeos.asset.dto.InvoiceItemDto;
import com.lifeos.asset.entity.AssetCategory;
import com.lifeos.asset.entity.InvoicePaymentStatus;
import com.lifeos.asset.service.AssetService;
import com.lifeos.asset.service.InvoiceService;
import com.lifeos.auth.dto.AuthResponse;
import com.lifeos.auth.dto.RegisterRequest;
import com.lifeos.auth.service.AuthService;
import com.lifeos.dependent.dto.CreateDependentRequest;
import com.lifeos.dependent.dto.DependentResponse;
import com.lifeos.dependent.entity.RelationshipType;
import com.lifeos.dependent.service.DependentService;
import com.lifeos.finance.dto.CreateTransactionRequest;
import com.lifeos.finance.entity.PaymentMethod;
import com.lifeos.finance.entity.TransactionCategory;
import com.lifeos.finance.entity.TransactionType;
import com.lifeos.finance.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private DependentService dependentService;

    private String tokenA;
    private UUID userAId;

    private String tokenB;
    private UUID userBId;

    @BeforeEach
    void setUp() {
        String suffixA = UUID.randomUUID().toString().substring(0, 8);
        RegisterRequest reqA = RegisterRequest.builder()
                .email("search.userA." + suffixA + "@example.com")
                .password("Password123!@#")
                .firstName("SearchUserA")
                .lastName("Tester")
                .build();
        AuthResponse resA = authService.register(reqA);
        tokenA = resA.getAccessToken();
        userAId = resA.getUser().getId();

        String suffixB = UUID.randomUUID().toString().substring(0, 8);
        RegisterRequest reqB = RegisterRequest.builder()
                .email("search.userB." + suffixB + "@example.com")
                .password("Password123!@#")
                .firstName("SearchUserB")
                .lastName("Tester")
                .build();
        AuthResponse resB = authService.register(reqB);
        tokenB = resB.getAccessToken();
        userBId = resB.getUser().getId();
    }

    @Test
    @DisplayName("1. Keyword search finds matching asset by name")
    void testKeywordSearch_SingleWord() throws Exception {
        CreateAssetRequest assetReq = CreateAssetRequest.builder()
                .name("MacBook Pro 16 M3 Max")
                .category(AssetCategory.ELECTRONICS)
                .brand("Apple")
                .modelNumber("A2991")
                .serialNumber("C02XYZ1234AB")
                .purchasePrice(new BigDecimal("3499.00"))
                .currency("USD")
                .purchaseDate(LocalDate.of(2026, 1, 15))
                .notes("Primary developer workstation")
                .build();
        assetService.createAsset(assetReq, userAId);

        mockMvc.perform(get("/api/v1/search")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("q", "MacBook"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].entityType").value("ASSET"))
                .andExpect(jsonPath("$.data.content[0].title").value("MacBook Pro 16 M3 Max"))
                .andExpect(jsonPath("$.data.content[0].currency").value("USD"))
                .andExpect(jsonPath("$.data.content[0].amount").value(3499.00));
    }

    @Test
    @DisplayName("2. Exact phrase search finds matching invoice by number")
    void testKeywordSearch_ExactPhrase() throws Exception {
        CreateInvoiceRequest invReq = CreateInvoiceRequest.builder()
                .invoiceNumber("INV-2026-SEARCH-01")
                .vendorName("Dell Technologies")
                .invoiceDate(LocalDate.of(2026, 2, 10))
                .currency("USD")
                .subtotal(new BigDecimal("1200.00"))
                .taxAmount(new BigDecimal("96.00"))
                .shippingAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .otherCharges(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("1296.00"))
                .paymentStatus(InvoicePaymentStatus.PAID)
                .paymentDate(LocalDate.of(2026, 2, 10))
                .notes("Dell UltraSharp Monitor purchase")
                .items(List.of(
                        InvoiceItemDto.builder()
                                .itemDescription("Dell 32 inch 4K Monitor")
                                .quantity(1)
                                .unitPrice(new BigDecimal("1200.00"))
                                .totalPrice(new BigDecimal("1200.00"))
                                .build()
                ))
                .build();
        invoiceService.createInvoice(invReq, userAId);

        mockMvc.perform(get("/api/v1/search")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("q", "\"Dell Technologies\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].entityType").value("INVOICE"))
                .andExpect(jsonPath("$.data.content[0].title").value("INV-2026-SEARCH-01"));
    }

    @Test
    @DisplayName("3. Empty query returns structured results with category filter")
    void testSearch_EmptyQueryWithFilters() throws Exception {
        CreateAssetRequest assetReq = CreateAssetRequest.builder()
                .name("Sony WH-1000XM5")
                .category(AssetCategory.ELECTRONICS)
                .brand("Sony")
                .purchasePrice(new BigDecimal("399.00"))
                .currency("USD")
                .purchaseDate(LocalDate.of(2026, 3, 1))
                .build();
        assetService.createAsset(assetReq, userAId);

        mockMvc.perform(get("/api/v1/search")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("category", "ELECTRONICS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].category").value("ELECTRONICS"));
    }

    @Test
    @DisplayName("4. Special characters in query are handled gracefully without syntax error")
    void testSearch_SpecialCharacters() throws Exception {
        mockMvc.perform(get("/api/v1/search")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("q", "test!@#$%^&*()_+-=[]{}|;':\",.<>/?`~"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("5. Entity type filter restricts results to specified types only")
    void testSearch_EntitiesFilter() throws Exception {
        // Create Asset
        CreateAssetRequest assetReq = CreateAssetRequest.builder()
                .name("Samsung Galaxy S24")
                .category(AssetCategory.ELECTRONICS)
                .brand("Samsung")
                .purchasePrice(new BigDecimal("899.00"))
                .currency("USD")
                .purchaseDate(LocalDate.of(2026, 4, 1))
                .build();
        assetService.createAsset(assetReq, userAId);

        // Create Finance Transaction
        CreateTransactionRequest txReq = CreateTransactionRequest.builder()
                .amount(new BigDecimal("899.00"))
                .transactionType(TransactionType.EXPENSE)
                .category(TransactionCategory.SHOPPING)
                .transactionDate(LocalDate.of(2026, 4, 1))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .description("Samsung Galaxy phone purchase")
                .build();
        transactionService.createTransaction(txReq, userAId);

        // Search with entities=ASSET
        mockMvc.perform(get("/api/v1/search")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("q", "Samsung")
                        .param("entities", "ASSET"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].entityType").value("ASSET"));
    }

    @Test
    @DisplayName("6. Date range filter correctly bounds results")
    void testSearch_DateRangeFilter() throws Exception {
        CreateAssetRequest assetReq = CreateAssetRequest.builder()
                .name("Spring Item 2026")
                .category(AssetCategory.OTHER)
                .purchasePrice(new BigDecimal("100.00"))
                .currency("USD")
                .purchaseDate(LocalDate.of(2026, 5, 15))
                .build();
        assetService.createAsset(assetReq, userAId);

        // In range
        mockMvc.perform(get("/api/v1/search")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))));

        // Out of range
        mockMvc.perform(get("/api/v1/search")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("q", "Spring Item 2026")
                        .param("startDate", "2026-06-01")
                        .param("endDate", "2026-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }

    @Test
    @DisplayName("7. Amount range and currency filters restrict results accurately")
    void testSearch_AmountAndCurrencyFilter() throws Exception {
        CreateAssetRequest assetReq = CreateAssetRequest.builder()
                .name("Expensive Luxury Watch")
                .category(AssetCategory.JEWELRY_LUXURY)
                .purchasePrice(new BigDecimal("5000.00"))
                .currency("USD")
                .purchaseDate(LocalDate.of(2026, 5, 20))
                .build();
        assetService.createAsset(assetReq, userAId);

        mockMvc.perform(get("/api/v1/search")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("minAmount", "4000.00")
                        .param("maxAmount", "6000.00")
                        .param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].amount").value(5000.00));
    }

    @Test
    @DisplayName("8. Dependent filter binds search and enriches dependentName")
    void testSearch_DependentFilterAndEnrichment() throws Exception {
        CreateDependentRequest depReq = CreateDependentRequest.builder()
                .fullName("Charlie Jr.")
                .relationship(RelationshipType.CHILD)
                .dateOfBirth(LocalDate.of(2018, 9, 10))
                .build();
        DependentResponse dep = dependentService.createDependent(userAId, depReq);

        CreateAssetRequest assetReq = CreateAssetRequest.builder()
                .name("Charlie's iPad Mini")
                .category(AssetCategory.ELECTRONICS)
                .dependentId(dep.getId())
                .purchasePrice(new BigDecimal("499.00"))
                .currency("USD")
                .purchaseDate(LocalDate.of(2026, 6, 1))
                .build();
        assetService.createAsset(assetReq, userAId);

        mockMvc.perform(get("/api/v1/search")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("dependentId", dep.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].dependentId").value(dep.getId().toString()))
                .andExpect(jsonPath("$.data.content[0].dependentName").value("Charlie Jr."));
    }

    @Test
    @DisplayName("9. Cross-tenant dependent filter returns 404 Not Found")
    void testSearch_CrossTenantDependentReturns404() throws Exception {
        // User B creates dependent
        CreateDependentRequest depReq = CreateDependentRequest.builder()
                .fullName("Bob Jr.")
                .relationship(RelationshipType.CHILD)
                .dateOfBirth(LocalDate.of(2019, 1, 1))
                .build();
        DependentResponse depB = dependentService.createDependent(userBId, depReq);

        // User A tries to search using User B's dependentId
        mockMvc.perform(get("/api/v1/search")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("dependentId", depB.getId().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("10. Strict multi-tenant isolation: User A records never appear in User B's search")
    void testSearch_MultiTenantIsolation() throws Exception {
        CreateAssetRequest assetReq = CreateAssetRequest.builder()
                .name("Confidential Classified Blueprints")
                .category(AssetCategory.OTHER)
                .purchasePrice(new BigDecimal("10000.00"))
                .currency("USD")
                .purchaseDate(LocalDate.of(2026, 7, 1))
                .build();
        assetService.createAsset(assetReq, userAId);

        // User B searches for same keyword
        mockMvc.perform(get("/api/v1/search")
                        .header("Authorization", "Bearer " + tokenB)
                        .param("q", "Confidential Classified Blueprints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }

    @Test
    @DisplayName("11. Pagination and sorting by amount descending")
    void testSearch_PaginationAndSorting() throws Exception {
        CreateAssetRequest asset1 = CreateAssetRequest.builder()
                .name("SortItem Cheap")
                .category(AssetCategory.OTHER)
                .purchasePrice(new BigDecimal("10.00"))
                .currency("USD")
                .purchaseDate(LocalDate.of(2026, 8, 1))
                .build();
        assetService.createAsset(asset1, userAId);

        CreateAssetRequest asset2 = CreateAssetRequest.builder()
                .name("SortItem Expensive")
                .category(AssetCategory.OTHER)
                .purchasePrice(new BigDecimal("990.00"))
                .currency("USD")
                .purchaseDate(LocalDate.of(2026, 8, 2))
                .build();
        assetService.createAsset(asset2, userAId);

        mockMvc.perform(get("/api/v1/search")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("q", "SortItem")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "AMOUNT_DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("SortItem Expensive"))
                .andExpect(jsonPath("$.data.content[1].title").value("SortItem Cheap"));
    }

    @Test
    @DisplayName("12. Faceted count endpoint returns counts partitioned by entity type")
    void testSearch_CountByEntity() throws Exception {
        CreateAssetRequest assetReq = CreateAssetRequest.builder()
                .name("Faceted Device")
                .category(AssetCategory.ELECTRONICS)
                .purchasePrice(new BigDecimal("250.00"))
                .currency("USD")
                .purchaseDate(LocalDate.of(2026, 8, 10))
                .build();
        assetService.createAsset(assetReq, userAId);

        mockMvc.perform(get("/api/v1/search/count")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("q", "Faceted Device"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.countsByEntity.ASSET", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("13. Typeahead suggestion endpoint returns matching title prefixes")
    void testSearch_Suggest() throws Exception {
        CreateAssetRequest assetReq = CreateAssetRequest.builder()
                .name("PlayStation 5 Pro")
                .category(AssetCategory.ELECTRONICS)
                .purchasePrice(new BigDecimal("699.00"))
                .currency("USD")
                .purchaseDate(LocalDate.of(2026, 8, 15))
                .build();
        assetService.createAsset(assetReq, userAId);

        mockMvc.perform(get("/api/v1/search/suggest")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("q", "Play")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].text").value(containsString("PlayStation 5 Pro")));
    }

    @Test
    @DisplayName("14. Invalid date range (startDate > endDate) returns 400 Bad Request")
    void testSearch_InvalidDateRangeReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/search")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("startDate", "2026-12-31")
                        .param("endDate", "2026-01-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("15. Invalid amount range (minAmount > maxAmount) returns 400 Bad Request")
    void testSearch_InvalidAmountRangeReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/search")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("minAmount", "500.00")
                        .param("maxAmount", "100.00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("16. List supported entities returns complete list of 15 entity types")
    void testListSupportedEntities() throws Exception {
        mockMvc.perform(get("/api/v1/search/entities")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasItem("ASSET")))
                .andExpect(jsonPath("$.data", hasItem("INVOICE")))
                .andExpect(jsonPath("$.data", hasItem("WARRANTY")))
                .andExpect(jsonPath("$.data", hasItem("WARRANTY_CLAIM")))
                .andExpect(jsonPath("$.data", hasItem("SERVICE_RECORD")))
                .andExpect(jsonPath("$.data", hasItem("LOAN")))
                .andExpect(jsonPath("$.data", hasItem("INSURANCE_POLICY")))
                .andExpect(jsonPath("$.data", hasItem("HEALTH_APPOINTMENT")))
                .andExpect(jsonPath("$.data", hasItem("TRIP")))
                .andExpect(jsonPath("$.data", hasItem("ITINERARY_ITEM")))
                .andExpect(jsonPath("$.data", hasItem("FINANCE_TRANSACTION")))
                .andExpect(jsonPath("$.data", hasItem("BUDGET")))
                .andExpect(jsonPath("$.data", hasItem("DOCUMENT")))
                .andExpect(jsonPath("$.data", hasItem("DEPENDENT")))
                .andExpect(jsonPath("$.data", hasItem("REMINDER")));
    }
}
