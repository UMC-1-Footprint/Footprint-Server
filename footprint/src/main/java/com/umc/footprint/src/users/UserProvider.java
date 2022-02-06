package com.umc.footprint.src.users;

import com.umc.footprint.config.BaseException;
import com.umc.footprint.src.walks.WalkDao;

import com.umc.footprint.src.users.model.GetUserTodayRes;
import com.umc.footprint.utils.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import com.umc.footprint.src.users.model.*;

import java.util.List;

import static com.umc.footprint.config.BaseResponseStatus.*;

@Service
public class UserProvider {

    private final WalkDao walkDao;
    private final UserDao userDao;
    private final JwtService jwtService;

    @Autowired
    public UserProvider(WalkDao walkDao, UserDao userDao, JwtService jwtService) {
        this.walkDao = walkDao;
        this.userDao = userDao;
        this.jwtService = jwtService;
    }

    // 해당 userIdx를 갖는 오늘 산책 관련 정보 조회
    public GetUserTodayRes getUserToday(int userIdx) throws BaseException {

        GetUserTodayRes userTodayRes = userDao.getUserToday(userIdx);

        System.out.println("userTodayRes : " + userTodayRes);

        return userTodayRes;
    }


    //월별 발자국(일기) 갯수 조회
    public List<GetFootprintCount> getMonthFootprints(int userIdx, int year, int month) throws BaseException {
        try {
            List<GetFootprintCount> getMonthFootprints = userDao.getMonthFootprints(userIdx, year, month);
            return getMonthFootprints;
        } catch (Exception exception) {
          throw new BaseException(DATABASE_ERROR);
        }
    }
  

    // 해당 userIdx를 갖는 date의 산책 관련 정보 조회
    public List<GetUserDateRes> getUserDate(int userIdx, String date) throws BaseException {

        try {
            // Validation 2. Walk Table 안 존재하는 User인지 확인
            boolean existUserResult = userDao.checkUser(userIdx,"Walk");
            if (existUserResult == false)
                throw new BaseException(NOT_EXIST_USER_IN_WALK);

            // Validation 3. 해당 날짜에 User가 기록한 Walk가 있는지 확인
            int existUserDateResult = userDao.checkUserDateWalk(userIdx, date);
            if (existUserDateResult == 0)
                throw new BaseException(NO_EXIST_WALK);

            List<GetUserDateRes> userDateRes = userDao.getUserDate(userIdx, date);

            return userDateRes;
        } catch(Exception exception){

            throw new BaseException(DATABASE_ERROR);
        }
    }


    //월별 달성률 및 누적 정보 조회 - yummy 4
    public GetMonthInfoRes getMonthInfoRes(int userIdx, int year, int month) throws BaseException {
        try {
            boolean userExist = userDao.checkUser(userIdx, "User"); // User 테이블 validation
            if (userExist == false) {
                throw new BaseException(INVALID_USERIDX);
            }

            String status = userDao.getStatus(userIdx, "User"); // 사용자 status 확인
            if (status.equals("INACTIVE")) {
                throw new BaseException(INACTIVE_USER);
            }
            else if (status.equals("BLACK")) {
                throw new BaseException(BLACK_USER);
            }

            userExist = userDao.checkUser(userIdx, "Goal"); // Goal 테이블 validation
            if (userExist == false) {
                throw new BaseException(NOT_EXIST_USER_IN_GOAL);
            }

            userExist = userDao.checkUser(userIdx, "Walk"); // Walk 테이블 validation
            if (userExist == false) {
                throw new BaseException(NOT_EXIST_USER_IN_WALK);
            }

            GetMonthInfoRes getMonthInfoRes = userDao.getMonthInfoRes(userIdx, year, month);
            return getMonthInfoRes;
          } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }


    // 해당 userIdx를 갖는 User의 정보 조회
    public GetUserRes getUser(int userIdx) throws BaseException {
        try {
            boolean userExist = userDao.checkUser(userIdx, "User");
            if (userExist == false) {
                throw new BaseException(INVALID_USERIDX);
            }

            String status = userDao.getStatus(userIdx, "User");
            if (status.equals("INACTIVE")) {
                throw new BaseException(INACTIVE_USER);
            }
            else if (status.equals("BLACK")) {
                throw new BaseException(BLACK_USER);
            }
            GetUserRes getUserRes = userDao.getUser(userIdx);
            return getUserRes;
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new BaseException(DATABASE_ERROR);
        }
    }


    // yummy11
    // 사용자 전체 뱃지 조회 API
    public GetUserBadges getUserBadges(int userIdx) throws BaseException {
       // try {
            GetUserBadges getUserBadges = userDao.getUserBadges(userIdx);

            for(BadgeOrder badge : getUserBadges.getBadgeList()) {
                if(badge.getBadgeDate() == "0") {
                    switch (badge.getBadgeIdx()) {
                        case 1 :
                            badge.setBadgeOrder(0);
                            break;
                        case 2 :
                            badge.setBadgeOrder(1);
                            break;
                        case 3 :
                            badge.setBadgeOrder(2);
                            break;
                        case 4 :
                            badge.setBadgeOrder(3);
                            break;
                        case 5 :
                            badge.setBadgeOrder(4);
                            break;
                        case 6 :
                            badge.setBadgeOrder(5);
                            break;
                        case 7 :
                            badge.setBadgeOrder(6);
                            break;
                        case 8 :
                            badge.setBadgeOrder(7);
                            break;
                        default:
                            if(badge.getBadgeDate().charAt(5)=='1') {
                                if(badge.getBadgeDate().charAt(6)=='-') { //1월
                                    badge.setBadgeOrder(8);
                                }
                                if(badge.getBadgeDate().charAt(6)=='0') { //10월
                                    badge.setBadgeOrder(17);
                                }
                                if(badge.getBadgeDate().charAt(6)=='1') { //11월
                                    badge.setBadgeOrder(18);
                                }
                                if(badge.getBadgeDate().charAt(6)=='2') { //12월
                                    badge.setBadgeOrder(19);
                                }
                            }
                            if(badge.getBadgeDate().charAt(5)=='2') {
                                badge.setBadgeOrder(9);
                            }
                            if(badge.getBadgeDate().charAt(5)=='3') {
                                badge.setBadgeOrder(10);
                            }
                            if(badge.getBadgeDate().charAt(5)=='4') {
                                badge.setBadgeOrder(11);
                            }
                            if(badge.getBadgeDate().charAt(5)=='5') {
                                badge.setBadgeOrder(12);
                            }
                            if(badge.getBadgeDate().charAt(5)=='6') {
                                badge.setBadgeOrder(13);
                            }
                            if(badge.getBadgeDate().charAt(5)=='7') {
                                badge.setBadgeOrder(14);
                            }
                            if(badge.getBadgeDate().charAt(5)=='8') {
                                badge.setBadgeOrder(15);
                            }
                            if(badge.getBadgeDate().charAt(5)=='9') {
                                badge.setBadgeOrder(16);
                            }
                    }
                }
            }
            return getUserBadges;
          //} catch (Exception exception) {
          //  throw new BaseException(DATABASE_ERROR);
        //}
    }


    // 해당 userIdx를 갖는 "이번달" Goal 정보 조회
    public GetUserGoalRes getUserGoal(int userIdx) throws BaseException{
        try{
            GetUserGoalRes getUserGoalRes = userDao.getUserGoal(userIdx);
            return getUserGoalRes;
        } catch (Exception exception){
            throw new BaseException(DATABASE_ERROR);
        }
    }

    // 해당 userIdx를 갖는 "다음달" Goal 정보 조회
    public GetUserGoalRes getUserGoalNext(int userIdx) throws BaseException{
        try{
            GetUserGoalRes getUserGoalRes = userDao.getUserGoalNext(userIdx);
            return getUserGoalRes;
        } catch (Exception exception){
          throw new BaseException(DATABASE_ERROR);
        }
    }
  
    // 해당 userIdx를 갖는 User의 세부 정보 조회
    public GetUserInfoRes getUserInfo(int userIdx) throws BaseException {
        try {
            // 1. user 달성률 정보
            UserInfoAchieve userInfoAchieve = userDao.getUserInfoAchieve(userIdx);
            // 2. user 이번달 목표 정보
            GetUserGoalRes getUserGoalRes = userDao.getUserGoal(userIdx);
            // 3. user 통계 정보
            UserInfoStat userInfoStat = userDao.getUserInfoStat(userIdx);

            // 4. 1+2+3
            return new GetUserInfoRes(userInfoAchieve,getUserGoalRes,userInfoStat);
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    // 해당 유저의 산책기록 중 태그를 포함하는 산책기록 조회
    public List<GetTagRes> getTagResult(int userIdx, String tag) throws BaseException {
        try {
            List<GetTagRes> getTagResult = userDao.getWalks(userIdx, tag);
            if (getTagResult.isEmpty()) { // 검색결과가 없음
                throw new BaseException(NO_EXIST_RESULT);
            }
            return getTagResult;
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    //yummy 13
    // 이번달 사용자가 얻은 뱃지 조회 (PRO, LOVER, MASTER)
    public BadgeInfo getMonthlyBadgeStatus(int userIdx) throws BaseException {
        try {
            BadgeInfo getBadgeInfo = userDao.getMonthlyBadgeStatus(userIdx);
            if(getBadgeInfo==null) {
                throw new BaseException(NO_MONTHLY_BADGE);
            }
            return getBadgeInfo;
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    // email을 통해 유저 중복 검사
    public PostLoginRes checkEmail(String email) throws BaseException {
        System.out.println("UserProvider.checkEmail1");
        try {
            if (userDao.checkEmail(email) == 1) {
                System.out.println("UserProvider.checkEmail2");
                // email로 userId랑 상태 추출
                PostLoginRes postLoginRes = userDao.getUserIdAndStatus(email);
                // userId 암호화
                String jwtId = jwtService.createJwt(postLoginRes.getJwtId());
                // response에 저장
                postLoginRes.setJwtId(jwtId);
                return postLoginRes;
            } else {
                return new PostLoginRes("", "NONE");
            }
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    // userId로 userIdx 추출
    public int getUserIdx(String userId) throws BaseException {
        try {
            System.out.println("UserProvider.getUserIdx");
            return userDao.getUserIdx(userId);
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }
}
