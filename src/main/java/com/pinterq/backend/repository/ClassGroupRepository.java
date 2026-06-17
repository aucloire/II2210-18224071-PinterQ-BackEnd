package com.pinterq.backend.repository;

import com.pinterq.backend.model.ClassGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassGroupRepository extends JpaRepository<ClassGroup, Long> {
    Optional<ClassGroup> findByClassCode(String classCode);
    boolean existsByClassCode(String classCode);
    List<ClassGroup> findAllByTeacher_Id(Long teacherId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ClassGroup cg WHERE cg.teacher.id = :teacherId")
    void deleteByTeacherId(@Param("teacherId") Long teacherId);
}
