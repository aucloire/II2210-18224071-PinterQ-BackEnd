package com.pinterq.backend.repository;

import com.pinterq.backend.model.Flashcard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface FlashcardRepository extends JpaRepository<Flashcard, Long> {
    @Query("SELECT f FROM Flashcard f WHERE f.material.category.id = :categoryId OR f.material.category.classGroup.id = :categoryId")
    List<Flashcard> findByCategoryIdOrClassId(@Param("categoryId") Long categoryId);
}
