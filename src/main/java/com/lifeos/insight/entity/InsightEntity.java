package com.lifeos.insight.entity;

import com.lifeos.common.entity.BaseEntity;
import com.lifeos.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "insights")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "insight_type", nullable = false, length = 60)
    private InsightType insightType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private InsightSeverity severity;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", length = 50)
    private InsightActionType actionType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "action_payload", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> actionPayload = new HashMap<>();

    @Column(name = "is_dismissed", nullable = false)
    @Builder.Default
    private boolean isDismissed = false;

    @Column(name = "is_actioned", nullable = false)
    @Builder.Default
    private boolean isActioned = false;
}
