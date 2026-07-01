package com.hypepia.apiverse.core.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("inquiries")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Inquiry {

    @Id
    private Long id;

    @Column("user_id")
    private Long userId;

    private String title;
    private String content;

    @Builder.Default
    private String status = "PENDING";

    private String answer;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("answered_at")
    private LocalDateTime answeredAt;
}
