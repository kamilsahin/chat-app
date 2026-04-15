package com.chatapp.internal.controller;

import com.chatapp.domain.model.User;
import com.chatapp.internal.dto.SyncUserRequest;
import com.chatapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    @PutMapping("/{externalId}")
    public User syncUser(
            @PathVariable String externalId,
            @RequestBody SyncUserRequest request) {
        return userService.syncUser(externalId, request);
    }

    @DeleteMapping("/{externalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable String externalId) {
        userService.deleteByExternalId(externalId);
    }
}
