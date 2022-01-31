package com.umc.footprint.src.users;

import com.umc.footprint.config.ApiResponse;
import com.umc.footprint.src.users.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserOAuthController {
    private final UserOAuthService userOauthService;

    @GetMapping
    public ApiResponse getUser() {
        org.springframework.security.core.userdetails.User principal = (org.springframework.security.core.userdetails.User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        User user = userOauthService.getUser(principal.getUsername());

        return ApiResponse.success("user", user);
    }
}
