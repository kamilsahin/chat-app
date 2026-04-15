package com.chatapp.common.security;

import com.chatapp.domain.model.User;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthHelper {

    private AuthHelper() {}

    public static User currentUser() {
        return (User) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }
}
