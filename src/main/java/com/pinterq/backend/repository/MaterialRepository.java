package com.pinterq.backend.repository;

import com.pinterq.backend.model.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {
    @Query("SELECT m FROM Material m WHERE m.category.id = :categoryId OR m.category.classGroup.id = :categoryId")
    List<Material> findByCategoryIdOrClassId(@Param("categoryId") Long categoryId);
}
