package com.umc.footprint.src.users;


import com.umc.footprint.config.BaseException;
import com.umc.footprint.config.EncryptProperties;
import com.umc.footprint.src.AwsS3Service;
import com.umc.footprint.src.users.model.*;

import com.umc.footprint.utils.AES128;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

import static com.umc.footprint.config.BaseResponseStatus.*;

@Slf4j
@Repository
public class UserDao {
    private JdbcTemplate jdbcTemplate;
    private EncryptProperties encryptProperties;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Autowired
    public UserDao(EncryptProperties encryptProperties){
        this.encryptProperties = encryptProperties;
    }

    /*
    *** [1] GET METHOD
    * */

    //월별 발자국(일기) 갯수 조회 - yummy 5
    public List<GetFootprintCount> getMonthFootprints(int userIdx, int year, int month) {
        String Query = "select day(startAt) as day, count(walkIdx) as walkCount from Walk\n" +
                "where userIdx=? and year(startAt)=? and month(startAt)=? and status='ACTIVE' group by day(startAt);";

        Object[] getMonthResParams = new Object[]{userIdx, year, month};
        return this.jdbcTemplate.query(Query,
                (rs, rowNum) -> new GetFootprintCount(
                        rs.getInt("day"),
                        rs.getInt("walkCount")), // RowMapper(위의 링크 참조): 원하는 결과값 형태로 받기
                getMonthResParams);
    }


    //월별 달성률 및 누적 정보 조회 - yummy 4
    public GetMonthInfoRes getMonthInfoRes(int userIdx, int year, int month) {
        // 사용자 목표 요일 조회
        List<String> goalDayList = getUserGoalDays(userIdx);

        //이번달 일별 달성률 조회(List)
        String getDayRateQuery = "select day(startAt) as day, sum(goalRate) as rate from Walk where userIdx=? and status = 'ACTIVE' and year(startAt)=? and month(startAt)=? group by day(startAt);";
        Object[] getDayRateParams = new Object[]{userIdx, year, month};
        List<GetDayRateRes> getDayRatesRes = this.jdbcTemplate.query(getDayRateQuery,
                (rs, rowNum) -> new GetDayRateRes(
                        rs.getInt("day"),
                        rs.getFloat("rate")),
                getDayRateParams);

        //사용자의 이번 달 누적 시간, 거리, 평균 칼로리
        String getMonthInfoQuery = "select sum((timestampdiff(second,startAt, endAt))) as monthTotalMin,\n" +
                "                sum(distance) as monthTotalDistance, sum(calorie) as monthPerCal\n" +
                "                from Walk where userIdx=? and status = 'ACTIVE' and year(startAt)=? and month(startAt)=?;";
        Object[] getMonthInfoParams = new Object[]{userIdx, year, month};
        GetMonthTotal getMonthTotal = this.jdbcTemplate.queryForObject(getMonthInfoQuery,
                (rs, rowNum) -> new GetMonthTotal(
                        rs.getInt("monthTotalMin"),
                        rs.getDouble("monthTotalDistance"),
                        rs.getInt("monthPerCal")),
                getMonthInfoParams);

        int dayCount = getDayRatesRes.toArray().length;
        getMonthTotal.avgCal(dayCount); // 산책 날짜 기준 평균 칼로리
        getMonthTotal.convertSecToMin();


        GetMonthInfoRes getMonthInfoRes = new GetMonthInfoRes(goalDayList, getDayRatesRes, getMonthTotal);
        return getMonthInfoRes;
    }


    public List<BadgeInfo> getBadgeList(int userIdx) {
        String getUserBadgesQuery = "select * from Badge where badgeIdx in " +
                "(select badgeIdx from UserBadge where userIdx=? and status='ACTIVE');";
        List<BadgeInfo> badgeInfoList = this.jdbcTemplate.query(getUserBadgesQuery,
                (rs, rowNum) -> new BadgeInfo(
                        rs.getInt("badgeIdx"),
                        rs.getString("badgeName"),
                        rs.getString("badgeUrl"),
                        rs.getString("badgeDate")),
                userIdx);

        return badgeInfoList;
    }
    public GetUserBadges getUserBadges(int userIdx) {
        //대표 뱃지 조회
        String getRepBadgeQuery = "select * from Badge where badgeIdx=(select badgeIdx from User where userIdx=?);";
        BadgeInfo repBadgeInfo = this.jdbcTemplate.queryForObject(getRepBadgeQuery,
                (rs,rowNum) -> new BadgeInfo(
                        rs.getInt("badgeIdx"),
                        rs.getString("badgeName"),
                        rs.getString("badgeUrl"),
                        rs.getString("badgeDate")), userIdx);

        //전체 뱃지 조회
        String getUserBadgesQuery = "select * from Badge where badgeIdx in " +
                "(select badgeIdx from UserBadge where userIdx=? and status='ACTIVE');";
        List<BadgeOrder> badgeInfoList = this.jdbcTemplate.query(getUserBadgesQuery,
                (rs, rowNum) -> new BadgeOrder(
                        rs.getInt("badgeIdx"),
                        rs.getString("badgeName"),
                        rs.getString("badgeUrl"),
                        rs.getString("badgeDate"),
                        0),
                userIdx);

        GetUserBadges getUserBadges = new GetUserBadges(repBadgeInfo, badgeInfoList);
        return getUserBadges;
    }

    //yummy
    // 사용자가 얻은 뱃지 등록
    public void postUserBadge(int userIdx, int badgeIdx) {
        //UserBadge 테이블에 얻은 뱃지 추가하기
        String insertBadgeQuery = "INSERT INTO UserBadge (userIdx, badgeIdx, status) SELECT ?, ?, 'ACTIVE'\n" +
                "FROM DUAL WHERE NOT EXISTS(SELECT userIdx, badgeIdx from UserBadge where userIdx=? and badgeIdx=? and status='ACTIVE');";
        Object[] insertBadgeParams = new Object[]{userIdx, badgeIdx, userIdx, badgeIdx};
        this.jdbcTemplate.update(insertBadgeQuery,insertBadgeParams);
    }


    // yummy 13
    // 이번 달에 사용자가 얻은 뱃지 조회 (PRO, LOVER, MASTER)
    public BadgeInfo getMonthlyBadgeStatus(int userIdx) {

        //저번달 기준
        LocalDate now = LocalDate.now();
        int rate = calcMonthGoalRate(userIdx, 1); //이전달 달성률

        //사용자 목표 요일 get
        String getGoalDayQuery = "SELECT sun, mon, tue, wed, thu, fri, sat FROM GoalDay WHERE userIdx = ? and " +
                "MONTH(createAt) = MONTH(DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 MONTH));";
        GetGoalDays getGoalDays = this.jdbcTemplate.queryForObject(getGoalDayQuery,
                (rs,rowNum) -> new GetGoalDays(
                        rs.getBoolean("sun"),
                        rs.getBoolean("mon"),
                        rs.getBoolean("tue"),
                        rs.getBoolean("wed"),
                        rs.getBoolean("thu"),
                        rs.getBoolean("fri"),
                        rs.getBoolean("sat")), userIdx);

        log.debug("getGoalDays: {}", getGoalDays.toString());
        double count = 0; //저번달의 설정한 목표요일 전체 횟수
        int year = now.getYear();
        int month = now.getMonthValue();

        if(month == 1) { //지금이 1월이면 저번달은 작년 12월로 조회
            year--;
            month = 12;
        }

        String badgeDate = Integer.toString(year)+"-"+Integer.toString(month)+"-";

        month--; //0-11월로 조회

        Calendar cal = new GregorianCalendar(year, month, 1);
        do {
            int day = cal.get(Calendar.DAY_OF_WEEK);
            if (day == Calendar.SUNDAY && getGoalDays.isSun()==true) {
                count++;
            }
            else if (day == Calendar.MONDAY && getGoalDays.isMon()==true) {
                count++;
            }
            else if (day == Calendar.TUESDAY && getGoalDays.isTue()==true) {
                count++;
            }
            else if (day == Calendar.WEDNESDAY && getGoalDays.isWed()==true) {
                count++;
            }
            else if (day == Calendar.THURSDAY && getGoalDays.isThu()==true) {
                count++;
            }
            else if (day == Calendar.FRIDAY && getGoalDays.isFri()==true) {
                count++;
            }
            else if (day == Calendar.SATURDAY && getGoalDays.isSat()==true) {
                count++;
            }
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }  while (cal.get(Calendar.MONTH) == month);

        // 이번달 산책 날짜의 요일을 받기
        String walkCountQuery = "select dayofweek(startAt) as day from Walk where userIdx=? and status = 'ACTIVE' " +
                "and MONTH(startAt) = MONTH(DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 MONTH));";
        List<Object> walkDays = this.jdbcTemplate.query(walkCountQuery,
                (rs,rowNum) -> (rs.getInt("day")), userIdx);


        // 목표 요일이랑 비교하기
        // 산책 요일이 목표 요일이랑 같으면 count
        double walkCount = 0; //목표요일에 산책한 횟수

        for(int i=0;i<walkDays.size();i++) {
            int walkday = (int) walkDays.get(i);
            switch (walkday) {
                case 1:
                    if(getGoalDays.isSun()==true) {
                        walkCount++;
                    }
                    break;
                case 2:
                    if(getGoalDays.isMon()==true) {
                        walkCount++;
                    }
                    break;
                case 3:
                    if(getGoalDays.isTue()==true) {
                        walkCount++;
                    }
                    break;
                case 4:
                    if(getGoalDays.isWed()==true) {
                        walkCount++;
                    }
                    break;
                case 5:
                    if(getGoalDays.isThu()==true) {
                        walkCount++;
                    }
                    break;
                case 6:
                    if(getGoalDays.isFri()==true) {
                        walkCount++;
                    }
                    break;
                case 7:
                    if(getGoalDays.isSat()==true) {
                        walkCount++;
                    }
                    break;
            }
        }
        double walkRate;
        if(walkCount==0) {
            walkRate=0;
        } else {
            walkRate=(walkCount/count) * 100;
        }

        log.debug("walkCout: {}", walkCount);
        log.debug("count: {}", count);
        log.debug("walkRate: {}", walkRate);
        log.debug("rate: {}", rate);
        /*
         * MASTER - 목표 요일 중 80% 이상 / 달성률 90%
         * PRO - 목표 요일 중 50% 이상 / 달성률 70%
         * LOVER - 목표 요일 고려 안함 / 달성률 50%
         * */
        int badgeNum = -1;

        if(rate >= 50) {
            //LOVER - badgeIdx 0
            badgeNum=0;
        }
        if(rate >= 70 && walkRate >= 50) {
            //PRO - badgeIdx 1
            badgeNum=1;
        }
        if(rate >= 90 && walkRate >= 80) {
            //MASTER - badgeIdx 2
            badgeNum=2;
        }


        if(badgeNum == -1) { //이번 달에 획득한 뱃지가 없는 경우
            return null;
        }

        badgeDate += badgeNum;

        //Badge 테이블에서 badgeIdx 얻어오기
        String getBadgeInfoQuery = "select * from Badge where badgeDate=?;";
        BadgeInfo badgeInfo = this.jdbcTemplate.queryForObject(getBadgeInfoQuery,
                (rs,rowNum) -> new BadgeInfo(
                        rs.getInt("badgeIdx"),
                        rs.getString("badgeName"),
                        rs.getString("badgeUrl"),
                        rs.getString("badgeDate")), badgeDate);

        //사용자에게 다른 달 뱃지 있는지 확인 - 한 달에 프로, 러버, 마스터 중 하나만 ACTIVE한 상태여야 함!


        //UserBadge 테이블에 얻은 뱃지 추가하기
        postUserBadge(userIdx, badgeInfo.getBadgeIdx());

        return badgeInfo;
    }

    // 해당 userIdx를 갖는 오늘 산책 관련 정보 조회
    public GetUserTodayRes getUserToday(int userIdx){
        String getUserTodayQuery = "SELECT SUM(W.goalRate) as goalRate, G.walkGoalTime, " +
                "SUM(TIMESTAMPDIFF(second ,W.startAt,W.endAt)) as walkTime, " +
                "SUM(W.distance) as distance, " +
                "SUM(W.calorie) as calorie " +
                "FROM Walk W " +
                "INNER JOIN (SELECT useridx, walkGoalTime FROM Goal WHERE useridx = ? and MONTH(createAt) = MONTH(NOW())) as G " +
                "ON W.userIdx = G.userIdx " +
                "WHERE W.userIdx = ? AND DATE(W.startAt) = DATE(NOW()) AND W.status = 'ACTIVE' " +
                "GROUP BY G.walkGoalTime ";
        int getUserIdxParam = userIdx;

        try {
            GetUserTodayRes getUserTodayRes =  this.jdbcTemplate.queryForObject(getUserTodayQuery,
                    (rs, rowNum) -> new GetUserTodayRes(
                            rs.getFloat("goalRate"),
                            rs.getInt("walkGoalTime"),
                            rs.getInt("walkTime"),
                            rs.getDouble("distance"),
                            rs.getInt("calorie")
                    ), getUserIdxParam, getUserIdxParam);

            getUserTodayRes.setGoalRate((float)Math.floor(getUserTodayRes.getGoalRate()));
            getUserTodayRes.setDistance(Math.floor(getUserTodayRes.getDistance()*10)/10.0);

            return getUserTodayRes;

        } catch(EmptyResultDataAccessException e){
            String getWalkGoalQuery = "SELECT walkGoalTime FROM Goal WHERE useridx = ? and MONTH(createAt) = MONTH(NOW())";
            int walkGoalTime = this.jdbcTemplate.queryForObject(getWalkGoalQuery,int.class,userIdx);

            return new GetUserTodayRes(0,walkGoalTime,0,0,0);
        }
    }

// 해당 userIdx를 갖는 date의 산책 관련 정보 조회
    public List<GetUserDateRes> getUserDate(int userIdx, String date) throws BaseException{

        // 1. Walk 정보 가져오기
        String getUserDateWalkQuery = "SELECT walkIdx, DATE_FORMAT(startAt,'%H:%i') as startTime, DATE_FORMAT(endAt,'%H:%i') as endTime, pathImageUrl " +
                "FROM Walk " +
                "WHERE userIdx = ? and DATE(startAt) = DATE(?) and status = 'ACTIVE' " +
                "ORDER BY startAt ";

        List<UserDateWalk> userDateWalkInfo = this.jdbcTemplate.query(getUserDateWalkQuery, (rs, rowNum) -> new UserDateWalk(
                rs.getInt("walkIdx"),
                rs.getString("startTime"),
                rs.getString("endTime"),
                rs.getString("pathImageUrl")
        ),userIdx,date);


        // 2-1. Hashtag 정보 가져오기
        String getHashtagQuery = "SELECT SF.walkIdx, H.hashtag " +
                "FROM Tag T " +
                "    INNER JOIN (SELECT F.walkIdx, F.footprintIdx " +
                "                FROM Footprint F " +
                "                INNER JOIN (SELECT walkIdx FROM Walk W WHERE DATE (startAt) = DATE (?) and userIdx = ? and status = 'ACTIVE' ) as W " +
                "                ON F.walkIdx = W.walkIdx) as SF " +
                "        ON T.footprintIdx = SF.footprintIdx " +
                "    INNER JOIN Hashtag H " +
                "        ON T.hashtagIdx = H.hashtagIdx " +
                "WHERE status = 'ACTIVE' ";

        List<Hashtag> entireHashtag = this.jdbcTemplate.query(getHashtagQuery, (rs, rowNum) -> new Hashtag(
                rs.getInt("walkIdx"),
                rs.getString("hashtag")
        ),date,userIdx);

        List<GetUserDateRes> getUserDateRes = new ArrayList<>();
        List<ArrayList<String>> hashtagList = new ArrayList<>();

        try {
            for (UserDateWalk walk : userDateWalkInfo) {
                walk.setPathImageUrl(new AES128(encryptProperties.getKey()).decrypt(walk.getPathImageUrl()));
                hashtagList.add(new ArrayList<>());
                for (Hashtag tag : entireHashtag) {
                    if (walk.getWalkIdx() == tag.getWalkIdx()) {
                        // hashtagList.get(hashtagList.size() - 1).add(tag.getHashtag());
                        hashtagList.get(hashtagList.size() - 1).add(new AES128(encryptProperties.getKey()).decrypt(tag.getHashtag()));
                    }
                }
                getUserDateRes.add(new GetUserDateRes(walk, hashtagList.get(hashtagList.size() - 1)));
            }
        } catch(Exception exception){
            throw new BaseException(DATABASE_ERROR);
        }


        for (GetUserDateRes userDateRes : getUserDateRes) {
            String getWalkIdxQuery = "SELECT count(walkIdx)+1 as walkIdx FROM Walk WHERE userIdx = ? and status = 'ACTIVE' and startAt < (SELECT startAt FROM Walk WHERE walkIdx = ? and status = 'ACTIVE')";

            int getWalkIdx = this.jdbcTemplate.queryForObject(getWalkIdxQuery, int.class, userIdx, userDateRes.getUserDateWalk().getWalkIdx());
            userDateRes.getUserDateWalk().setWalkIdx(getWalkIdx);
        }

        return getUserDateRes;
    }

    // 해당 userIdx를 갖는 유저 정보 조회
    public GetUserRes getUser(int userIdx) {
        int badgeIdx = this.jdbcTemplate.queryForObject("select badgeIdx from User where userIdx=?",
                int.class, userIdx);
        if (!badgeCheck(badgeIdx)) { // 유저 대표 뱃지 idx가 0일 경우
            String getUserQuery = "select userIdx, nickname, username, email, status, birth, sex, height, weight,\n" +
                    "                    (select count(*) from Walk where userIdx=? and status ='ACTIVE')+1 as walkNumber\n" +
                    "                    from User where userIdx=?";

            return this.jdbcTemplate.queryForObject(getUserQuery,
                    (rs, rowNum) -> new GetUserRes(
                            rs.getInt("userIdx"),
                            rs.getString("nickname"),
                            rs.getString("username"),
                            rs.getString("email"),
                            rs.getString("status"),
                            0,
                            "",
                            rs.getTimestamp("birth"),
                            rs.getString("sex"),
                            rs.getInt("height"),
                            rs.getInt("weight"),
                            rs.getInt("walkNumber")
                    ),
                    userIdx, userIdx);
        }
        else { // 유저 대표 뱃지 idx가 0이 아닐 경우
            String getUserQuery = "select userIdx, nickname, username, email, status, User.badgeIdx, badgeUrl, birth, sex, height, weight,\n" +
                    "       (select count(*) from Walk where userIdx=? and status ='ACTIVE')+1 as walkNumber\n" +
                    "from User inner join Badge B on User.badgeIdx = B.badgeIdx where userIdx=?";

            return this.jdbcTemplate.queryForObject(getUserQuery,
                    (rs, rowNum) -> new GetUserRes(
                            rs.getInt("userIdx"),
                            rs.getString("nickname"),
                            rs.getString("username"),
                            rs.getString("email"),
                            rs.getString("status"),
                            rs.getInt("badgeIdx"),
                            rs.getString("badgeUrl"),
                            rs.getTimestamp("birth"),
                            rs.getString("sex"),
                            rs.getInt("height"),
                            rs.getInt("weight"),
                            rs.getInt("walkNumber")
                    ),
                    userIdx, userIdx);
        }
    }

    // 해당 userIdx를 갖는 유저의 달성정보 조회
    public UserInfoAchieve getUserInfoAchieve(int userIdx){

        /*
         *   [ 1. 오늘 목표 달성률 계산 ] = todayGoalRate
         * */
        String getUserTodayGoalRateQuery = "SELECT IFNULL(SUM(goalRate),0) FROM Walk WHERE userIdx = ? and status = 'ACTIVE' and DATE(startAt) = DATE(NOW())";
        int todayGoalRate = this.jdbcTemplate.queryForObject(getUserTodayGoalRateQuery,int.class,userIdx);


        /*
         *   [ 2. 이번달 목표 달성률 계산 ] = monthGoalRate
         * */

        // method monthlyGoalRate(userIdx,0) 호출
        int monthGoalRate = calcMonthGoalRate(userIdx,0);

        /*
         *   [ 3. 산책 횟수 계산 ] = userWalkCount
         * */

        String getUserWalkCountQuery = "SELECT COUNT(*) as walkCount FROM Walk WHERE userIdx = ? and status = 'ACTIVE' ";
        int userWalkCount = this.jdbcTemplate.queryForObject(getUserWalkCountQuery,int.class,userIdx);


        return new UserInfoAchieve(todayGoalRate,monthGoalRate,userWalkCount);
    }



    // 유저 통계치 정보 제공
    public UserInfoStat getUserInfoStat(int userIdx){

        // [ 1. 이전 3달 기준 요일별 산책 비율 ] = List<String> mostWalkDay & List<Double> userWeekDayRate

        // 1-1. 이전 3달 기준 요일별 산책 수 확인
        List<Integer> userWeekDayCount = new ArrayList<>();
        List<Double> userWeekDayRate = new ArrayList<>();

        String getUserSunCountQuery = "SELECT count(*) FROM Walk Where userIdx = ? and status = 'ACTIVE' and  WEEKDAY(startAt) = 6 and startAt > DATE_SUB(DATE(CURRENT_TIMESTAMP), INTERVAL 3 MONTH )";
        userWeekDayCount.add(this.jdbcTemplate.queryForObject(getUserSunCountQuery,int.class,userIdx));

        String getUserMonCountQuery = "SELECT count(*) FROM Walk Where userIdx = ? and status = 'ACTIVE' and WEEKDAY(startAt) = 0 and startAt > DATE_SUB(DATE(CURRENT_TIMESTAMP), INTERVAL 3 MONTH )";
        userWeekDayCount.add(this.jdbcTemplate.queryForObject(getUserMonCountQuery,int.class,userIdx));

        String getUserTueCountQuery = "SELECT count(*) FROM Walk Where userIdx = ? and status = 'ACTIVE' and WEEKDAY(startAt) = 1 and startAt > DATE_SUB(DATE(CURRENT_TIMESTAMP), INTERVAL 3 MONTH )";
        userWeekDayCount.add(this.jdbcTemplate.queryForObject(getUserTueCountQuery,int.class,userIdx));

        String getUserWedCountQuery = "SELECT count(*) FROM Walk Where userIdx = ? and status = 'ACTIVE' and WEEKDAY(startAt) = 2 and startAt > DATE_SUB(DATE(CURRENT_TIMESTAMP), INTERVAL 3 MONTH )";
        userWeekDayCount.add(this.jdbcTemplate.queryForObject(getUserWedCountQuery,int.class,userIdx));

        String getUserThuCountQuery = "SELECT count(*) FROM Walk Where userIdx = ? and status = 'ACTIVE' and WEEKDAY(startAt) = 3 and startAt > DATE_SUB(DATE(CURRENT_TIMESTAMP), INTERVAL 3 MONTH )";
        userWeekDayCount.add(this.jdbcTemplate.queryForObject(getUserThuCountQuery,int.class,userIdx));

        String getUserFriCountQuery = "SELECT count(*) FROM Walk Where userIdx = ? and status = 'ACTIVE' and WEEKDAY(startAt) = 4 and startAt > DATE_SUB(DATE(CURRENT_TIMESTAMP), INTERVAL 3 MONTH )";
        userWeekDayCount.add(this.jdbcTemplate.queryForObject(getUserFriCountQuery,int.class,userIdx));

        String getUserSatCountQuery = "SELECT count(*) FROM Walk Where userIdx = ? and status = 'ACTIVE' and WEEKDAY(startAt) = 5 and startAt > DATE_SUB(DATE(CURRENT_TIMESTAMP), INTERVAL 3 MONTH )";
        userWeekDayCount.add(this.jdbcTemplate.queryForObject(getUserSatCountQuery,int.class,userIdx));

        // 1-2. 이전 3달 기준 전체 산책 수 구하기
        int entireCount = 0;
        for(Integer dayCount : userWeekDayCount){
            entireCount += dayCount;
        }

        // 1-3. 가장 산책이 많은 요일 추출 (동일 max 존재시 둘다 return) = List<String> mostWalkDay
        List<String> mostWalkDay = new ArrayList<>();

        if(entireCount == 0) // 최근 3개월간 산책 기록이 없을때
            mostWalkDay.add("최근 3개월간 산책을 하지 않았어요");
        else{
            int max = Collections.max(userWeekDayCount);

            for (int i=0; i<userWeekDayCount.size(); i++){
                if(userWeekDayCount.get(i) == max) {
                    switch (i) {
                        case 0:
                            mostWalkDay.add("일");
                            break;
                        case 1:
                            mostWalkDay.add("월");
                            break;
                        case 2:
                            mostWalkDay.add("화");
                            break;
                        case 3:
                            mostWalkDay.add("수");
                            break;
                        case 4:
                            mostWalkDay.add("목");
                            break;
                        case 5:
                            mostWalkDay.add("금");
                            break;
                        case 6:
                            mostWalkDay.add("토");
                            break;
                    }
                }
            }
        }

        // 1-4. 요일별 비율 구하기
        // *** 순서 : 일 월 화 수 목 금 토 ***
        for(Integer dayCount : userWeekDayCount){
            if (entireCount == 0)
                userWeekDayRate.add(0.0);
            else
                userWeekDayRate.add(dayCount/(double)entireCount*100);
        }

        // [ 2. 이전 6달 범위 월별 산책 횟수 ] = thisMonthWalkCount + List<Integer> monthlyWalkCount
        // List 순서 : -6달 , -5달 , ... , 전달 , 이번달 (총 7개 element)

        // 2-1. 이번달 산책 횟수 가져오기
        String userThisMonthWalkCountQuery = "SELECT count(*) FROM Walk WHERE userIdx = ? and status = 'ACTIVE' and MONTH(startAt) = MONTH(CURRENT_TIMESTAMP)";
        int thisMonthWalkCount = this.jdbcTemplate.queryForObject(userThisMonthWalkCountQuery, int.class, userIdx);

        // 2-2. 이전 6달 범위 유저 월별 산책 횟수 가져오기
        String userMonthlyWalkCountQuery = "SELECT count(*) FROM Walk WHERE userIdx = ? and status = 'ACTIVE' and MONTH(startAt) = MONTH(DATE_SUB(CURRENT_TIMESTAMP, INTERVAL ? MONTH))";

        List<Integer> monthlyWalkCount = new ArrayList<>();
        for(int i=6; i>=0; i--)
            monthlyWalkCount.add(this.jdbcTemplate.queryForObject(userMonthlyWalkCountQuery, int.class, userIdx, i));

        // [ 3. 이전 5달 범위 월별 달성률 & 평균 달성률 ] = List<Integer>monthlyGoalRate + avgGoalRate
        // List 순서 : 평균, -5달 , ... , 전달 , 이번달 (총 7개 element)
        List<Integer> monthlyGoalRate = new ArrayList<>();
        int sumGoalRate = 0;

        // 현재 달 + 이전 5달 범위의 월별 달성률 계산 by getMonthGoalRate()
        for(int i=5; i>=0 ; i--) {
            int goalRate = calcMonthGoalRate(userIdx, i); // Method getMonthGoalRate
            if (goalRate > 100) // 100 초과시 100으로 저장
                goalRate = 100;
            monthlyGoalRate.add(goalRate);
            sumGoalRate += monthlyGoalRate.get(5-i);
        }

        int avgGoalRate = (int)((double)sumGoalRate / 6);

        // monthlyGoalRate index 0에 avgGoalRate 추가
        monthlyGoalRate.add(0,avgGoalRate);

        return new UserInfoStat(mostWalkDay,userWeekDayRate,thisMonthWalkCount,monthlyWalkCount,monthlyGoalRate.get(6),monthlyGoalRate);

    }

    // 해당 userIdx를 갖는 유저의 "이번달" 목표 조회
    public GetUserGoalRes getUserGoal(int userIdx) throws BaseException {

        // Validation 1. Goal Table에 없는 userIdx인지 확인
        List<ExistUser> existUserIdx = this.jdbcTemplate.query("SELECT userIdx FROM Goal WHERE userIdx = ? ",
                (rs, rowNum) -> new ExistUser(rs.getInt("userIdx")),userIdx);
        if(existUserIdx.size() == 0)
            throw new BaseException(INVALID_USERIDX);

        // 1. 이번달 정보 구하기
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        DateFormat df = new SimpleDateFormat("yyyy년 MM월");
        String month = df.format(cal.getTime());


        // 2-1. get UserGoalDay
        String getUserGoalDayQuery = "SELECT sun, mon, tue, wed, thu, fri, sat FROM GoalDay WHERE userIdx = ? and MONTH(createAt) = MONTH(NOW())";
        UserGoalDay userGoalDay = this.jdbcTemplate.queryForObject(getUserGoalDayQuery,
                (rs,rowNum) -> new UserGoalDay(
                        rs.getBoolean("sun"),
                        rs.getBoolean("mon"),
                        rs.getBoolean("tue"),
                        rs.getBoolean("wed"),
                        rs.getBoolean("thu"),
                        rs.getBoolean("fri"),
                        rs.getBoolean("sat")
                ),userIdx);

        // 2-2. List<Integer> 형태로 변형
        List<Integer> dayIdx = new ArrayList<>();
        if(userGoalDay.isSun() == true)
            dayIdx.add(1);
        if(userGoalDay.isMon() == true)
            dayIdx.add(2);
        if(userGoalDay.isTue() == true)
            dayIdx.add(3);
        if(userGoalDay.isWed() == true)
            dayIdx.add(4);
        if(userGoalDay.isThu() == true)
            dayIdx.add(5);
        if(userGoalDay.isFri() == true)
            dayIdx.add(6);
        if(userGoalDay.isSat() == true)
            dayIdx.add(7);

        // 3. get UserGoalTime
        String getUserGoalTimeQuery = "SELECT walkGoalTime, walkTimeSlot FROM Goal WHERE userIdx = ? and MONTH(createAt) = MONTH(NOW())";
        UserGoalTime userGoalTime = this.jdbcTemplate.queryForObject(getUserGoalTimeQuery,
                (rs,rowNum) -> new UserGoalTime(
                        rs.getInt("walkGoalTime"),
                        rs.getInt("walkTimeSlot")
                ), userIdx);

        boolean goalNextModified = checkGoalModified(userIdx);

        // 4. GetUserGoalRes에 dayIdx 와 userGoalTime 합침
        return new GetUserGoalRes(month,dayIdx,userGoalTime,goalNextModified);

    }

    // 해당 userIdx를 갖는 유저의 "다음달" 목표 조회
    public GetUserGoalRes getUserGoalNext(int userIdx) throws BaseException {

        // Validation 1. Goal Table에 없는 userIdx인지 확인
        List<ExistUser> existUserIdx = this.jdbcTemplate.query("SELECT userIdx FROM GoalNext WHERE userIdx = ? ",
                (rs, rowNum) -> new ExistUser(rs.getInt("userIdx")),userIdx);
        if(existUserIdx.size() == 0)
            throw new BaseException(INVALID_USERIDX);

        // 1. 다음달 정보 구하기
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.MONTH,1);
        DateFormat df = new SimpleDateFormat("yyyy년 MM월");
        String month = df.format(cal.getTime());


        // 2-1. get UserGoalDay
        String getUserGoalDayNextQuery = "SELECT sun, mon, tue, wed, thu, fri, sat FROM GoalDayNext WHERE userIdx = ?";
        UserGoalDay userGoalDay = this.jdbcTemplate.queryForObject(getUserGoalDayNextQuery,
                (rs,rowNum) -> new UserGoalDay(
                        rs.getBoolean("sun"),
                        rs.getBoolean("mon"),
                        rs.getBoolean("tue"),
                        rs.getBoolean("wed"),
                        rs.getBoolean("thu"),
                        rs.getBoolean("fri"),
                        rs.getBoolean("sat")
                ),userIdx);

        // 2-2. List<Integer> 형태로 변형
        List<Integer> dayIdx = new ArrayList<>();
        if(userGoalDay.isSun() == true)
            dayIdx.add(1);
        if(userGoalDay.isMon() == true)
            dayIdx.add(2);
        if(userGoalDay.isTue() == true)
            dayIdx.add(3);
        if(userGoalDay.isWed() == true)
            dayIdx.add(4);
        if(userGoalDay.isThu() == true)
            dayIdx.add(5);
        if(userGoalDay.isFri() == true)
            dayIdx.add(6);
        if(userGoalDay.isSat() == true)
            dayIdx.add(7);

        // 3. get UserGoalTime
        String getUserGoalTimeNextQuery = "SELECT walkGoalTime, walkTimeSlot FROM GoalNext WHERE userIdx = ?";
        UserGoalTime userGoalTime = this.jdbcTemplate.queryForObject(getUserGoalTimeNextQuery,
                (rs,rowNum) -> new UserGoalTime(
                        rs.getInt("walkGoalTime"),
                        rs.getInt("walkTimeSlot")
                ), userIdx);

        boolean goalNextModified = checkGoalModified(userIdx);

        // 4. GetUserGoalRes에 dayIdx 와 userGoalTime 합침
        return new GetUserGoalRes(month,dayIdx,userGoalTime,goalNextModified);
    }


    // 태그 검색 결과 조회
    public List<GetTagRes> getWalks(int userIdx, String tag) {
        // 산책 날짜
        String getWalkAtQuery = "select distinct cast(date_format(endAt, '%Y.%m.%d') as char(10)) from Hashtag\n" +
                "inner join Tag T on Hashtag.hashtagIdx = T.hashtagIdx\n" +
                "inner join Footprint F on T.footprintIdx = F.footprintIdx\n" +
                "inner join Walk W on F.walkIdx = W.walkIdx\n" +
                "where W.walkIdx in (\n" + // 해당 태그를 가지고 있는 산책 기록의 인덱스들
                "    select walkIdx from Hashtag\n" +
                "    inner join Tag T on Hashtag.hashtagIdx = T.hashtagIdx\n" +
                "    inner join Footprint F on T.footprintIdx = F.footprintIdx\n" +
                "    where hashtag=? and T.status = ?" +
                "    ) and W.userIdx=? and T.status=?" +
                "order by endAt desc";

        List<String> walkAtList = jdbcTemplate.queryForList(getWalkAtQuery, String.class, tag, "ACTIVE", userIdx, "ACTIVE");
        log.debug("walkAtList: {}", walkAtList);
        List<GetTagRes> result = new ArrayList<>(); // 최종 출력 값을 담을 리스트

        for(String walkAt : walkAtList) {
            // String에 요일 추가
            String getDayQuery = "select case WEEKDAY(?)\n" +
                    "    when '0' then '월'\n" +
                    "    when '1' then '화'\n" +
                    "    when '2' then '수'\n" +
                    "    when '3' then '목'\n" +
                    "    when '4' then '금'\n" +
                    "    when '5' then '토'\n" +
                    "    when '6' then '일'\n" +
                    "    end as dayofweek";
            String day = jdbcTemplate.queryForObject(getDayQuery, String.class, walkAt);
            String walkAtResult = walkAt + " " + day;

            // 태그를 가지고 있는 산책 기록 인덱스 조회
            String walkIdxQuery = "select F.walkIdx from Hashtag\n" +
                    "    inner join Tag T on Hashtag.hashtagIdx = T.hashtagIdx\n" +
                    "    inner join Footprint F on T.footprintIdx = F.footprintIdx\n" +
                    "    inner join Walk W on F.walkIdx = W.walkIdx\n" +
                    "    where hashtag=?\n" +
                    "    and cast(date_format(endAt, '%Y.%m.%d') as char(10))=? and T.status=?";
            List<Integer> walkIdxList = jdbcTemplate.queryForList(walkIdxQuery, int.class, tag, walkAt, "ACTIVE");
            log.debug("walkIdxList: {}", walkIdxList);
            List<SearchWalk> walks = new ArrayList<>(); // 해당 날짜 + 해당 해시태그를 가지는 산책 기록 리스트
            for(Integer walkIdx : walkIdxList) {
                // 산책 기록 하나 조회
                String getUserDateWalkQuery = "select walkIdx, date_format(startAt, '%k:%i') as startTime, date_format(endAt, '%k:%i') as endTime, pathImageUrl\n" +
                        "from Walk W where W.walkIdx=? and W.status=?";
                String getWalkIdxQuery = "SELECT count(walkIdx)+1 as walkIdx FROM Walk WHERE userIdx = ? and startAt < (SELECT startAt FROM Walk WHERE walkIdx = ? and status='ACTIVE') and status='ACTIVE'";
                int finalWalkIdx = this.jdbcTemplate.queryForObject(getWalkIdxQuery, int.class, userIdx, walkIdx);
                UserDateWalk userDateWalk = this.jdbcTemplate.queryForObject(getUserDateWalkQuery,
                        (rs, rowNum)-> new UserDateWalk(
                                finalWalkIdx,
                                rs.getString("startTime"),
                                rs.getString("endTime"),
                                rs.getString("pathImageUrl")
                        )
                        , walkIdx, "ACTIVE"
                );

                SearchWalk walk = new SearchWalk(userDateWalk, getTagList(walkIdx));
                walks.add(walk);
            }

            // 2022.01.07 > 2022. 1. 7 의 형식으로 바꿈
           if(walkAtResult.charAt(5) == '0' || walkAt.charAt(8) == '0') {
                walkAtResult = walkAtResult.replace(".0", ". ");
            }
            GetTagRes getTagRes = new GetTagRes(walkAtResult, walks);
            result.add(getTagRes);
        }

        return result;
    }





    // 해당 userIdx를 갖는 Goal의 Time 정보 & GoalDay의 요일 정보 CREATE
    public int postGoal(int userIdx, PatchUserInfoReq patchUserInfoReq) throws BaseException {

        // Validation 7. Goal Table에 이미 존재하는 userIdx인지 확인
        if(checkUser(userIdx,"GoalNext") == true)
            throw new BaseException(EXIST_USER_ERROR);

        // 1. Goal Table에 userIdx에 맞는 walkGoalTime, walkTimeSlot Create
        int result1; // 1에서 update 확인용 result
        String createUserGoalTimeQuery = "INSERT INTO Goal (userIdx,walkGoalTime,walkTimeSlot) VALUES (?,?,?)";
        Object[] createUserGoalTimeParams = new Object[]{userIdx, patchUserInfoReq.getWalkGoalTime(), patchUserInfoReq.getWalkTimeSlot()};
        result1 = this.jdbcTemplate.update(createUserGoalTimeQuery,createUserGoalTimeParams);

        // 2. GoalDay Table에 sun~fri Create
        int result2; // 2에서 update 확인용 result
        Boolean[] days = {false,false,false,false,false,false,false};   // false로 초기화

        for (int dayIdx : patchUserInfoReq.getDayIdx()){ // dayIdx에 해당하는 요일만 true로 변경
            days[dayIdx-1] = true;
        }

        String createUserGoalDayQuery = "INSERT INTO GoalDay (userIdx,sun,mon,tue,wed,thu,fri,sat) VALUES (?,?,?,?,?,?,?,?)";
        Object[] createUserGoalDayParams = new Object[]{ userIdx, days[0], days[1], days[2], days[3], days[4], days[5], days[6]};
        result2 = this.jdbcTemplate.update(createUserGoalDayQuery,createUserGoalDayParams);

        if (result1 == 0 || result2 == 0) // result1 과 result2 중 하나라도 0이면(영향을 미치지 못함) 0 return
            return 0;

        return 1;
    }


    // 해당 userIdx를 갖는 GoalNext의 Time 정보 & GoalDayNext의 요일 정보 CREATE
    public int postGoalNext(int userIdx, PatchUserInfoReq patchUserInfoReq) throws BaseException {

        // Validation 7. GoalNext Table에 이미 존재하는 userIdx인지 확인
        if(checkUser(userIdx,"GoalNext") == true)
            throw new BaseException(EXIST_USER_ERROR);

        // 1. GoalNext Table에 userIdx에 맞는 walkGoalTime, walkTimeSlot Create
        int result1; // 1에서 update 확인용 result
        String createUserGoalTimeQuery = "INSERT INTO GoalNext (userIdx,walkGoalTime,walkTimeSlot) VALUES (?,?,?)";
        Object[] createUserGoalTimeNextParams = new Object[]{userIdx, patchUserInfoReq.getWalkGoalTime(), patchUserInfoReq.getWalkTimeSlot()};
        result1 = this.jdbcTemplate.update(createUserGoalTimeQuery,createUserGoalTimeNextParams);

        // 2. GoalDayNext Table에 sun~fri Create
        int result2; // 2에서 update 확인용 result
        Boolean[] days = {false,false,false,false,false,false,false};   // false로 초기화

        for (int dayIdx : patchUserInfoReq.getDayIdx()){ // dayIdx에 해당하는 요일만 true로 변경
            days[dayIdx-1] = true;
        }

        String createUserGoalDayQuery = "INSERT INTO GoalDayNext (userIdx,sun,mon,tue,wed,thu,fri,sat) VALUES (?,?,?,?,?,?,?,?)";
        Object[] createUserGoalDayNextParams = new Object[]{ userIdx, days[0], days[1], days[2], days[3], days[4], days[5], days[6]};
        result2 = this.jdbcTemplate.update(createUserGoalDayQuery,createUserGoalDayNextParams);

        if (result1 == 0 || result2 == 0) // result1 과 result2 중 하나라도 0이면(영향을 미치지 못함) 0 return
            return 0;

        return 1;
    }


    /*
     *** [3] PATCH METHOD
     * */

    //yummy 12
    //대표 뱃지 수정

    public BadgeInfo modifyRepBadge(int userIdx, int badgeIdx) {
        //TO DO : badgeIdx의 뱃지가 ACTIVE인지 validation 검사하기

        String patchRepBadgeQuery = "update User set badgeIdx=? where userIdx=?;";
        Object[] patchRepBadgeParams = new Object[]{badgeIdx, userIdx};
        this.jdbcTemplate.update(patchRepBadgeQuery, patchRepBadgeParams);

        String repBadgeInfoQuery = "select * from Badge where badgeIdx=(select badgeIdx from User where userIdx=?);";
        BadgeInfo patchRepBadgeInfo = this.jdbcTemplate.queryForObject(repBadgeInfoQuery,
                (rs, rowNum) -> new BadgeInfo(
                        rs.getInt("badgeIdx"),
                        rs.getString("badgeName"),
                        rs.getString("badgeUrl"),
                        rs.getString("badgeDate")
                ), userIdx);

        return patchRepBadgeInfo;
    }

    // Goal Table에 userIdx에 맞는 walkGoalTime, walkTimeSlot MODIFY
    public int modifyUserGoalTime(int userIdx, PatchUserGoalReq patchUserGoalReq){

        String modifyUserGoalTimeQuery = "UPDATE GoalNext SET walkGoalTime = ?, walkTimeSlot = ?, updateAt = NOW() WHERE userIdx = ?";
        Object[] modifyUserGoalTimeParams = new Object[]{ patchUserGoalReq.getWalkGoalTime(), patchUserGoalReq.getWalkTimeSlot(), userIdx };
        return this.jdbcTemplate.update(modifyUserGoalTimeQuery,modifyUserGoalTimeParams);

    }

    // Goal Table에 userIdx에 맞는 요일(dayIdx) MODIFY
    public int modifyUserGoalDay(int userIdx, PatchUserGoalReq patchUserGoalReq){

        Boolean[] days = {false,false,false,false,false,false,false};   // false로 초기화

        for (int dayIdx : patchUserGoalReq.getDayIdx()){ // dayIdx에 해당하는 요일만 true로 변경
            days[dayIdx-1] = true;
        }

        String modifyUserGoalDayQuery = "UPDATE GoalDayNext SET sun = ?, mon = ?, tue = ?, wed = ?, thu = ?, fri = ?, sat = ?, updateAt = NOW() WHERE userIdx = ?";
        Object[] modifyUserGoalDayParams = new Object[]{ days[0], days[1], days[2], days[3], days[4], days[5], days[6], userIdx };
        return this.jdbcTemplate.update(modifyUserGoalDayQuery,modifyUserGoalDayParams);

    }

    // 초기 유저 추가 정보를 User 테이블에 추가
    public int modifyUserInfo(int userIdx, PatchUserInfoReq patchUserInfoReq) {

        log.debug("userIdx: {}", userIdx);
        String patchUserInfoQuery = "UPDATE User SET nickname = ?, birth = ?, sex = ?, height = ?, weight = ?, status = ? WHERE userIdx = ?";
        Object[] patchUserInfoParams = new Object[]{patchUserInfoReq.getNickname(), patchUserInfoReq.getBirth(), patchUserInfoReq.getSex(),
                patchUserInfoReq.getHeight(), patchUserInfoReq.getWeight(), "ACTIVE",userIdx};

        return this.jdbcTemplate.update(patchUserInfoQuery, patchUserInfoParams);
    }

    /*
     *** [4] TOOL METHOD
     * */

    // 사용자의 목표 요일을 조회하는 Method
    public List<String> getUserGoalDays (int userIdx) {
        // 목표 요일 조회 (boolean)
        String getGoalDaysQuery = "select G.sun, G.mon, G.tue, G.wed, G.thu, G.fri, G.sat from GoalDay as G " +
                "where userIdx=? and MONTH(createAt)=MONTH(NOW());";
        GetGoalDays getGoalDays = this.jdbcTemplate.queryForObject(getGoalDaysQuery,
                (rs, rowNum) -> new GetGoalDays(
                        rs.getBoolean("sun"),
                        rs.getBoolean("mon"),
                        rs.getBoolean("tue"),
                        rs.getBoolean("wed"),
                        rs.getBoolean("thu"),
                        rs.getBoolean("fri"),
                        rs.getBoolean("sat")), userIdx);

        List<String> goalDayList = convertGoaldayBoolToString(getGoalDays);
        return goalDayList;
    }


    // GoalDay Table의 true인 요일 List<String>으로 return
    public List<String> convertGoaldayBoolToString(GetGoalDays getGoalDays) {
        List<String> goalDayString = new ArrayList<String>();

        if(getGoalDays.isSun()) {
            goalDayString.add("SUN");
        }
        if(getGoalDays.isMon()) {
            goalDayString.add("MON");
        }
        if(getGoalDays.isTue()) {
            goalDayString.add("TUE");
        }
        if(getGoalDays.isWed()) {
            goalDayString.add("WED");
        }
        if(getGoalDays.isThu()) {
            goalDayString.add("THU");
        }
        if(getGoalDays.isFri()) {
            goalDayString.add("FRI");
        }
        if(getGoalDays.isSat()) {
            goalDayString.add("SAT");
        }

        return goalDayString;
    }

    // true = 유저 있다 & false = 유저 없다.
    public boolean checkUser(int userIdx, String tableName) throws BaseException {
        log.debug("UserDao.checkUser");
        int userCount;
        if (tableName == "Walk")
            userCount = this.jdbcTemplate.queryForObject("SELECT count(*) FROM " + tableName + " WHERE userIdx = ? and status = 'ACTIVE'", int.class, userIdx);
        else
            userCount = this.jdbcTemplate.queryForObject("SELECT count(*) FROM " + tableName + " WHERE userIdx = ? ", int.class, userIdx);

        if (userCount != 0)
            return true;
        return false;
    }

    public boolean checkWalk(int userIdx, int year, int month){
        log.debug("UserDao.checkWalk");
        int walkCount = this.jdbcTemplate.queryForObject("SELECT count(*) " +
                "FROM Walk " +
                "WHERE userIdx = ? and status = 'ACTIVE' and year(startAt)=? and month(startAt)=?", int.class
                ,userIdx,year,month);
        if(walkCount != 0)
            return true;
        return false;
    }

    // 해당 날짜에 해당 유저가 산책을 했는지 확인
    public int checkUserDateWalk(int userIdx, String date){

        String checkUserWalkQuery = "SELECT userIdx FROM Walk WHERE DATE(startAt) = DATE(?) and userIdx = ? and status = 'ACTIVE' GROUP BY userIdx";
        List<Integer> existWalk =  this.jdbcTemplate.queryForList(checkUserWalkQuery,int.class,date,userIdx);

        if (existWalk.size() != 0)
            return 1;

        return 0;
    }

    // 유저 상태 조회 - validation에 사용
    public String getStatus(int userIdx, String tableName) {
        log.debug("UserDao.getStatus");
        String getStatusQuery = "select status from " + tableName + " where userIdx=?";
        return this.jdbcTemplate.queryForObject(getStatusQuery, String.class, userIdx);
    }

    // 월 단위 달성률 계산
    public int calcMonthGoalRate(int userIdx, int beforeMonth){

        // 0. 해당 달에 사용자 목표 기록이 있는지 확인
        String checkGoalExistQuery = "SELECT count(*) FROM Goal WHERE userIdx = ? and MONTH(createAt) = MONTH(DATE_SUB(CURRENT_TIMESTAMP, INTERVAL ? MONTH))";
        int checkGoalExist = this.jdbcTemplate.queryForObject(checkGoalExistQuery,int.class,userIdx,beforeMonth);
        if(checkGoalExist == 0)
            return 0;

        // 1. 사용자의 원하는 달 전체 산책 시간 확인 (초 단위)
        String getUserMonthWalkTimeQuery = "SELECT IFNULL(SUM(TIMESTAMPDIFF(second ,startAt,endAt)),0) as monthWalkTime FROM Walk WHERE userIdx = ? and status = 'ACTIVE' and MONTH(startAt) = MONTH(DATE_SUB(CURRENT_TIMESTAMP, INTERVAL ? MONTH))";
        int userMonthWalkTime = this.jdbcTemplate.queryForObject(getUserMonthWalkTimeQuery,int.class,userIdx,beforeMonth);

        // 2. 이번달 목표 시간 계산
        // 2-1. 사용자 목표 요일 정보 확인
        String getUserGoalDayQuery = "SELECT sun, mon, tue, wed, thu, fri, sat FROM GoalDay WHERE userIdx = ? and MONTH(createAt) = MONTH(DATE_SUB(CURRENT_TIMESTAMP, INTERVAL ? MONTH))";
        UserGoalDay userGoalDay = this.jdbcTemplate.queryForObject(getUserGoalDayQuery,
                (rs,rowNum) -> new UserGoalDay(
                        rs.getBoolean("sun"),
                        rs.getBoolean("mon"),
                        rs.getBoolean("tue"),
                        rs.getBoolean("wed"),
                        rs.getBoolean("thu"),
                        rs.getBoolean("fri"),
                        rs.getBoolean("sat")
                ),userIdx,beforeMonth);

        // 2-2. 원하는 달 요일별 횟수 정보 확인
        // 2-2-1. 원하는 달 최대 일수 알아오기
        LocalDate month = LocalDate.now().minusMonths(beforeMonth);
        int monthLength = month.lengthOfMonth();

        // 2-2-2. 해당 월 첫 요일 알아오기
        // *** 1:월 / 2:화 / ... / 7:일 ***
        LocalDate firstDay = LocalDate.of(month.getYear(), month.getMonth(),1);
        DayOfWeek dayOfWeek = firstDay.getDayOfWeek();
        int firstDayIdx = dayOfWeek.getValue(); // 첫 요일 인덱스

        // 2-2-3. 첫 요일 기준 그 달의 요일 수 계산
        int weekNum = monthLength/7;
        int moreDay = monthLength%7;

        // 2-2-4. 해당 달의 요일별 횟수 저장
        // dayCountArray[0] = 해당 달의 첫 요일(2021년 1월 기준 토요일)
        int[] dayCountArray = {weekNum,weekNum,weekNum,weekNum,weekNum,weekNum,weekNum};
        for (int i=0; i<moreDay; i++){
            dayCountArray[i]++;
        }

        // 2-3. 해당 달 목표 시간 계산
        // 2-3-1. GoalDay Table이 true 인 요일만 countDay에 sum
        int countDay =0 ; // 해당 달의 선택한 일수 총합
        int loopIdx = firstDayIdx; // firstDayIdx 를 시작으로 loop 을 돌 idx
        for (int i=0; i<7; i++) {
            switch (loopIdx % 7) {
                case 1: // 월요일
                    if(userGoalDay.isMon() == true)
                        countDay += dayCountArray[i];
                    break;
                case 2: // 화요일
                    if(userGoalDay.isTue() == true)
                        countDay += dayCountArray[i];
                    break;
                case 3: // 수요일
                    if(userGoalDay.isWed() == true)
                        countDay += dayCountArray[i];
                    break;
                case 4: // 목요일
                    if(userGoalDay.isThu() == true)
                        countDay += dayCountArray[i];
                    break;
                case 5: // 금요일
                    if(userGoalDay.isFri() == true)
                        countDay += dayCountArray[i];
                    break;
                case 6: // 토요일
                    if(userGoalDay.isSat() == true)
                        countDay += dayCountArray[i];
                    break;
                case 0: // 일요일
                    if(userGoalDay.isSun() == true)
                        countDay += dayCountArray[i];
                    break;
            }
            loopIdx++;
        }

        // 2-3-2. 하루 산책 목표 시간 확인
        String getUserWalkGoalTimeQuery = "SELECT walkGoalTime FROM Goal WHERE userIdx = ? and MONTH(createAt) = MONTH(DATE_SUB(CURRENT_TIMESTAMP, INTERVAL ? MONTH))";
        int userWalkGoalTime = this.jdbcTemplate.queryForObject(getUserWalkGoalTimeQuery,int.class,userIdx,beforeMonth);

        // 2-3-3. 목표 시간 계산
        int userMonthGoalTime = countDay * userWalkGoalTime;

        // 3. 이번달 목표 달성률 계산
        int monthGoalRate = (int)((userMonthWalkTime/ (double)( userMonthGoalTime*60 )) * 100);

        if(monthGoalRate > 100)
            monthGoalRate = 100;

        return monthGoalRate;
    }


    // UserController
    // 유저 목표 최신화
    // GoalNext -> Goal
    public void updateGoal(){

        // 1. GoalNext 튜플 -> Goal
        String updateGoalQuery = "INSERT INTO Goal (userIdx, walkGoalTime, walkTimeSlot)  SELECT userIdx, walkGoalTime, walkTimeSlot FROM GoalNext";
        int updateAffectedRow = this.jdbcTemplate.update(updateGoalQuery);

    }

    // UserController
    // 유저 목표 요일 최신화
    // GoalDayNext -> GoalDay
    public void updateGoalDay(){

        // 1. GoalDayNext 튜플 -> Goal
        String updateGoalQuery = "INSERT INTO GoalDay (userIdx, sun, mon, tue, wed, thu, fri, sat)  SELECT userIdx, sun, mon, tue, wed, thu, fri, sat FROM GoalDayNext";
        int updateAffectedRow = this.jdbcTemplate.update(updateGoalQuery);


    }

    // tag를 가진 발자국이 있는 산책기록에 해당하는 전체 태그 리스트
    public List<String> getTagList(int walkIdx) {
        String getTagQuery = "select hashtag from Hashtag\n" +
                "inner join Tag T on Hashtag.hashtagIdx = T.hashtagIdx\n" +
                "inner join Footprint F on T.footprintIdx = F.footprintIdx\n" +
                "inner join Walk W on F.walkIdx = W.walkIdx\n" +
                "where F.walkIdx = ? and T.status = ?";
        List<String> tagList = jdbcTemplate.queryForList(getTagQuery, String.class, walkIdx, "ACTIVE");

        return tagList;
    }

    // 다음달 목표 변경 여부 확인
    public boolean checkGoalModified(int userIdx){

        String getUpdateAtQuery = "SELECT IF(YEAR(createAt) = YEAR(NOW()) and MONTH(createAt) = MONTH(NOW()), " +
                                            "IF(createAt = updateAt, false, true), " +
                                            "IF(YEAR(updateAt) = YEAR(NOW()) and MONTH(updateAt) = MONTH(NOW()), true, false) ) " +
                                    "FROM GoalNext WHERE userIdx = ?";
        boolean updateBool = this.jdbcTemplate.queryForObject(getUpdateAtQuery,Boolean.class,userIdx);

        return updateBool;

    }

    public int getUserIdx(String userId) {
        log.debug("userId: {}", userId);
        String getUserIdxQuery = "select userIdx from User where userId = ?";
        return this.jdbcTemplate.queryForObject(getUserIdxQuery, int.class, userId);
    }

    //Badge 테이블에 존재하는 뱃지인지 검사하는 메소드
    public boolean badgeCheck(int badgeIdx) {
        String checkQuery = "select EXISTS (select badgeIdx from Badge where badgeIdx=? limit 1) as success;";
        boolean result = this.jdbcTemplate.queryForObject(checkQuery,
                (rs,rowNum)->rs.getBoolean("success"),
                badgeIdx);
        return result;
    }

    // 해당 뱃지를 사용자가 갖고 있는지 검사하는 메소드
    public boolean userBadgeCheck(int userIdx, int badgeIdx) {
        String checkQuery = "select EXISTS (select badgeIdx from UserBadge where userIdx=? and badgeIdx=? and status='ACTIVE' limit 1) as success;";
        Object[] checkParams = new Object[]{userIdx, badgeIdx};
        boolean result = this.jdbcTemplate.queryForObject(checkQuery,
                (rs,rowNum)->rs.getBoolean("success"),
                checkParams);
        return result;
    }

    //Badge 테이블에 존재하는 뱃지인지 검사하는 메소드
    public boolean checkPrevGoalDay(int userIdx) {
        String checkQuery = "select EXISTS(SELECT sun, mon, tue, wed, thu, fri, sat FROM GoalDay WHERE userIdx = ? and\n" +
                "        MONTH(createAt) = MONTH(DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 MONTH))) as success;";
        boolean result = this.jdbcTemplate.queryForObject(checkQuery,
                (rs,rowNum)->rs.getBoolean("success"),
                userIdx);
        return result;
    }

    //GoalNext 테이블에서 해당 사용자의 목표 삭제 메소드
    public void deleteGoalNext(int userIdx) {
        String delGoalNextQuery="delete from GoalNext where userIdx=?;";
        this.jdbcTemplate.update(delGoalNextQuery,userIdx);
    }

    //Goal 테이블에서 해당 사용자의 목표 삭제 메소드
    public void deleteGoal(int userIdx) {
        String delGoalQuery="delete from Goal where userIdx=?;";
        this.jdbcTemplate.update(delGoalQuery,userIdx);
    }

    //GoalDayNext 테이블에서 해당 사용자의 목표 삭제 메소드
    public void deleteGoalDayNext(int userIdx) {
        String delGoalDayNextQuery="delete from GoalDayNext where userIdx=?;";
        this.jdbcTemplate.update(delGoalDayNextQuery,userIdx);
    }

    //GoalDay 테이블에서 해당 사용자의 목표 삭제 메소드
    public void deleteGoalDay(int userIdx) {
        String delGoalDayQuery="delete from GoalDay where userIdx=?;";
        this.jdbcTemplate.update(delGoalDayQuery,userIdx);
    }
    //UserBadge 테이블에서 해당 사용자의 뱃지 삭제 메소드
    public void deleteUserBadge(int userIdx) {
        String delUserBadgeQuery="delete from UserBadge where userIdx=?;";
        this.jdbcTemplate.update(delUserBadgeQuery,userIdx);
    }

    // Tag 테이블에서 해당 사용자의 태그 삭제 메소드
    public void deleteTag(int userIdx) {
        String delTagQuery="delete from Tag where userIdx=?;";
        this.jdbcTemplate.update(delTagQuery,userIdx);
    }

    //Photo 테이블에서 해당 사용자의 사진 삭제 메소드
    public void deletePhoto(int userIdx) {
        //Photo 테이블에서 해당 사용자의 사진 모두 삭제
        String delPhotoQuery="delete from Photo where userIdx=?;";
        this.jdbcTemplate.update(delPhotoQuery,userIdx);
    }

    //Photo 테이블에서 사용자의 사진 url을 반환하는 메소드
    public List<String> getImageUrlList(int userIdx) {
        //s3에서 이미지 url 먼저 삭제하기 위해 imageUrl 리스트로 반환
        String getImageUrlQuery = "select imageUrl from Photo where userIdx=?;";
        List<String> imageUrlList = jdbcTemplate.queryForList(getImageUrlQuery, String.class, userIdx);
        return imageUrlList;
    }

    // Footprint 테이블에서 해당 사용자의 산책일기 삭제
    public void deleteFootprint(int userIdx) {
        //userIdx로 walkIdx 추출
        String getWalkIdxQuery = "select walkIdx from Walk where userIdx=?;";
        List<Integer> walkIdxList = jdbcTemplate.queryForList(getWalkIdxQuery, int.class, userIdx);

        //해당 walkIdx에 해당하는 발자국 모두 삭제
        String delFootprintQuery="delete from Footprint where walkIdx=?;";
        for(int walkIdx : walkIdxList) {
            this.jdbcTemplate.update(delFootprintQuery,walkIdx);
        }
    }

    // Walk 테이블에서 해당 사용자의 산책 기록 삭제
    public void deleteWalk(int userIdx) {
        String delWalkQuery="delete from Walk where userIdx=?;";
        this.jdbcTemplate.update(delWalkQuery,userIdx);
    }

    //Walk 테이블에서 사용자의 사진 url을 반환하는 메소드
    public List<String> getPathImageUrlList(int userIdx) {
        //s3에서 이미지 url 먼저 삭제하기 위해 imageUrl 리스트로 반환
        String getPathImageUrlQuery = "select pathImageUrl from Walk where userIdx=? and status = 'ACTIVE';";
        List<String> pathImageUrlList = jdbcTemplate.queryForList(getPathImageUrlQuery, String.class, userIdx);
        return pathImageUrlList;
    }

    // User 테이블에서 해당 사용자 삭제
    public void deleteUser(int userIdx) {
        String delUserQuery="delete from User where userIdx=?;";
        this.jdbcTemplate.update(delUserQuery,userIdx);
    }



}
