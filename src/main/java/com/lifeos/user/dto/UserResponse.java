package com.lifeos.user.dto;

import com.lifeos.user.entity.Role;
import com.lifeos.user.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private Role role;
    private Map<String, Object> preferences;
    private OffsetDateTime createdAt;

    public static UserResponse fromEntity(UserEntity entity) {
        return UserResponse.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .phone(entity.getPhone())
                .role(entity.getRole())
                .preferences(entity.getPreferences())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
