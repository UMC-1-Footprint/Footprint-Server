package com.umc.footprint.src.users;

import com.umc.footprint.src.users.model.UserOAuth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserProvider {

    private final UserDao userDao;

    @Autowired
    public UserProvider(UserDao userDao) {
        this.userDao = userDao;
    }

    UserOAuth findByUserOAuthId(String userId) {
        return userDao.findByUserID(userId);
    }
}
