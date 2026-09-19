package com.lifeos.asset.entity;

import com.lifeos.common.entity.BaseEntity;
import com.lifeos.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "asset_service_records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetServiceRecordEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private AssetEntity asset;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 50)
    private ServiceType serviceType;

    @Column(name = "service_provider", nullable = false, length = 255)
    private String serviceProvider;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "cost", precision = 14, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal cost = BigDecimal.ZERO.setScale(2);

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ServiceStatus status = ServiceStatus.COMPLETED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private InvoiceEntity invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warranty_claim_id")
    private WarrantyClaimEntity warrantyClaim;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
