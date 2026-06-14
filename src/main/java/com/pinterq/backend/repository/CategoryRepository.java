package com.pinterq.backend.repository;

import com.pinterq.backend.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUser_Id(Long userId);
    Optional<Category> findByClassGroup_Id(Long classId);
}
