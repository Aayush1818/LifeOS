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
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "trips")
public class TripEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "destination", nullable = false, length = 200)
    private String destination;

    @Column(name = "trip_title", nullable = false, length = 200)
    private String tripTitle;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "total_budget", precision = 14, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalBudget = BigDecimal.ZERO;

    @Column(name = "actual_spend", precision = 14, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal actualSpend = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private TripStatus status = TripStatus.PLANNED;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

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
        if (totalBudget == null) {
            totalBudget = BigDecimal.ZERO;
        }
        if (actualSpend == null) {
            actualSpend = BigDecimal.ZERO;
        }
        if (currency == null || currency.isBlank()) {
            currency = "USD";
        }
    }
}
