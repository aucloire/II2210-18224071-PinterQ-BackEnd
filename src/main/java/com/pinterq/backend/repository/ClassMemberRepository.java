package com.pinterq.backend.repository;

import com.pinterq.backend.model.ClassMember;
import com.pinterq.backend.model.ClassGroup;
import com.pinterq.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ClassMemberRepository extends JpaRepository<ClassMember, Long> {
    List<ClassMember> findByClassGroup(ClassGroup classGroup);
    List<ClassMember> findByUser(User user);
    Optional<ClassMember> findByClassGroupAndUser(ClassGroup classGroup, User user);
}
