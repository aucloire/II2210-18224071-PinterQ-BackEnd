package com.pinterq.backend.repository;

import com.pinterq.backend.model.ClassMember;
import com.pinterq.backend.model.ClassGroup;
import com.pinterq.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassMemberRepository extends JpaRepository<ClassMember, Long> {
    List<ClassMember> findByClassGroup(ClassGroup classGroup);
    List<ClassMember> findByUser(User user);
    Optional<ClassMember> findByClassGroupAndUser(ClassGroup classGroup, User user);

    @Modifying
    @Transactional
    @Query("DELETE FROM ClassMember cm WHERE cm.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
