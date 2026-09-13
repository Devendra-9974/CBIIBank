package com.banking.util;

import com.banking.entity.User;
import com.banking.exception.UnauthorizedException;
import com.banking.repository.UserRepository;
import com.banking.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    public UserDetailsImpl getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            throw new UnauthorizedException("User is not authenticated");
        }
        return (UserDetailsImpl) authentication.getPrincipal();
    }

    public Long getCurrentUserId() {
        return getCurrentUserDetails().getId();
    }

    public User getCurrentUser() {
        Long userId = getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));
    }
}
