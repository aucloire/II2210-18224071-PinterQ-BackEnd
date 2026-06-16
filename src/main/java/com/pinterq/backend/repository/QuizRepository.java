package com.pinterq.backend.repository;

import com.pinterq.backend.model.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    @Query("SELECT q FROM Quiz q WHERE q.material.category.id = :categoryId OR q.material.category.classGroup.id = :categoryId")
    List<Quiz> findByCategoryIdOrClassId(@Param("categoryId") Long categoryId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Quiz q WHERE q.material.id = :materialId")
    void deleteByMaterialId(@Param("materialId") Long materialId);
}
