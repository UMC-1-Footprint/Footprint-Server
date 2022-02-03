package com.umc.footprint.api.controller.user;

import com.umc.footprint.api.entity.user.OAuthUser;
import com.umc.footprint.api.service.OAuthUserService;
import com.umc.footprint.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class OAuthUserController {

    private final OAuthUserService userService;

    @GetMapping("/info")
    public ApiResponse getUser() {
        org.springframework.security.core.userdetails.User principal = (org.springframework.security.core.userdetails.User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();


        Optional<OAuthUser> user = userService.getOAuthUser(principal.getUsername());

        return ApiResponse.success("user", user);
    }
}
