/*
 * Copyright © 2023 Blue Habit.
 *
 * Unauthorized copying, publishing of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.bluehabit.eureka.component.data;

import com.bluehabit.eureka.component.AttachmentType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "tb_task_attachment"
)
@SQLDelete(
    sql = "UPDATE tb_task_attachment SET deleted=true WHERE id=?"
)
@Where(
    clause = "deleted = false"
)
public class TaskAttachment {
    @Id
    @GenericGenerator(
        name = "UUID",
        type = org.hibernate.id.uuid.UuidGenerator.class
    )
    private String id;
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.PERSIST})
    private Task task;
    @Column
    private String name;
    @Enumerated(EnumType.ORDINAL)
    @Column(columnDefinition = "int2")
    private AttachmentType type;
    @Column
    private String mimeType;
    @Column
    @CreatedDate
    private OffsetDateTime createdAt;
    @Column
    @LastModifiedDate
    private OffsetDateTime updatedAt;
    @Column(
        name = "deleted",
        nullable = false
    )
    private boolean deleted;
}
