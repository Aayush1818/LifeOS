package com.lifeos.asset.entity;

import com.lifeos.common.entity.BaseEntity;
import com.lifeos.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "warranty_claims")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarrantyClaimEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warranty_id", nullable = false)
    private WarrantyEntity warranty;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private AssetEntity asset;

    @Column(name = "claim_number", length = 100)
    private String claimNumber;

    @Column(name = "claim_date", nullable = false)
    private LocalDate claimDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "claim_type", nullable = false, length = 50)
    private ClaimType claimType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ClaimStatus status = ClaimStatus.FILED;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "resolution", columnDefinition = "TEXT")
    private String resolution;

    @Column(name = "resolved_date")
    private LocalDate resolvedDate;

    @Column(name = "claim_cost_covered", precision = 14, scale = 2)
    private BigDecimal claimCostCovered;

    @Column(name = "out_of_pocket_cost", precision = 14, scale = 2)
    private BigDecimal outOfPocketCost;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";
}
