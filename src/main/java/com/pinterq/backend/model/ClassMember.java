package com.pinterq.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "class_members")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_group_id", nullable = false)
    private ClassGroup classGroup;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
