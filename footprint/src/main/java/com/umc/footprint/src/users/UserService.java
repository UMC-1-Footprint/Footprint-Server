package com.umc.footprint.src.users;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserDao userDao;
    private final UserProvider userProvider;

    @Autowired
    public UserService(UserDao userDao, UserProvider userProvider) {
        this.userDao = userDao;
        this.userProvider = userProvider;
    }

}
