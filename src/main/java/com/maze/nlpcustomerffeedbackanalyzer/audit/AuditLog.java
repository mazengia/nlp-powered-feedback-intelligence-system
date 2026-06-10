package com.maze.nlpcustomerffeedbackanalyzer.audit;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_entity_type", columnList = "entityType"),
        @Index(name = "idx_entity_id", columnList = "entityId"),
        @Index(name = "idx_operation", columnList = "operation"),
        @Index(name = "idx_created_at", columnList = "createdAt"),
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String entityType;

    @Column(nullable = false)
    private Long entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Operation operation;

    @Column(columnDefinition = "TEXT")
    private String oldValue;

    @Column(columnDefinition = "TEXT")
    private String newValue;

    @Column(length = 200)
    private String changedFields;

    @Column(length = 100)
    private String createdBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(length = 50)
    private String ipAddress;

    @Column(length = 500)
    private String details;

    public enum Operation {
        CREATE, UPDATE, DELETE, READ
    }
}

