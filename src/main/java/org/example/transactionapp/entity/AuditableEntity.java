package org.example.transactionapp.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@MappedSuperclass
public class AuditableEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @CreationTimestamp
    @JsonFormat(pattern = "dd.MM.yyyy")
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime created;

    @UpdateTimestamp
    @JsonFormat(pattern = "dd.MM.yyyy")
    @Column(name = "modified_at",nullable = false)
    private OffsetDateTime modifiedAt;

    @Column(name = "archived_at")
    private OffsetDateTime archivedAt;


}
