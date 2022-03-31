package com.umc.footprint.src.users;

import com.umc.footprint.config.BaseException;
import com.umc.footprint.config.EncryptProperties;
import com.umc.footprint.src.walks.WalkDao;

import com.umc.footprint.src.users.model.GetUserTodayRes;
import com.umc.footprint.utils.AES128;
import com.umc.footprint.utils.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import com.umc.footprint.src.users.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.umc.footprint.config.BaseResponseStatus.*;

@Slf4j
@Service
public class UserProvider {

    private final WalkDao walkDao;
    private final UserDao userDao;
    private final EncryptProperties encryptProperties;
    private final JwtService jwtService;

    @Autowired
    public UserProvider(WalkDao walkDao, UserDao userDao, EncryptProperties encryptProperties, JwtService jwtService) {
        this.walkDao = walkDao;
        this.userDao = userDao;
        this.encryptProperties = encryptProperties;
        this.jwtService = jwtService;
    }

    // 해당 userIdx를 갖는 오늘 산책 관련 정보 조회
    public GetUserTodayRes getUserToday(int userIdx) throws BaseException {

        GetUserTodayRes userTodayRes = userDao.getUserToday(userIdx);
        userTodayRes.setWalkTime(userTodayRes.getWalkTime()/60);

        log.debug("userTodayRes: {}", userTodayRes);

        return userTodayRes;
    }

    //yummy 5
    //월별 발자국(일기) 갯수 조회
    public List<GetFootprintCount> getMonthFootprints(int userIdx, int year, int month) throws BaseException {
        try {
            // User 테이블 validation
            boolean userExist = userDao.checkUser(userIdx, "User");
            if (userExist == false) {
                throw new BaseException(INVALID_USERIDX);
            }

            // 사용자 status 확인
            String status = userDao.getStatus(userIdx, "User");
            if (status.equals("INACTIVE")) {
                throw new BaseException(INACTIVE_USER);
            }
            else if (status.equals("BLACK")) {
                throw new BaseException(BLACK_USER);
            }

            // 유효한 날짜인지 확인 - year, month validation


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
            if (existUserResult == false) {
                List<GetUserDateRes> returnList = new ArrayList<>();
                return returnList;
            }

            // Validation 3. 해당 날짜에 User가 기록한 Walk가 있는지 확인
            int existUserDateResult = userDao.checkUserDateWalk(userIdx, date);
            if (existUserDateResult == 0) {
                List<GetUserDateRes> returnList = new ArrayList<>();
                return returnList;
            }

            List<GetUserDateRes> userDateRes = userDao.getUserDate(userIdx, date);


            return userDateRes;
        } catch(Exception exception){

            throw new BaseException(DATABASE_ERROR);
        }
    }


    //월별 달성률 및 누적 정보 조회 - yummy 4
    public GetMonthInfoRes getMonthInfoRes(int userIdx, int year, int month) throws BaseException {
        try {
            // User 테이블 validation
            boolean userExist = userDao.checkUser(userIdx, "User");
            if (userExist == false) {
                throw new BaseException(INVALID_USERIDX);
            }

            // 사용자 status 확인
            String status = userDao.getStatus(userIdx, "User");
            if (status.equals("INACTIVE")) {
                throw new BaseException(INACTIVE_USER);
            }
            else if (status.equals("BLACK")) {
                throw new BaseException(BLACK_USER);
            }

            // Goal 테이블 validation
            userExist = userDao.checkUser(userIdx, "Goal");
            if (userExist == false) { //사용자가 목표를 지정하지 않은 경우
                throw new BaseException(NOT_EXIST_USER_IN_GOAL);
            }

            // Walk 테이블 validation
            boolean userWalkExist = userDao.checkWalk(userIdx,year,month);

            GetMonthInfoRes getMonthInfoRes;
            if(userWalkExist == false) {
                List<String> getGoalDays = userDao.getUserGoalDays(userIdx);
                GetMonthTotal getMonthTotal =new GetMonthTotal(0,0,0);
                getMonthInfoRes = new GetMonthInfoRes(getGoalDays, null, getMonthTotal);
            } else {
                getMonthInfoRes = userDao.getMonthInfoRes(userIdx, year, month);
            }

            return getMonthInfoRes;
          } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }


    // 해당 userIdx를 갖는 유저의 정보 조회
    public GetUserRes getUser(int userIdx) throws BaseException {
        try {
            boolean userExist = userDao.checkUser(userIdx, "User");
            // 해당 유저가 존재하지 않음
            if (userExist == false) {
                throw new BaseException(INVALID_USERIDX);
            }

            // 유저 상태에 따른 validation
            String status = userDao.getStatus(userIdx, "User");
            if (status.equals("INACTIVE")) {
                throw new BaseException(INACTIVE_USER);
            }
            else if (status.equals("BLACK")) {
                throw new BaseException(BLACK_USER);
            }

            GetUserRes getUserRes = userDao.getUser(userIdx);
            getUserRes.setUsername(new AES128(encryptProperties.getKey()).decrypt(getUserRes.getUsername()));
            getUserRes.setEmail(new AES128(encryptProperties.getKey()).decrypt(getUserRes.getEmail()));
            return getUserRes;
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new BaseException(DATABASE_ERROR);
        }
    }


    // yummy 11
    // 사용자 전체 뱃지 조회 API
    public GetUserBadges getUserBadges(int userIdx) throws BaseException {
        try {
            GetUserBadges getUserBadges = userDao.getUserBadges(userIdx);

        for(int i=0;i<getUserBadges.getBadgeList().size();i++) {
            BadgeOrder badge = getUserBadges.getBadgeList().get(i);
            if(badge.getBadgeDate().equals("0")) {
                int badgeIdx = badge.getBadgeIdx();
                switch (badgeIdx) {
                    case 1:
                        badge.setBadgeOrder(0);
                        getUserBadges.getBadgeList().set(i, badge);
                        break;
                    case 2:
                        badge.setBadgeOrder(1);
                        getUserBadges.getBadgeList().set(i, badge);
                        ;
                        break;
                    case 3:
                        badge.setBadgeOrder(2);
                        getUserBadges.getBadgeList().set(i, badge);
                        break;
                    case 4:
                        badge.setBadgeOrder(3);
                        getUserBadges.getBadgeList().set(i, badge);
                        break;
                    case 5:
                        badge.setBadgeOrder(4);
                        getUserBadges.getBadgeList().set(i, badge);
                        break;
                    case 6:
                        badge.setBadgeOrder(5);
                        getUserBadges.getBadgeList().set(i, badge);
                        break;
                    case 7:
                        badge.setBadgeOrder(6);
                        getUserBadges.getBadgeList().set(i, badge);
                        break;
                    case 8:
                        badge.setBadgeOrder(7);
                        getUserBadges.getBadgeList().set(i, badge);
                        break;
                }
            } else {
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
                getUserBadges.getBadgeList().set(i,badge);
            }
        }
            return getUserBadges;
          } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }


    // 해당 userIdx를 갖는 "이번달" Goal 정보 조회
    public GetUserGoalRes getUserGoal(int userIdx) throws BaseException{
        try{
            GetUserGoalRes getUserGoalRes = userDao.getUserGoal(userIdx);

            // 요일별 인덱스 차이 해결을 위한 임시 코드
            List<Integer> dayIdxList = new ArrayList<>();
            for (Integer dayIdx: getUserGoalRes.getDayIdx()){
                if(dayIdx == 1)
                    dayIdxList.add(7);
                else
                    dayIdxList.add(dayIdx-1);
            }
            Collections.sort(dayIdxList);
            getUserGoalRes.setDayIdx(dayIdxList);
            //

            return getUserGoalRes;
        } catch (Exception exception){
            throw new BaseException(DATABASE_ERROR);
        }
    }

    // 해당 userIdx를 갖는 "다음달" Goal 정보 조회
    public GetUserGoalRes getUserGoalNext(int userIdx) throws BaseException{
        try{
            GetUserGoalRes getUserGoalRes = userDao.getUserGoalNext(userIdx);

            // 요일별 인덱스 차이 해결을 위한 임시 코드
            List<Integer> dayIdxList = new ArrayList<>();
            for (Integer dayIdx: getUserGoalRes.getDayIdx()){
                if(dayIdx == 1)
                    dayIdxList.add(7);
                else
                    dayIdxList.add(dayIdx-1);
            }
            Collections.sort(dayIdxList);
            getUserGoalRes.setDayIdx(dayIdxList);
            //

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
            tag = "#" + tag;
            String encryptedTag = new AES128(encryptProperties.getKey()).encrypt(tag);
            List<GetTagRes> getTagResult = userDao.getWalks(userIdx, encryptedTag);

            List<GetTagRes> decryptedTagResultList = new ArrayList<>();
            for (GetTagRes getTagRes : getTagResult) {
                GetTagRes decryptedGetTagRes = new GetTagRes();
                decryptedGetTagRes.setWalkAt(getTagRes.getWalkAt());

                List<SearchWalk> decryptedSearchWalkList = new ArrayList<>();

                for (SearchWalk walk : getTagRes.getWalks()) {
                    SearchWalk decryptedSearchWalk = new SearchWalk();

                    // 복호화된 태그 리스트 설정
                    List<String> decryptedHashtagList = new ArrayList<>();
                    for (String hashtag : walk.getHashtag()) {
                        String decryptedHashtag = new AES128(encryptProperties.getKey()).decrypt(hashtag);
                        decryptedHashtagList.add(decryptedHashtag);
                    }
                    decryptedSearchWalk.setHashtag(decryptedHashtagList);

                    // 복호화된 이미지 설정
                    String decryptedImage = new AES128(encryptProperties.getKey()).decrypt(walk.getUserDateWalk().getPathImageUrl());
                    decryptedSearchWalk.setUserDateWalk(new UserDateWalk(
                            walk.getUserDateWalk().getWalkIdx(),
                            walk.getUserDateWalk().getStartTime(),
                            walk.getUserDateWalk().getEndTime(),
                            decryptedImage
                    ));

                    decryptedSearchWalkList.add(decryptedSearchWalk);
                }

                decryptedGetTagRes.setWalks(decryptedSearchWalkList);

                decryptedTagResultList.add(decryptedGetTagRes);
            }
            
            return decryptedTagResultList;
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new BaseException(DATABASE_ERROR);
        }
    }

    //yummy 13
    // 이번달 사용자가 얻은 뱃지 조회 (PRO, LOVER, MASTER)
    public BadgeInfo getMonthlyBadgeStatus(int userIdx) throws BaseException {
        try {
            // 이전달 목표 설정 여부 확인
            if(!userDao.checkPrevGoalDay(userIdx)) {
                throw new BaseException(NOT_EXIST_USER_IN_PREV_GOAL);
            }

            BadgeInfo getBadgeInfo = userDao.getMonthlyBadgeStatus(userIdx);
            if(getBadgeInfo==null) {
                throw new BaseException(NO_MONTHLY_BADGE);
           }
            return getBadgeInfo;
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    // userId로 userIdx 추출
    public int getUserIdx(String userId) throws BaseException {
        try {
            return userDao.getUserIdx(userId);
        } catch (Exception exception) {
            throw new BaseException(NOT_EXIST_USER);
        }
    }

}
