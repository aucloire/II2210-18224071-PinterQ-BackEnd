package com.pinterq.backend.repository;

import com.pinterq.backend.model.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    @Modifying
    @Transactional
    @Query("DELETE FROM QuizAttempt a WHERE a.material.id = :materialId")
    void deleteByMaterialId(@Param("materialId") Long materialId);
}
