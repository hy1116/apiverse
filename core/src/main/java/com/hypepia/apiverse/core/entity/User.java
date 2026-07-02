package com.hypepia.apiverse.core.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    private Long id;

    private String email;

    @Column("password_hash")
    private String passwordHash;

    @Column("company_name")
    private String companyName;

    private String phone;

    @Builder.Default
    private String tier = "FREE";

    @Builder.Default
    private String role = "USER";

    @Column("created_at")
    private LocalDateTime createdAt;
}
