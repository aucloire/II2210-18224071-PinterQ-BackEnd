package com.pinterq.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "class_groups")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(name = "class_code", nullable = false, unique = true, length = 10)
    private String classCode;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "classGroup", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ClassMember> members;

    @OneToMany(mappedBy = "classGroup", fetch = FetchType.LAZY)
    private List<Category> categories;

    @PrePersist
    public void prePersist() {
        if (classCode == null) {
            classCode = generateClassCode();
        }
    }

    private static String generateClassCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return sb.toString();
    }
}
