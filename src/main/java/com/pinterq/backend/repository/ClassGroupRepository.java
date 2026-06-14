package com.pinterq.backend.repository;

import com.pinterq.backend.model.ClassGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ClassGroupRepository extends JpaRepository<ClassGroup, Long> {
    Optional<ClassGroup> findByClassCode(String classCode);
    boolean existsByClassCode(String classCode);
}
