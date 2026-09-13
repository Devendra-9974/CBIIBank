package com.banking.dto;

import com.banking.enums.Role;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private Role role;
    private LocalDateTime createdAt;
}
