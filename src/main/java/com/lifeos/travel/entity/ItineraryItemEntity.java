package com.lifeos.travel.entity;

import com.lifeos.common.entity.BaseEntity;
import com.lifeos.user.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "itinerary_items")
public class ItineraryItemEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private TripEntity trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 50)
    private ItineraryItemType itemType;

    @Column(name = "custom_type_name", length = 100)
    private String customTypeName;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "provider", length = 150)
    private String provider;

    @Column(name = "booking_reference", length = 100)
    private String bookingReference;

    @Column(name = "confirmation_details", columnDefinition = "TEXT")
    private String confirmationDetails;

    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @Column(name = "start_time_zone", nullable = false, length = 50)
    @Builder.Default
    private String startTimeZone = "UTC";

    @Column(name = "start_location", length = 255)
    private String startLocation;

    @Column(name = "end_time")
    private OffsetDateTime endTime;

    @Column(name = "end_time_zone", length = 50)
    @Builder.Default
    private String endTimeZone = "UTC";

    @Column(name = "end_location", length = 255)
    private String endLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private BookingStatus status = BookingStatus.CONFIRMED;

    @Column(name = "cost", precision = 14, scale = 2)
    private BigDecimal cost;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "exchange_rate_to_base", precision = 12, scale = 6)
    private BigDecimal exchangeRateToBase;

    @Column(name = "reminder_offset_minutes")
    private Integer reminderOffsetMinutes;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @PrePersist
    @PreUpdate
    protected void ensureMetadata() {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        if (startTimeZone == null || startTimeZone.isBlank()) {
            startTimeZone = "UTC";
        }
        if (endTimeZone == null || endTimeZone.isBlank()) {
            endTimeZone = "UTC";
        }
        if (currency == null || currency.isBlank()) {
            currency = "USD";
        }
    }
}
