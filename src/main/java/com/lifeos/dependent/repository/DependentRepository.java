package com.lifeos.dependent.repository;

import com.lifeos.dependent.entity.DependentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DependentRepository extends JpaRepository<DependentEntity, UUID> {

    List<DependentEntity> findAllByUserIdAndIsDeletedFalse(UUID userId);

    Optional<DependentEntity> findByIdAndUserIdAndIsDeletedFalse(UUID id, UUID userId);
}
