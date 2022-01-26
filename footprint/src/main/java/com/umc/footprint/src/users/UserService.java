package com.umc.footprint.src.users;

import com.umc.footprint.src.users.model.User;
import lombok.RequiredArgsConstructor;
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

    public User getUser(String userId) {
        return userRepository.findByUserId(userId);
    }
}
