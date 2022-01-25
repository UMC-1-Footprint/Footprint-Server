package com.umc.footprint.src.users;

import com.umc.footprint.src.users.model.UserOAuth;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserDao userDao;
    private final UserProvider userProvider;
    private final UserRepository userRepository;

//    @Autowired
//    public UserService(UserDao userDao, UserProvider userProvider) {
//        this.userDao = userDao;
//        this.userProvider = userProvider;
//    }

    public UserOAuth getUser(String userId) {
        return userRepository.findByUserOAuthId(userId);
    }
}
