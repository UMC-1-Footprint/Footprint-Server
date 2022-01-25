package com.umc.footprint.src.users;


import com.umc.footprint.common.ApiResponse;
import com.umc.footprint.src.users.model.UserOAuth;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


//@RestController
//@RequestMapping("/users")
//public class UserController {
//
//    @Autowired
//    private final UserProvider userProvider;
//    @Autowired
//    private final UserService userService;
//
//    public UserController(UserProvider userProvider, UserService userService) {
//        this.userProvider = userProvider;
//        this.userService = userService;
//    }
//
//
//}

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public ApiResponse getUser() {
        org.springframework.security.core.userdetails.User principal = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserOAuth userOAuth = userService.getUser(principal.getUsername());

        return ApiResponse.success("userOAuth", userOAuth);
    }
}