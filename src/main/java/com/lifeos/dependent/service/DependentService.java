package com.lifeos.dependent.service;

import com.lifeos.common.exception.ResourceNotFoundException;
import com.lifeos.dependent.dto.CreateDependentRequest;
import com.lifeos.dependent.dto.DependentResponse;
import com.lifeos.dependent.dto.UpdateDependentRequest;
import com.lifeos.dependent.entity.DependentEntity;
import com.lifeos.dependent.repository.DependentRepository;
import com.lifeos.user.entity.UserEntity;
import com.lifeos.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DependentService {

    private final DependentRepository dependentRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<DependentResponse> listDependents(UUID userId) {
        return dependentRepository.findAllByUserIdAndIsDeletedFalse(userId).stream()
                .map(DependentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DependentResponse getDependentById(UUID id, UUID userId) {
        DependentEntity entity = dependentRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Dependent not found with id: " + id));
        return DependentResponse.fromEntity(entity);
    }

    @Transactional
    public DependentResponse createDependent(UUID userId, CreateDependentRequest request) {
        UserEntity user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        DependentEntity entity = DependentEntity.builder()
                .user(user)
                .fullName(request.getFullName().trim())
                .relationship(request.getRelationship())
                .dateOfBirth(request.getDateOfBirth())
                .emergencyPhone(request.getEmergencyPhone() != null ? request.getEmergencyPhone().trim() : null)
                .medicalNotes(request.getMedicalNotes() != null ? request.getMedicalNotes() : new HashMap<>())
                .build();

        entity = dependentRepository.save(entity);
        log.info("Created dependent [{}] for user [{}]", entity.getId(), userId);
        return DependentResponse.fromEntity(entity);
    }

    @Transactional
    public DependentResponse updateDependent(UUID id, UUID userId, UpdateDependentRequest request) {
        DependentEntity entity = dependentRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Dependent not found with id: " + id));

        entity.setFullName(request.getFullName().trim());
        entity.setRelationship(request.getRelationship());
        entity.setDateOfBirth(request.getDateOfBirth());
        if (request.getEmergencyPhone() != null) {
            entity.setEmergencyPhone(request.getEmergencyPhone().trim());
        }
        if (request.getMedicalNotes() != null) {
            entity.setMedicalNotes(request.getMedicalNotes());
        }

        entity = dependentRepository.save(entity);
        log.info("Updated dependent [{}] for user [{}]", id, userId);
        return DependentResponse.fromEntity(entity);
    }

    @Transactional
    public void deleteDependent(UUID id, UUID userId) {
        DependentEntity entity = dependentRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Dependent not found with id: " + id));

        entity.setDeleted(true);
        dependentRepository.save(entity);
        log.info("Soft-deleted dependent [{}] for user [{}]", id, userId);
    }
}
