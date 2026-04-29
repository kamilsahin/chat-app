package com.chatapp.api.controller;

import com.chatapp.api.dto.UpdateProfileRequest;
import com.chatapp.common.security.AuthHelper;
import com.chatapp.domain.model.User;
import com.chatapp.internal.dto.SyncUserRequest;
import com.chatapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    public User getUser(@PathVariable String userId) {
        return userService.getByExternalId(userId);
    }

    @PutMapping("/me")
    public User updateProfile(@RequestBody UpdateProfileRequest request) {
        User current = AuthHelper.currentUser();
        return userService.syncUser(current.getExternalId(),
                new SyncUserRequest(request.displayName(), request.nickname(), request.avatarUrl(), request.bio()));
    }

    @PutMapping("/me/fcm-token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateFcmToken(@RequestBody Map<String, String> body) {
        userService.updateFcmToken(AuthHelper.currentUser().getExternalId(), body.get("token"));
    }
}
