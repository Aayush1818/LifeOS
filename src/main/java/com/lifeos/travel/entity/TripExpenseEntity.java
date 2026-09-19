package com.lifeos.travel.entity;

import com.lifeos.document.entity.DocumentEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "trip_expenses")
public class TripExpenseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private TripEntity trip;

    @Column(name = "amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "exchange_rate_to_base", precision = 12, scale = 6)
    private BigDecimal exchangeRateToBase;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_item_id")
    private ItineraryItemEntity itineraryItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private DocumentEntity document;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (currency == null || currency.isBlank()) {
            currency = "USD";
        }
    }
}
