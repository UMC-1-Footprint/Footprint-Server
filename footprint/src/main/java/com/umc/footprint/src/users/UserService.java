package com.umc.footprint.src.users;

import static com.umc.footprint.config.BaseResponseStatus.*;

import com.umc.footprint.config.BaseException;
import com.umc.footprint.config.BaseResponseStatus;
import com.umc.footprint.src.users.model.*;

import com.umc.footprint.utils.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

@Service
public class UserService {
    private final UserDao userDao;
    private final UserProvider userProvider;
    private final JwtService jwtService;

    @Autowired
    public UserService(UserDao userDao, UserProvider userProvider, JwtService jwtService) {
        this.userDao = userDao;
        this.userProvider = userProvider;
        this.jwtService = jwtService;
    }


    // yummy 12
    @Transactional(rollbackFor = Exception.class)
    public BadgeInfo modifyRepBadge(int userIdx, int badgeIdx) throws BaseException {
        try {
            BadgeInfo patchRepBadgeInfo = userDao.modifyRepBadge(userIdx, badgeIdx);
            return patchRepBadgeInfo;
          } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    // 닉네임 수정(Patch)
    public void modifyNickname(PatchNicknameReq patchNicknameReq) throws BaseException {
        try {
            int result = userDao.modifyNickname(patchNicknameReq);

            if (result == 0) { // 닉네임 변경 실패
                throw new BaseException(MODIFY_NICKNAME_FAIL);
            }
        } catch (Exception exception) { // DB에 이상이 있는 경우 에러 메시지를 보냅니다.
            throw new BaseException(DATABASE_ERROR);
        }
    }


    @Transactional(rollbackFor = Exception.class)
    public void modifyGoal(int userIdx, PatchUserGoalReq patchUserGoalReq) throws BaseException{
        try{
            int resultTime = userDao.modifyUserGoalTime(userIdx, patchUserGoalReq);
            if(resultTime == 0)
                throw new BaseException(BaseResponseStatus.MODIFY_USER_GOAL_FAIL);

            int resultDay = userDao.modifyUserGoalDay(userIdx, patchUserGoalReq);
            if(resultDay == 0)
                throw new BaseException(BaseResponseStatus.MODIFY_USER_GOAL_FAIL);

        } catch(Exception exception){
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw new BaseException(BaseResponseStatus.DATABASE_ERROR);
        }
    }


    // 해당 userIdx를 갖는 Goal 정보 저장
    @Transactional(rollbackFor = Exception.class)
    public int postUserInfo(int userIdx, PatchUserInfoReq patchUserInfoReq) throws BaseException{
        try {
            int resultInfo = userDao.modifyUserInfo(userIdx, patchUserInfoReq);
            int result = userDao.postGoal(userIdx, patchUserInfoReq);
            int resultNext = userDao.postGoalNext(userIdx, patchUserInfoReq);

            if(resultInfo == 0 || result == 0 || resultNext == 0)
                return 0;
            return 1;

        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public PostLoginRes postUserLogin(PostLoginReq postLoginReq) throws BaseException {
        // email 중복 확인 있으면 status에 Done 넣고 return
        System.out.println("UserService.postUserLogin1");
        PostLoginRes result = userProvider.checkEmail(postLoginReq.getEmail());
        System.out.println("result.getStatus() = " + result.getStatus());
        switch (result.getStatus()) {
            case "NONE":
                try {
                    System.out.println("UserService.postUserLogin2");
                    // 암호화
                    String jwt = jwtService.createJwt(postLoginReq.getUserId());
                    userDao.postUserLogin(postLoginReq);

                    return new PostLoginRes(jwt, "ONGOING");
                } catch (Exception exception) {
                    throw new BaseException(DATABASE_ERROR);
                }
            case "ACTIVE":
            case "ONGOING":
                return result;
        }
        return null;
    }
}
