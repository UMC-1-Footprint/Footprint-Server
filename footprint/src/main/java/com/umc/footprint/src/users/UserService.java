package com.umc.footprint.src.users;

import static com.umc.footprint.config.BaseResponseStatus.*;

import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.umc.footprint.config.BaseException;
import com.umc.footprint.src.AwsS3Service;
import com.umc.footprint.src.badge.model.Badge;
import com.umc.footprint.src.badge.model.BadgeRepository;
import com.umc.footprint.src.badge.model.UserBadge;
import com.umc.footprint.src.badge.model.UserBadgeRepository;
import com.umc.footprint.src.common.model.entity.Photo;
import com.umc.footprint.src.common.model.entity.Tag;
import com.umc.footprint.src.common.model.vo.WalkHashtag;
import com.umc.footprint.src.common.repository.PhotoRepository;
import com.umc.footprint.src.common.repository.TagRepository;
import com.umc.footprint.src.footprints.model.dto.GetFootprintCount;
import com.umc.footprint.src.footprints.model.entity.Footprint;
import com.umc.footprint.src.footprints.model.vo.GetFootprintCountInterface;
import com.umc.footprint.src.footprints.repository.FootprintRepository;
import com.umc.footprint.src.goal.GoalService;
import com.umc.footprint.src.goal.model.entity.Goal;
import com.umc.footprint.src.goal.model.entity.GoalDay;
import com.umc.footprint.src.goal.model.entity.GoalDayNext;
import com.umc.footprint.src.goal.model.entity.GoalNext;
import com.umc.footprint.src.goal.model.vo.GetMonthTotalInterface;
import com.umc.footprint.src.goal.repository.GoalDayNextRepository;
import com.umc.footprint.src.goal.repository.GoalDayRepository;
import com.umc.footprint.src.goal.repository.GoalNextRepository;
import com.umc.footprint.src.goal.repository.GoalRepository;
import com.umc.footprint.src.users.model.dto.GetDayRateRes;
import com.umc.footprint.src.users.model.dto.GetMonthInfoRes;
import com.umc.footprint.src.users.model.dto.GetTagRes;
import com.umc.footprint.src.users.model.dto.GetUserDateRes;
import com.umc.footprint.src.users.model.dto.GetUserRes;
import com.umc.footprint.src.users.model.dto.GetUserTodayRes;
import com.umc.footprint.src.users.model.dto.PatchUserInfoReq;
import com.umc.footprint.src.users.model.dto.PostLoginReq;
import com.umc.footprint.src.users.model.dto.PostLoginRes;
import com.umc.footprint.src.users.model.entity.User;
import com.umc.footprint.src.users.model.vo.GetDayRateResInterface;
import com.umc.footprint.src.users.model.vo.GetMonthTotal;
import com.umc.footprint.src.users.model.vo.UserInfoAchieve;
import com.umc.footprint.src.users.model.vo.UserInfoStat;
import com.umc.footprint.src.users.repository.UserRepository;
import com.umc.footprint.src.walks.model.entity.Walk;
import com.umc.footprint.src.walks.model.vo.UserDateWalk;
import com.umc.footprint.src.walks.repository.WalkRepository;
import com.umc.footprint.utils.AES128;
import com.umc.footprint.utils.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserDao userDao;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final WalkRepository walkRepository;
    private final JwtService jwtService;
    private final AwsS3Service awsS3Service;
    private final GoalService goalService;
    private final GoalRepository goalRepository;
    private final GoalNextRepository goalNextRepository;
    private final GoalDayRepository goalDayRepository;
    private final GoalDayNextRepository goalDayNextRepository;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final PhotoRepository photoRepository;
    private final FootprintRepository footprintRepository;


    // 해당 유저의 산책기록 중 태그를 포함하는 산책기록 조회
    public List<GetTagRes> getTagResult(String userId, String tag) throws BaseException {
        try {
            Integer userIdx = userRepository.findByUserId(userId).getUserIdx();

            // 2. 태그 검색
            List<WalkHashtag> walkAndHashtagList = tagRepository.findAllWalkAndHashtag(tag, userIdx);

            // 3. 추출한 값으로 response 객체 초기화
            Set<String> dateSet = new HashSet<>();
            // 날짜만 추출
            for (WalkHashtag walkHashtag : walkAndHashtagList) {
                dateSet.add(
                        // 날짜 + 요일 ex) 2022. 5. 8 일
                        walkHashtag.getEndAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")).replace(".0", ". ")
                                + " " + walkHashtag.getEndAt().getDayOfWeek().getDisplayName(TextStyle.NARROW, Locale.KOREAN)
                );
            }

            List<Integer> allByUserIdx = walkRepository.findAllByUserIdxAndStatusOrderByWalkIdx(userIdx, "ACTIVE").stream().map(Walk::getWalkIdx).collect(Collectors.toList());

            // response 객체
            List<GetTagRes> getTagResList = new ArrayList<>();
            // response 객체에 저장된 walkIdx (중복 저장 확인용)
            List<Integer> responseWalkIdxList = new ArrayList<>();

            // 날짜에 따라 분류
            for (String date : dateSet) {
                // walk 정보 + hashtag 리스트
                Set<GetUserDateRes> searchWalkSet = new HashSet<>();

                for (WalkHashtag walkHashtag : walkAndHashtagList) {
                    // 날짜 + 요일 ex) 2022. 5. 8 일
                    String walkDate = walkHashtag.getEndAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")).replace(".0", ". ")
                            + " " + walkHashtag.getEndAt().getDayOfWeek().getDisplayName(TextStyle.NARROW, Locale.KOREAN);
                    // 날짜 같은 경우만 response 객체에 삽입 && response에 중복된 산책이 없어야함
                    if (date.equals(walkDate) && responseWalkIdxList.indexOf(walkHashtag.getWalkIdx()) == -1) {
                        // hashtag 리스트
                        List<String> hashtagList = new ArrayList<>();
                        for (WalkHashtag hashtag : walkAndHashtagList) {
                            // 동일한 산책에 있는 지 확인
                            if (walkHashtag.getWalkIdx().equals(hashtag.getWalkIdx())) {
                                hashtagList.add(hashtag.getHashtag());
                            }
                        }


                        searchWalkSet.add(
                                GetUserDateRes.builder()
                                        // 산책 정보
                                        .userDateWalk(
                                                UserDateWalk.builder()
                                                        .walkIdx(allByUserIdx.indexOf(walkHashtag.getWalkIdx()) + 1)
                                                        .startTime(walkHashtag.getStartAt().format(DateTimeFormatter.ofPattern("HH:mm")))
                                                        .endTime(walkHashtag.getEndAt().format(DateTimeFormatter.ofPattern("HH:mm")))
                                                        .pathImageUrl(AES128.decrypt(walkHashtag.getPathImageUrl()))
                                                        .build()
                                        )
                                        .hashtag(hashtagList)
                                        .build()
                        );
                        responseWalkIdxList.add(walkHashtag.getWalkIdx());
                    }
                }

                getTagResList.add(
                        GetTagRes.builder()
                                .walkAt(date)
                                .walks(new ArrayList<>(searchWalkSet))
                                .build()
                );
            }

            return getTagResList;
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new BaseException(DATABASE_ERROR);
        }
    }

    // 유저 정보 수정(Patch)
    @Transactional(propagation = Propagation.NESTED, rollbackFor = Exception.class)
    public void modifyUserInfoJPA(String userId, PatchUserInfoReq patchUserInfoReq) throws BaseException {
        try {
            int userIdx = getUserIdxByUserId(userId);
            Optional<User> user = userRepository.findByUserIdx(userIdx);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            user.get().setBirth(LocalDate.parse(patchUserInfoReq.getBirth(), formatter).atStartOfDay());
            user.get().setNickname(patchUserInfoReq.getNickname());
            user.get().setSex(patchUserInfoReq.getSex());
            user.get().setHeight(patchUserInfoReq.getHeight());
            user.get().setWeight(patchUserInfoReq.getWeight());

            userRepository.save(user.get());

        } catch (Exception exception) { // DB에 이상이 있는 경우 에러 메시지를 보냅니다.
            exception.printStackTrace();
            throw new BaseException(DATABASE_ERROR);
        }
    }


    // 해당 userIdx를 갖는 Info 정보 저장
    @Transactional(propagation = Propagation.NESTED, rollbackFor = Exception.class)
    public int postUserInfo(int userIdx, PatchUserInfoReq patchUserInfoReq) throws BaseException {
        try {
            int resultInfo = userDao.modifyUserInfo(userIdx, patchUserInfoReq);
            log.debug("resultInfo: {}", resultInfo);

            // 요일별 인덱스 차이 해결을 위한 임시 코드
            List<Integer> dayIdxList = new ArrayList<>();
            for (Integer dayIdx : patchUserInfoReq.getDayIdx()) {
                if (dayIdx == 7)
                    dayIdxList.add(1);
                else
                    dayIdxList.add(dayIdx + 1);
            }
            Collections.sort(dayIdxList);
            patchUserInfoReq.setDayIdx(dayIdxList);
            log.debug("dayIdxList : {}", dayIdxList);
            //

            int result = userDao.postGoal(userIdx, patchUserInfoReq);
            log.debug("result : {}", result);
            int resultNext = userDao.postGoalNext(userIdx, patchUserInfoReq);
            log.debug("resultNext: {}", resultNext);

            if (resultInfo == 0 || result == 0 || resultNext == 0)
                return 0;
            return 1;

        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    public PostLoginRes checkEmail(String email) throws BaseException {
        try {
            log.debug("email: {}", email);
            // checkExistFlag == true -> 유저 이미 존재
            // checkExistFlag == false -> 유저 정보 등록 필요
            boolean checkExistFlag = userRepository.existsByEmail(email);

            if (checkExistFlag) {
                // email로 userId랑 상태 추출
                User user = userRepository.findByEmail(email);
                PostLoginRes postLoginRes = PostLoginRes.builder()
                        .jwtId(user.getUserId())
                        .status(user.getStatus())
                        .build();
                // userId 암호화
                String jwtId = jwtService.createJwt(postLoginRes.getJwtId());
                // response에 저장
                postLoginRes.setJwtId(jwtId);
                return postLoginRes;
            } else {
                return PostLoginRes.builder()
                        .jwtId("")
                        .status("NONE")
                        .checkMonthChanged(false)
                        .build();
            }
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    @Transactional(propagation = Propagation.NESTED, rollbackFor = Exception.class)
    public PostLoginRes postUserLogin(PostLoginReq postLoginReq) throws BaseException {
        // email 중복 확인 있으면 status에 Done 넣고 return
        try {
            String encryptEmail = AES128.encrypt(postLoginReq.getEmail());
            PostLoginRes result = checkEmail(encryptEmail);
            log.debug("유저의 status: {}", result.getStatus());
            // status: NONE -> 회원가입(유저 정보 db에 등록 필요)
            // status: ACTIVE -> 로그인
            // status: ACTIVE -> 정보 입력 필요
            switch (result.getStatus()) {
                case "NONE":
                    // 암호화
                    String jwt = jwtService.createJwt(postLoginReq.getUserId());
                    // 유저 정보 db에 등록
                    postLoginReq.setEncryptedInfos(AES128.encrypt(postLoginReq.getUsername()), encryptEmail);
                    userRepository.save(postLoginReq.toUserEntity());
                    userBadgeRepository.save(
                            UserBadge.builder()
                                    .badgeIdx(0)
                                    .userIdx(userRepository.findByUserId(postLoginReq.getUserId()).getUserIdx())
                                    .status("ACTIVE")
                                    .build()
                    );

                    return PostLoginRes.builder()
                            .jwtId(jwt)
                            .status("ONGOING")
                            .checkMonthChanged(false)
                            .build();
                case "ACTIVE":
                case "ONGOING":
                    return result;
            }
            return null;
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }

    }

    public PostLoginRes modifyUserLogAt(String userId) throws BaseException {
        try {
            // 이전에 로그인 했던 시간
            User user = userRepository.findByUserId(userId);

            LocalDateTime beforeLogAt = user.getLogAt();

            boolean checkMonthChangedFlag = true;

            // 달이 같은 경우
            if (beforeLogAt.getMonth() == LocalDateTime.now().getMonth()) {
                // 달이 바뀌지 않았다고 response에 저장
                checkMonthChangedFlag = false;
            }

            // 현재 로그인하는 시간 logAt에 저장
            user.updateLogAtNow(LocalDateTime.now());
            userRepository.save(user);

            return PostLoginRes.builder()
                    .jwtId(jwtService.createJwt(user.getUserId()))
                    .status(user.getStatus())
                    .checkMonthChanged(checkMonthChangedFlag)
                    .build();
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    @Transactional(propagation = Propagation.NESTED, rollbackFor = Exception.class)
    public void deleteUser(String userId) throws BaseException {
        try {
            int userIdx = getUserIdxByUserId(userId);
            // GoalNext 테이블
            Optional<GoalNext> goalNext = goalNextRepository.findByUserIdx(userIdx);
            goalNextRepository.deleteById(goalNext.get().getPlanIdx());

            // GoalDayNext 테이블
            Optional<GoalDayNext> goalDayNext = goalDayNextRepository.findByUserIdx(userIdx);
            goalDayNextRepository.deleteById(goalDayNext.get().getPlanIdx());

            // Goal 테이블
            List<Goal> goalList = goalRepository.findAllByUserIdx(userIdx);
            for (Goal goal : goalList) {
                goalRepository.deleteById(goal.getPlanIdx());
            }

            // GoalDay 테이블
            List<GoalDay> goalDayList = goalDayRepository.findByUserIdx(userIdx);
            for (GoalDay goalDay : goalDayList) {
                goalDayRepository.deleteById(goalDay.getPlanIdx());
            }

            // UserBadge 테이블
            List<UserBadge> userBadgeList = userBadgeRepository.findAllByUserIdx(userIdx);
            for (UserBadge userBadge : userBadgeList) {
                userBadgeRepository.deleteById(userBadge.getCollectionIdx());
            }

            // Tag 테이블
            List<Tag> tagList = tagRepository.findAllByUserIdx(userIdx);
            for (Tag tag : tagList) {
                tagRepository.deleteById(tag.getTagIdx());
            }

            // Photo 테이블 -> s3에서 이미지 url 먼저 삭제 후 테이블 삭제 필요
            List<Photo> photoList = photoRepository.findAllByUserIdx(userIdx);
            for (Photo photo : photoList) {
                String decryptedImageUrl = AES128.decrypt(photo.getImageUrl());
                String fileName = decryptedImageUrl.substring(decryptedImageUrl.lastIndexOf("/") + 1); // 파일 이름만 자르기
                awsS3Service.deleteFile(fileName);

                photoRepository.deleteById(photo.getPhotoIdx());
            }


            // Walk + Footprint 테이블
            List<Walk> walkList = walkRepository.findAllByUserIdx(userIdx);
            for (Walk walk : walkList) {
                List<Footprint> footprintList = walk.getFootprintList();

                // Walk에 해당하는 Footprint 모두 삭제
                for (Footprint footprint : footprintList) {
                    footprintRepository.deleteById(footprint.getFootprintIdx());
                }

                // Walk 테이블 - 동선 이미지 S3 에서도 삭제
                String decryptedImageUrl = AES128.decrypt(walk.getPathImageUrl());
                String fileName = decryptedImageUrl.substring(decryptedImageUrl.lastIndexOf("/") + 1); // 파일 이름만 자르기
                awsS3Service.deleteFile(fileName);

                // Walk 삭제
                walkRepository.deleteById(walk.getWalkIdx());
            }


            // User 테이블
            userRepository.deleteById(userIdx);

        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    public GetUserTodayRes getUserToday(String userId) throws BaseException {
        int userIdx = getUserIdxByUserId(userId);
        List<Walk> userWalkList = walkRepository.findAllByStatusAndUserIdx("ACTIVE", userIdx);
        System.out.println("userWalkList.size() = " + userWalkList.size());
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        double todayGoalRate = 0;
        int todayTotalTime = 0;
        double todayTotalDist = 0;
        int todayTotalCal = 0;

        for (Walk userWalk : userWalkList) {
            if (userWalk.getStartAt().toLocalDate().isEqual(today)) {
                // 1. 오늘 목표 달성량 추출
                todayGoalRate += userWalk.getGoalRate();

                // 2. 오늘 산책 누적 시간 추출
                Duration timeDiff = Duration.between(userWalk.getStartAt(), userWalk.getEndAt());
                todayTotalTime += timeDiff.getSeconds() / 60;

                // 3. 오늘 산책 누적 거리 추출
                todayTotalDist += userWalk.getDistance();

                // 4. 오늘 산책 누적 칼로리 추출
                todayTotalCal += userWalk.getCalorie();
            }
        }
        // 1-1. 목표 달성량 100 초과시 100으로 설정
        if (todayGoalRate > 100)
            todayGoalRate = 100;

        // 3-1. 누적 거리 소수점 셋째자리 절삭
        todayTotalDist = Math.floor(todayTotalDist * 100) / 100.0;

        // 5. 오늘 산책 목표 시간 추출
        List<GoalDay> userGoalDayList = goalDayRepository.findByUserIdx(userIdx);
        GoalDay goalDay = GoalDay.builder().build();
        for (GoalDay gd : userGoalDayList) {
            LocalDate goalDayCreateAt = gd.getCreateAt().toLocalDate();
            if (goalDayCreateAt.getMonth().equals(LocalDate.now().getMonth()) && goalDayCreateAt.getYear() == LocalDate.now().getYear()) {
                goalDay = gd;
                break;
            }
        }

//        /** 목표에 등록한 요일만 목표 시간이 제공되고 그 외에는 목표 시간이 0으로 제공 */
//        DayOfWeek dayOfWeek = today.getDayOfWeek();
//        boolean onDay = false;
//
//        switch (dayOfWeek){
//            case MONDAY:
//                if (goalDay.getMon().equals(1)){
//                    onDay = true;
//                } break;
//            case TUESDAY:
//                if (goalDay.getTue().equals(1)){
//                    onDay = true;
//                } break;
//            case WEDNESDAY:
//                if (goalDay.getWed().equals(1)){
//                    onDay = true;
//                } break;
//            case THURSDAY:
//                if (goalDay.getThu().equals(1)){
//                    onDay = true;
//                } break;
//            case FRIDAY:
//                if (goalDay.getFri().equals(1)){
//                    onDay = true;
//                } break;
//            case SATURDAY:
//                if (goalDay.getSat().equals(1)){
//                    onDay = true;
//                } break;
//            case SUNDAY:
//                if (goalDay.getSun().equals(1)){
//                    onDay = true;
//                } break;
//
//        }
//
//        int walkGoalTime = 0;
//        if(onDay == true){

        List<Goal> userGoalList = goalRepository.findAllByUserIdx(userIdx);
        Goal userGoal = Goal.builder().build();
        for (Goal goal : userGoalList) {
            LocalDate goalCreateAt = goal.getCreateAt().toLocalDate();
            if (goalCreateAt.getMonth().equals(LocalDate.now().getMonth()) && goalCreateAt.getYear() == LocalDate.now().getYear()) {
                userGoal = goal;
                break;
            }
        }

        int walkGoalTime = userGoal.getWalkGoalTime();

        return GetUserTodayRes.builder()
                .goalRate(todayGoalRate)
                .walkTime(todayTotalTime)
                .distance(todayTotalDist)
                .calorie(todayTotalCal)
                .walkGoalTime(walkGoalTime)
                .build();

    }

    public List<GetUserDateRes> getUserDate(String userId, String date) throws BaseException {

        try {
            int userIdx = getUserIdxByUserId(userId);

            List<Walk> userWalkList = walkRepository.findAllByStatusAndUserIdx("ACTIVE", userIdx);
            List<GetUserDateRes> getUserDateResList = new ArrayList<>();
            UserDateWalk userDateWalk;

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            int count = 0;

            for (Walk userWalk : userWalkList) {
                count++;

                if (!userWalk.getStartAt().toLocalDate().equals(LocalDate.parse(date, formatter)))
                    continue;

                userDateWalk = UserDateWalk.builder()
                        .walkIdx(count)
                        .startTime(userWalk.getStartAt().format(DateTimeFormatter.ofPattern("HH:mm")))
                        .endTime(userWalk.getEndAt().format(DateTimeFormatter.ofPattern("HH:mm")))
                        .pathImageUrl(AES128.decrypt(userWalk.getPathImageUrl()))
                        .build();

                List<Footprint> footprintList = userWalk.getFootprintList();
                ArrayList<String> tagString = new ArrayList<>();
                for (Footprint footprint : footprintList) {
                    List<Tag> tagList = footprint.getTagList();

                    for (Tag tag : tagList) {
                        if (tag.getStatus().equals("ACTIVE")) {
                            tagString.add(tag.getHashtag().getHashtag());
                        }
                    }
                }
                getUserDateResList.add(GetUserDateRes.builder()
                        .hashtag(tagString)
                        .userDateWalk(userDateWalk)
                        .build());

            }

            return getUserDateResList;
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }

    }


    public UserInfoAchieve getUserInfoAchieve(int userIdx) {

        List<Walk> walkList = walkRepository.findAllByStatusAndUserIdx("ACTIVE", userIdx);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        System.out.println("CHECK POINT 1");

        List<Walk> todayWalkList = walkList.stream()
                .filter(s -> s.getStartAt().toLocalDate().equals(today))
                .collect(Collectors.toList());

        System.out.println("CHECK POINT 2");

        /** 1. 오늘 목표 달성률 계산 = todayGoalRate */
        double totalGoalRate = 0;
        for (Walk todayWalk : todayWalkList) {
            totalGoalRate += todayWalk.getGoalRate();
        }

        System.out.println("CHECK POINT 3");

        /** 2. 이번달 목표 달성률 계산 = monthGoalRate */
        int monthGoalRate = calcMonthGoalRate(userIdx, 0);

        System.out.println("CHECK POINT 4");

        /** 3. 산책 횟수 계산 = userWalkCount */
        int userWalkCount = walkList.size();

        System.out.println("CHECK POINT 5");

        return new UserInfoAchieve((int) totalGoalRate, monthGoalRate, userWalkCount);
    }

    public UserInfoStat getUserInfoStat(int userIdx) {
        // [ 1. 이전 3달 기준 요일별 산책 비율 ] = List<String> mostWalkDay & List<Double> userWeekDayRate

        // 1-1. 이전 3달 기준 요일별 산책 수 확인
        List<Integer> userWeekDayCount = new ArrayList<>(Arrays.asList(0, 0, 0, 0, 0, 0, 0));
        List<Double> userWeekDayRate = new ArrayList<>();

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        List<Walk> userWalkList = walkRepository.findAllByStatusAndUserIdx("ACTIVE", userIdx);

        for (Walk userWalk : userWalkList) {
            if (userWalk.getStartAt().toLocalDate().isAfter(today.minusMonths(3))) {
                DayOfWeek dayOfWeek = userWalk.getStartAt().getDayOfWeek();
                if (dayOfWeek.equals(DayOfWeek.SUNDAY)) {
                    userWeekDayCount.set(0, userWeekDayCount.get(0) + 1);
                } else if (dayOfWeek.equals(DayOfWeek.MONDAY)) {
                    userWeekDayCount.set(1, userWeekDayCount.get(1) + 1);
                } else if (dayOfWeek.equals(DayOfWeek.TUESDAY)) {
                    userWeekDayCount.set(2, userWeekDayCount.get(2) + 1);
                } else if (dayOfWeek.equals(DayOfWeek.WEDNESDAY)) {
                    userWeekDayCount.set(3, userWeekDayCount.get(3) + 1);
                } else if (dayOfWeek.equals(DayOfWeek.THURSDAY)) {
                    userWeekDayCount.set(4, userWeekDayCount.get(4) + 1);
                } else if (dayOfWeek.equals(DayOfWeek.FRIDAY)) {
                    userWeekDayCount.set(5, userWeekDayCount.get(5) + 1);
                } else if (dayOfWeek.equals(DayOfWeek.SATURDAY)) {
                    userWeekDayCount.set(6, userWeekDayCount.get(6) + 1);
                }
            }
        }

        // 1-2. 이전 3달 기준 전체 산책 수 구하기
        int entireCount = 0;
        for (Integer dayCount : userWeekDayCount) {
            entireCount += dayCount;
        }
        // 1-3. 가장 산책이 많은 요일 추출 (동일 max 존재시 둘다 return) = List<String> mostWalkDay
        List<String> mostWalkDay = new ArrayList<>();

        if (entireCount == 0) // 최근 3개월간 산책 기록이 없을때
            mostWalkDay.add("최근 3개월간 산책을 하지 않았어요");
        else {
            int max = Collections.max(userWeekDayCount);

            for (int i = 0; i < userWeekDayCount.size(); i++) {
                if (userWeekDayCount.get(i) == max) {
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
        // *** 순서 : 일 월 화 수 목 금 토 ***
        for (Integer dayCount : userWeekDayCount) {
            if (entireCount == 0)
                userWeekDayRate.add(0.0);
            else
                userWeekDayRate.add(dayCount / (double) entireCount * 100);
        }

        // [ 2. 이전 6달 범위 월별 산책 횟수 ] = thisMonthWalkCount + List<Integer> monthlyWalkCount
        // List 순서 : -6달 , -5달 , ... , 전달 , 이번달 (총 7개 element)

        // 2-1. 이번달 산책 횟수 가져오기
        int thisMonthWalkCount = userWalkList.size();

        // 2-2. 이전 6달 범위 유저 월별 산책 횟수 가져오기
        List<Integer> monthlyWalkCount = new ArrayList<>(Arrays.asList(0, 0, 0, 0, 0, 0, 0));
        for (Walk userWalk : userWalkList) {
            if (userWalk.getStartAt().getMonth().equals(today.minusMonths(5).getMonth())) {
                monthlyWalkCount.set(1, monthlyWalkCount.get(1) + 1);
            } else if (userWalk.getStartAt().getMonth().equals(today.minusMonths(4).getMonth())) {
                monthlyWalkCount.set(2, monthlyWalkCount.get(2) + 1);
            } else if (userWalk.getStartAt().getMonth().equals(today.minusMonths(3).getMonth())) {
                monthlyWalkCount.set(3, monthlyWalkCount.get(3) + 1);
            } else if (userWalk.getStartAt().getMonth().equals(today.minusMonths(2).getMonth())) {
                monthlyWalkCount.set(4, monthlyWalkCount.get(4) + 1);
            } else if (userWalk.getStartAt().getMonth().equals(today.minusMonths(1).getMonth())) {
                monthlyWalkCount.set(5, monthlyWalkCount.get(5) + 1);
            } else if (userWalk.getStartAt().getMonth().equals(today.getMonth())) {
                monthlyWalkCount.set(6, monthlyWalkCount.get(6) + 1);
            }
        }

        // [ 3. 이전 5달 범위 월별 달성률 & 평균 달성률 ] = List<Integer>monthlyGoalRate + avgGoalRate
        // List 순서 : 평균, -5달 , ... , 전달 , 이번달 (총 7개 element)
        List<Integer> monthlyGoalRate = new ArrayList<>();
        int sumGoalRate = 0;

        // 현재 달 + 이전 5달 범위의 월별 달성률 계산 by getMonthGoalRate()
        for (int i = 5; i >= 0; i--) {
            int goalRate = calcMonthGoalRate(userIdx, i); // Method getMonthGoalRate
            if (goalRate > 100) // 100 초과시 100으로 저장
                goalRate = 100;
            monthlyGoalRate.add(goalRate);
            sumGoalRate += monthlyGoalRate.get(5 - i);
        }

        int avgGoalRate = (int) ((double) sumGoalRate / 6);

        // monthlyGoalRate index 0에 avgGoalRate 추가
        monthlyGoalRate.add(0, avgGoalRate);

        return new UserInfoStat(mostWalkDay, userWeekDayRate, thisMonthWalkCount, monthlyWalkCount, monthlyGoalRate.get(6), monthlyGoalRate);

    }


    public int calcMonthGoalRate(int userIdx, int beforeMonth) {

        // 0. 해당 달에 사용자 목표 기록이 있는지 확인
        List<Goal> userGoalList = goalRepository.findAllByUserIdx(userIdx);
        boolean isGoalExist = false;
        for (Goal goal : userGoalList) {
            LocalDate goalCreateAt = goal.getCreateAt().toLocalDate();
            if (goalCreateAt.getMonth().equals(LocalDate.now().minusMonths(beforeMonth).getMonth()) && goalCreateAt.getYear() == LocalDate.now(ZoneId.of("Asia/Seoul")).minusMonths(beforeMonth).getYear()) {
                isGoalExist = true;
                break;
            }
        }
        if (isGoalExist == false) {
            return 0;
        }

        System.out.println("CHECK POINT. calc 1");

        List<Walk> walkList = walkRepository.findAllByStatusAndUserIdx("ACTIVE", userIdx);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        System.out.println("CHECK POINT. calc 2");

        List<Walk> monthWalkList = walkList.stream()
                .filter(s -> s.getStartAt().toLocalDate().getYear() == today.getYear() && s.getStartAt().toLocalDate().getMonth().equals(today.getMonth()))
                .collect(Collectors.toList());

        System.out.println("CHECK POINT. calc 3");

        // 1. 사용자의 원하는 달 전체 산책 시간 확인 (초 단위)
        int userMonthWalkTime = 0;
        for (Walk monthWalk : monthWalkList) {
            System.out.println("Duration.between(monthWalk.getStartAt(),monthWalk.getEndAt()) = " + Duration.between(monthWalk.getStartAt(), monthWalk.getEndAt()).getSeconds());
            userMonthWalkTime += (int) Duration.between(monthWalk.getStartAt(), monthWalk.getEndAt()).getSeconds();
        }

        System.out.println("CHECK POINT. calc 4");

        // 2. 이번달 목표시간 계산
        List<GoalDay> userGoalDayList = goalDayRepository.findByUserIdx(userIdx);
        GoalDay userGoalDay = GoalDay.builder().build();
        for (GoalDay goalDay : userGoalDayList) {
            LocalDate goalDayCreateAt = goalDay.getCreateAt().toLocalDate();
            if (goalDayCreateAt.getMonth().equals(LocalDate.now().getMonth()) && goalDayCreateAt.getYear() == LocalDate.now().getYear()) {
                userGoalDay = goalDay;
                break;
            }
        }

        System.out.println("userGoalDay = " + userGoalDay);
        System.out.println("userGoalDay = " + userGoalDay);
        System.out.println("userGoalDay.getSun() = " + userGoalDay.getSun());
        System.out.println("userGoalDay.getMon() = " + userGoalDay.getMon());

        System.out.println("CHECK POINT. calc 5");

        List<Integer> dayIdxList = new ArrayList<>();
        if (userGoalDay.getMon().equals(1))
            dayIdxList.add(1);
        if (userGoalDay.getTue().equals(1))
            dayIdxList.add(2);
        if (userGoalDay.getWed().equals(1))
            dayIdxList.add(3);
        if (userGoalDay.getThu().equals(1))
            dayIdxList.add(4);
        if (userGoalDay.getFri().equals(1))
            dayIdxList.add(5);
        if (userGoalDay.getSat().equals(1))
            dayIdxList.add(6);
        if (userGoalDay.getSun().equals(1))
            dayIdxList.add(7);

        System.out.println("CHECK POINT. calc 6");

        // 2-2. 원하는 달 요일별 횟수 정보 확인
        // 2-2-1. 원하는 달 최대 일수 알아오기
        LocalDate month = LocalDate.now().minusMonths(beforeMonth);
        int monthLength = month.lengthOfMonth();

        // 2-2-2. 해당 월 첫 요일 알아오기
        // *** 1:월 / 2:화 / ... / 7:일 ***
        LocalDate firstDay = LocalDate.of(month.getYear(), month.getMonth(), 1);
        DayOfWeek dayOfWeek = firstDay.getDayOfWeek();
        int firstDayIdx = dayOfWeek.getValue(); // 첫 요일 인덱스

        // 2-2-3. 첫 요일 기준 그 달의 요일 수 계산
        int weekNum = monthLength / 7;
        int moreDay = monthLength % 7;

        // 2-2-4. 해당 달의 요일별 횟수 저장
        // dayCountArray[0] = 해당 달의 첫 요일(2021년 1월 기준 토요일)
        int[] dayCountArray = {weekNum, weekNum, weekNum, weekNum, weekNum, weekNum, weekNum};
        for (int i = 0; i < moreDay; i++) {
            dayCountArray[i]++;
        }

        System.out.println("CHECK POINT. calc 7");

        // 2-3. 해당 달 목표 시간 계산
        // 2-3-1. GoalDay Table이 true 인 요일만 countDay에 sum
        int countDay = 0; // 해당 달의 선택한 일수 총합
        int loopIdx = firstDayIdx; // firstDayIdx 를 시작으로 loop 을 돌 idx
        for (int i = 0; i < 7; i++) {
            switch (loopIdx % 7) {
                case 1: // 월요일
                    if (userGoalDay.getMon() == 1)
                        countDay += dayCountArray[i];
                    break;
                case 2: // 화요일
                    if (userGoalDay.getTue() == 1)
                        countDay += dayCountArray[i];
                    break;
                case 3: // 수요일
                    if (userGoalDay.getWed() == 1)
                        countDay += dayCountArray[i];
                    break;
                case 4: // 목요일
                    if (userGoalDay.getThu() == 1)
                        countDay += dayCountArray[i];
                    break;
                case 5: // 금요일
                    if (userGoalDay.getFri() == 1)
                        countDay += dayCountArray[i];
                    break;
                case 6: // 토요일
                    if (userGoalDay.getSat() == 1)
                        countDay += dayCountArray[i];
                    break;
                case 0: // 일요일
                    if (userGoalDay.getSun() == 1)
                        countDay += dayCountArray[i];
                    break;
            }
            loopIdx++;
        }

        // 2-3-2. 하루 산책 목표 시간 확인
        Goal userGoal = Goal.builder().build();
        for (Goal goal : userGoalList) {
            LocalDate goalCreateAt = goal.getCreateAt().toLocalDate();
            System.out.println("beforeMonth = " + beforeMonth);
            if (goalCreateAt.getMonth().equals(LocalDate.now().minusMonths(beforeMonth).getMonth()) && goalCreateAt.getYear() == LocalDate.now(ZoneId.of("Asia/Seoul")).minusMonths(beforeMonth).getYear()) {
                System.out.println("goalCreateAt = " + goalCreateAt);
                userGoal = goal;
                break;
            }

        }

        System.out.println("CHECK POINT. calc 8");
        System.out.println("userGoal = " + userGoal);
        System.out.println("userGoal.getWalkGoalTime() = " + userGoal.getWalkGoalTime());
        int userWalkGoalTime = userGoal.getWalkGoalTime();
        System.out.println("userWalkGoalTime = " + userWalkGoalTime);

        // 2-3-3. 목표 시간 계산
        int userMonthGoalTime = countDay * userWalkGoalTime;
        System.out.println("userMonthGoalTime = " + userMonthGoalTime);

        // 3. 이번달 목표 달성률 계산
        int monthGoalRate = (int) ((userMonthWalkTime / (double) (userMonthGoalTime * 60)) * 100);
        System.out.println("monthGoalRate = " + monthGoalRate);

        if (monthGoalRate > 100)
            monthGoalRate = 100;

        return monthGoalRate;
    }

    public GetUserRes getUser(String userId) throws BaseException {
        try {
            int userIdx = getUserIdxByUserId(userId);
            Optional<User> user = userRepository.findByUserIdx(userIdx);

            if (user == null) {
                throw new BaseException(INVALID_USERIDX);
            }

            String status = user.get().getStatus();
            if (status.equals("INACTIVE")) {
                throw new BaseException(INACTIVE_USER);
            } else if (status.equals("BLACK")) {
                throw new BaseException(BLACK_USER);
            }

            List<Walk> walkList = walkRepository.findAllByStatusAndUserIdx("ACTIVE", userIdx);

            // Badge Url
            Optional<Badge> badge = badgeRepository.findById(user.get().getBadgeIdx());
            String badgeUrl = "";
            if (badge.get().getBadgeUrl() != null)
                badgeUrl = badge.get().getBadgeUrl();

            // Birth
            Timestamp birth = Timestamp.valueOf("1900-01-01 00:00:00");
            if (user.get().getBirth() != null)
                birth = Timestamp.valueOf(user.get().getBirth());

            return GetUserRes.builder()
                    .userIdx(user.get().getUserIdx())
                    .nickname(user.get().getNickname())
                    .username(AES128.decrypt(user.get().getUsername()))
                    .email(AES128.decrypt(user.get().getEmail()))
                    .status(user.get().getStatus())
                    .badgeIdx(user.get().getBadgeIdx())
                    .badgeUrl(badgeUrl)
                    .birth(birth)
                    .sex(user.get().getSex())
                    .height(user.get().getHeight())
                    .weight(user.get().getWeight())
                    .walkNumber(walkList.size())
                    .build();

        } catch (Exception exception) {
            exception.printStackTrace();
            throw new BaseException(DATABASE_ERROR);
        }
    }

    public GetMonthInfoRes getMonthInfoRes(String userId) throws BaseException {
        try {
            User user = checkUserStatus(userId);

            LocalDateTime now = LocalDateTime.now();
            int nowYear = now.getYear();
            int nowMonth = now.getMonthValue();

            List<String> goalDayList = goalService.getUserGoalDays(user.getUserIdx(), nowYear, nowMonth);
            List<GetDayRateResInterface> getDayRateResInterfaces = walkRepository.getRateByUserIdxAndStartAt(
                    user.getUserIdx(),
                    nowYear,
                    nowMonth);

            List<GetDayRateRes> getDayRateRes = getDayRateResInterfaces.stream()
                    .map(GetDayRateRes::new)
                    .collect(Collectors.toList());

            int dayCount = getDayRateRes.toArray().length;

            GetMonthTotalInterface getMonthTotalInterface = walkRepository.getMonthTotalByQuery(
                    user.getUserIdx(),
                    nowYear,
                    nowMonth
            );

            GetMonthTotal getMonthTotal = new GetMonthTotal(
                    getMonthTotalInterface.getMonthTotalMin(),
                    getMonthTotalInterface.getMonthTotalDistance(),
                    getMonthTotalInterface.getMonthPerCal());
            getMonthTotal.avgCal(dayCount);
            getMonthTotal.convertSecToMin();

            return new GetMonthInfoRes(goalDayList, getDayRateRes, getMonthTotal);
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }


    public List<GetFootprintCount> getMonthFootprints(String userId, int year, int month) throws BaseException {
        try {
            User user = checkUserStatus(userId);
            List<GetFootprintCount> getFootprintCounts = new ArrayList<>();

            LocalDate nowDate = LocalDate.now();
            LocalDate paramDate = LocalDate.of(year, month, 1);
            if (month < 1 || month > 12) {
                throw new BaseException(INVALID_DATE);
            }
            if (nowDate.isBefore(paramDate)) {
                return getFootprintCounts;
            }

            List<GetFootprintCountInterface> getMonthFootprints = walkRepository.getMonthFootCountByQuery(
                    user.getUserIdx(),
                    year,
                    month
            );

            for (GetFootprintCountInterface i : getMonthFootprints) {
                getFootprintCounts.add(
                        new GetFootprintCount(i.getDay(), i.getWalkCount())
                );
            }

            return getFootprintCounts;
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    @Transactional(readOnly = true)
    public User checkUserStatus(String userId) throws BaseException {
        User user = userRepository.getByUserId(userId)
                .orElseThrow(() -> new BaseException(INVALID_USERIDX));

        if (user.getStatus().equals("INACTIVE")) {
            throw new BaseException(INACTIVE_USER);
        } else if (user.getStatus().equals("BLACK")) {
            throw new BaseException(BLACK_USER);
        }
        return user;
    }


    public Integer getUserIdxByUserId(String userId) throws BaseException {
        return userRepository.getUserIdxByUserId(userId).orElseThrow(() -> new BaseException(NOT_EXIST_USER));
    }
}
