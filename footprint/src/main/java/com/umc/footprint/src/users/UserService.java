package com.umc.footprint.src.users;

import com.umc.footprint.config.BaseException;
import com.umc.footprint.config.BaseResponseStatus;
import com.umc.footprint.config.EncryptProperties;
import com.umc.footprint.src.AwsS3Service;
import com.umc.footprint.src.model.User;
import com.umc.footprint.src.model.Walk;
import com.umc.footprint.src.repository.TagRepository;
import com.umc.footprint.src.repository.UserRepository;
import com.umc.footprint.src.repository.WalkRepository;
import com.umc.footprint.src.model.*;
import com.umc.footprint.src.repository.*;
import com.umc.footprint.src.users.model.*;
import com.umc.footprint.src.walks.model.GetFootprintCountInterface;
import com.umc.footprint.src.walks.model.GetMonthTotalInterface;
import com.umc.footprint.utils.AES128;
import com.umc.footprint.utils.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

import static com.umc.footprint.config.BaseResponseStatus.*;

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
    private final EncryptProperties encryptProperties;
    private final GoalRepository goalRepository;
    private final GoalNextRepository goalNextRepository;
    private final GoalDayRepository goalDayRepository;
    private final GoalDayNextRepository goalDayNextRepository;
    private final HashtagRepository hashtagRepository;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final PhotoRepository photoRepository;
    private final FootprintRepository footprintRepository;

    @Autowired
    public UserService(UserDao userDao, UserRepository userRepository, TagRepository tagRepository,
                       JwtService jwtService, AwsS3Service awsS3Service, EncryptProperties encryptProperties,
                       WalkRepository walkRepository, GoalRepository goalRepository, GoalNextRepository goalNextRepository,
                       GoalDayRepository goalDayRepository, GoalDayNextRepository goalDayNextRepository,
                       HashtagRepository hashtagRepository, BadgeRepository badgeRepository, UserBadgeRepository userBadgeRepository,
                       PhotoRepository photoRepository, FootprintRepository footprintRepository) {
        this.userDao = userDao;
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
        this.jwtService = jwtService;
        this.awsS3Service = awsS3Service;
        this.encryptProperties = encryptProperties;
        this.walkRepository = walkRepository;
        this.goalRepository = goalRepository;
        this.goalNextRepository = goalNextRepository;
        this.goalDayRepository = goalDayRepository;
        this.goalDayNextRepository = goalDayNextRepository;
        this.hashtagRepository = hashtagRepository;
        this.badgeRepository = badgeRepository;
        this.userBadgeRepository = userBadgeRepository;
        this.photoRepository = photoRepository;
        this.footprintRepository = footprintRepository;
    }


    // ?????? ????????? ???????????? ??? ????????? ???????????? ???????????? ??????
    public List<GetTagRes> getTagResult(String userId, String tag) throws BaseException {
        try {
            Integer userIdx = userRepository.findByUserId(userId).getUserIdx();

            tag = "#" + tag;
            // 1. ?????? ????????? ?????? ????????? ?????????
            String encryptedTag = new AES128(encryptProperties.getKey()).encrypt(tag);

            // 2. ?????? ??????
            List<WalkHashtag> walkAndHashtagList = tagRepository.findAllWalkAndHashtag(encryptedTag, userIdx);

            // 3. ????????? ????????? response ?????? ?????????
            Set<String> dateSet = new HashSet<>();
            // ????????? ??????
            for (WalkHashtag walkHashtag : walkAndHashtagList) {
                dateSet.add(
                        // ?????? + ?????? ex) 2022. 5. 8 ???
                        walkHashtag.getEndAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")).replace(".0", ". ")
                        + " " + walkHashtag.getEndAt().getDayOfWeek().getDisplayName(TextStyle.NARROW, Locale.KOREAN)
                );
            }

            List<Integer> allByUserIdx = walkRepository.findAllByUserIdxAndStatusOrderByWalkIdx(userIdx, "ACTIVE").stream().map(Walk::getWalkIdx).collect(Collectors.toList());

            // response ??????
            List<GetTagRes> getTagResList = new ArrayList<>();
            // response ????????? ????????? walkIdx (?????? ?????? ?????????)
            List<Integer> responseWalkIdxList = new ArrayList<>();

            // ????????? ?????? ??????
            for (String date : dateSet) {
                // walk ?????? + hashtag ?????????
                Set<SearchWalk> searchWalkSet = new HashSet<>();

                for (WalkHashtag walkHashtag : walkAndHashtagList) {
                    // ?????? + ?????? ex) 2022. 5. 8 ???
                    String walkDate = walkHashtag.getEndAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")).replace(".0", ". ")
                            + " " + walkHashtag.getEndAt().getDayOfWeek().getDisplayName(TextStyle.NARROW, Locale.KOREAN);
                    // ?????? ?????? ????????? response ????????? ?????? && response??? ????????? ????????? ????????????
                    if (date.equals(walkDate) && responseWalkIdxList.indexOf(walkHashtag.getWalkIdx()) == -1) {
                        // hashtag ?????????
                        List<String> hashtagList = new ArrayList<>();
                        for (WalkHashtag hashtag : walkAndHashtagList) {
                            // ????????? ????????? ?????? ??? ??????
                            if (walkHashtag.getWalkIdx().equals(hashtag.getWalkIdx())) {
                                hashtagList.add(new AES128(encryptProperties.getKey()).decrypt(hashtag.getHashtag()));
                            }
                        }


                        searchWalkSet.add(
                                SearchWalk.builder()
                                        // ?????? ??????
                                        .userDateWalk(
                                                UserDateWalk.builder()
                                                        .walkIdx(allByUserIdx.indexOf(walkHashtag.getWalkIdx()) + 1)
                                                        .startTime(walkHashtag.getStartAt().format(DateTimeFormatter.ofPattern("HH:mm")))
                                                        .endTime(walkHashtag.getEndAt().format(DateTimeFormatter.ofPattern("HH:mm")))
                                                        .pathImageUrl(new AES128(encryptProperties.getKey()).decrypt(walkHashtag.getPathImageUrl()))
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

    // ?????? ?????? ??????(Patch)
    @Transactional(propagation = Propagation.NESTED, rollbackFor = Exception.class)
    public void modifyUserInfo(int userIdx, PatchUserInfoReq patchUserInfoReq) throws BaseException {
        try {
            int result = userDao.modifyUserInfo(userIdx, patchUserInfoReq);

            if (result == 0) { // ?????? ?????? ?????? ??????
                throw new BaseException(MODIFY_USERINFO_FAIL);
            }
        } catch (Exception exception) { // DB??? ????????? ?????? ?????? ?????? ???????????? ????????????.
            exception.printStackTrace();
            throw new BaseException(DATABASE_ERROR);
        }
    }

    // ?????? ?????? ??????(Patch)
    @Transactional(propagation = Propagation.NESTED, rollbackFor = Exception.class)
    public void modifyUserInfoJPA(int userIdx, PatchUserInfoReq patchUserInfoReq) throws BaseException {
        try {

            Optional<User> user = userRepository.findByUserIdx(userIdx);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            user.get().setBirth(LocalDate.parse(patchUserInfoReq.getBirth(),formatter).atStartOfDay());
            user.get().setNickname(patchUserInfoReq.getNickname());
            user.get().setSex(patchUserInfoReq.getSex());
            user.get().setHeight(patchUserInfoReq.getHeight());
            user.get().setWeight(patchUserInfoReq.getWeight());

            userRepository.save(user.get());

        } catch (Exception exception) { // DB??? ????????? ?????? ?????? ?????? ???????????? ????????????.
            exception.printStackTrace();
            throw new BaseException(DATABASE_ERROR);
        }
    }


    @Transactional(propagation = Propagation.NESTED, rollbackFor = Exception.class)
    public void modifyGoal(int userIdx, PatchUserGoalReq patchUserGoalReq) throws BaseException{
        try{
            int resultTime = userDao.modifyUserGoalTime(userIdx, patchUserGoalReq);
            if(resultTime == 0)
                throw new BaseException(BaseResponseStatus.MODIFY_USER_GOAL_FAIL);

            // ????????? ????????? ?????? ????????? ?????? ?????? ??????
            List<Integer> dayIdxList = new ArrayList<>();
            for (Integer dayIdx: patchUserGoalReq.getDayIdx()){
                if(dayIdx == 7)
                    dayIdxList.add(1);
                else
                    dayIdxList.add(dayIdx+1);
            }
            Collections.sort(dayIdxList);
            patchUserGoalReq.setDayIdx(dayIdxList);
            log.debug("dayIdxList : {}",dayIdxList);
            //

            int resultDay = userDao.modifyUserGoalDay(userIdx, patchUserGoalReq);
            if(resultDay == 0)
                throw new BaseException(BaseResponseStatus.MODIFY_USER_GOAL_FAIL);

        } catch(Exception exception){
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw new BaseException(BaseResponseStatus.DATABASE_ERROR);
        }
    }

    @Transactional(propagation = Propagation.NESTED, rollbackFor = Exception.class)
    public void modifyGoalJPA(int userIdx, PatchUserGoalReq patchUserGoalReq) throws BaseException {
        try{

            /** 1. UPDATE Goal  */
            Optional<GoalNext> userGoal = goalNextRepository.findByUserIdx(userIdx);

            userGoal.get().setWalkGoalTime(patchUserGoalReq.getWalkGoalTime());
            userGoal.get().setWalkTimeSlot(patchUserGoalReq.getWalkTimeSlot());
            userGoal.get().setUpdateAt(LocalDateTime.now());
            goalNextRepository.save(userGoal.get());

            /** 2. UPDATE GoalDay  */
            Optional<GoalDayNext> userGoalDay = goalDayNextRepository.findByUserIdx(userIdx);

            List<Integer> newDayIdxList = new ArrayList<>(List.of(0,0,0,0,0,0,0));
            List<Integer> dayIdxList = patchUserGoalReq.getDayIdx();

            for(Integer dayIdx : dayIdxList) {
                newDayIdxList.set(dayIdx-1,1);
            }

            userGoalDay.get().setMon(newDayIdxList.get(0));
            userGoalDay.get().setTue(newDayIdxList.get(1));
            userGoalDay.get().setWed(newDayIdxList.get(2));
            userGoalDay.get().setThu(newDayIdxList.get(3));
            userGoalDay.get().setFri(newDayIdxList.get(4));
            userGoalDay.get().setSat(newDayIdxList.get(5));
            userGoalDay.get().setSun(newDayIdxList.get(6));

            userGoalDay.get().setUpdateAt(LocalDateTime.now());

            goalDayNextRepository.save(userGoalDay.get());

        } catch(Exception exception){
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw new BaseException(BaseResponseStatus.DATABASE_ERROR);
        }
    }


    // ?????? userIdx??? ?????? Info ?????? ??????
    @Transactional(propagation = Propagation.NESTED, rollbackFor = Exception.class)
    public int postUserInfo(int userIdx, PatchUserInfoReq patchUserInfoReq) throws BaseException{
        try {
            int resultInfo = userDao.modifyUserInfo(userIdx, patchUserInfoReq);
            log.debug("resultInfo: {}", resultInfo);

            // ????????? ????????? ?????? ????????? ?????? ?????? ??????
            List<Integer> dayIdxList = new ArrayList<>();
            for (Integer dayIdx: patchUserInfoReq.getDayIdx()){
                if(dayIdx == 7)
                    dayIdxList.add(1);
                else
                    dayIdxList.add(dayIdx+1);
            }
            Collections.sort(dayIdxList);
            patchUserInfoReq.setDayIdx(dayIdxList);
            log.debug("dayIdxList : {}",dayIdxList);
            //

            int result = userDao.postGoal(userIdx, patchUserInfoReq);
            log.debug("result : {}", result);
            int resultNext = userDao.postGoalNext(userIdx, patchUserInfoReq);
            log.debug("resultNext: {}", resultNext);

            if(resultInfo == 0 || result == 0 || resultNext == 0)
                return 0;
            return 1;

        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    public PostLoginRes checkEmail(String email) throws BaseException {
        try {
            log.debug("email: {}", email);
            // checkExistFlag == true -> ?????? ?????? ??????
            // checkExistFlag == false -> ?????? ?????? ?????? ??????
            boolean checkExistFlag = userRepository.existsByEmail(email);

            if (checkExistFlag) {
                // email??? userId??? ?????? ??????
                User user = userRepository.findByEmail(email);
                PostLoginRes postLoginRes = PostLoginRes.builder()
                        .jwtId(user.getUserId())
                        .status(user.getStatus())
                        .build();
                // userId ?????????
                String jwtId = jwtService.createJwt(postLoginRes.getJwtId());
                // response??? ??????
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
        { // email ?????? ?????? ????????? status??? Done ?????? return
            try {
                String encryptEmail = new AES128(encryptProperties.getKey()).encrypt(postLoginReq.getEmail());
                PostLoginRes result = checkEmail(encryptEmail);
                log.debug("????????? status: {}", result.getStatus());
                // status: NONE -> ????????????(?????? ?????? db??? ?????? ??????)
                // status: ACTIVE -> ?????????
                // status: ACTIVE -> ?????? ?????? ??????
                switch (result.getStatus()) {
                    case "NONE":
                        // ?????????
                        String jwt = jwtService.createJwt(postLoginReq.getUserId());
                        // ?????? ?????? db??? ??????
                        postLoginReq.setEncryptedInfos(new AES128(encryptProperties.getKey()).encrypt(postLoginReq.getUsername()), encryptEmail);
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
    }

    public PostLoginRes modifyUserLogAt(String userId) throws BaseException {
        try {
            // ????????? ????????? ?????? ??????
            User user = userRepository.findByUserId(userId);

            LocalDateTime beforeLogAt = user.getLogAt();

            boolean checkMonthChangedFlag = true;

            // ?????? ?????? ??????
            if (beforeLogAt.getMonth() == LocalDateTime.now().getMonth()) {
                // ?????? ????????? ???????????? response??? ??????
                checkMonthChangedFlag = false;
            }

            // ?????? ??????????????? ?????? logAt??? ??????
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
    public void deleteUser(int userIdx) throws BaseException {
        try{
            // GoalNext ?????????
            userDao.deleteGoalNext(userIdx);

            // GoalDayNext ?????????
            userDao.deleteGoalDayNext(userIdx);

            // Goal ?????????
            userDao.deleteGoal(userIdx);

            // GoalDay ?????????
            userDao.deleteGoalDay(userIdx);
            // UserBadge ?????????
            userDao.deleteUserBadge(userIdx);

            // Tag ?????????
            userDao.deleteTag(userIdx);

            // Photo ????????? -> s3?????? ????????? url ?????? ?????? ??? ????????? ?????? ??????
            List<String> imageUrlList = userDao.getImageUrlList(userIdx); //S3?????? ?????? ??????
            for(String imageUrl : imageUrlList) {
                String decryptedImageUrl = new AES128(encryptProperties.getKey()).decrypt(imageUrl);
                String fileName = decryptedImageUrl.substring(decryptedImageUrl.lastIndexOf("/")+1); // ?????? ????????? ?????????
                awsS3Service.deleteFile(fileName);
            }
            userDao.deletePhoto(userIdx); //Photo ??????????????? ??????

            // Footprint ?????????
            userDao.deleteFootprint(userIdx);

            // Walk ????????? - ?????? ????????? S3 ????????? ??????
            List<String> pathImageUrlList = userDao.getPathImageUrlList(userIdx); //S3?????? ?????? ??????
            for(String imageUrl : pathImageUrlList) {
                String decryptedImageUrl = new AES128(encryptProperties.getKey()).decrypt(imageUrl);
                String fileName = decryptedImageUrl.substring(decryptedImageUrl.lastIndexOf("/") + 1); // ?????? ????????? ?????????
                awsS3Service.deleteFile(fileName);
            }
            userDao.deleteWalk(userIdx);

            // User ?????????
            userDao.deleteUser(userIdx);

        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    @Transactional(propagation = Propagation.NESTED, rollbackFor = Exception.class)
    public void deleteUserJPA(int userIdx) throws BaseException {
        try{
            // GoalNext ?????????
            Optional<GoalNext> goalNext = goalNextRepository.findByUserIdx(userIdx);
            goalNextRepository.deleteById(goalNext.get().getPlanIdx());

            // GoalDayNext ?????????
            Optional<GoalDayNext> goalDayNext = goalDayNextRepository.findByUserIdx(userIdx);
            goalDayNextRepository.deleteById(goalDayNext.get().getPlanIdx());

            // Goal ?????????
            List<Goal> goalList = goalRepository.findByUserIdx(userIdx);
            for(Goal goal : goalList){
                goalRepository.deleteById(goal.getPlanIdx());
            }

            // GoalDay ?????????
            List<GoalDay> goalDayList = goalDayRepository.findByUserIdx(userIdx);
            for(GoalDay goalDay : goalDayList){
                goalDayRepository.deleteById(goalDay.getPlanIdx());
            }

            // UserBadge ?????????
            List<UserBadge> userBadgeList = userBadgeRepository.findAllByUserIdx(userIdx);
            for(UserBadge userBadge: userBadgeList){
                userBadgeRepository.deleteById(userBadge.getCollectionIdx());
            }

            // Tag ?????????
            List<Tag> tagList = tagRepository.findAllByUserIdx(userIdx);
            for(Tag tag : tagList){
                tagRepository.deleteById(tag.getTagIdx());
            }

            // Photo ????????? -> s3?????? ????????? url ?????? ?????? ??? ????????? ?????? ??????
            List<Photo> photoList = photoRepository.findAllByUserIdx(userIdx);
            for(Photo photo : photoList){
                String decryptedImageUrl = new AES128(encryptProperties.getKey()).decrypt(photo.getImageUrl());
                String fileName = decryptedImageUrl.substring(decryptedImageUrl.lastIndexOf("/")+1); // ?????? ????????? ?????????
                awsS3Service.deleteFile(fileName);

                photoRepository.deleteById(photo.getPhotoIdx());
            }


            // Walk + Footprint ?????????
            List<Walk> walkList = walkRepository.findAllByUserIdx(userIdx);
            for(Walk walk : walkList){
                List<Footprint> footprintList = walk.getFootprintList();

                // Walk??? ???????????? Footprint ?????? ??????
                for(Footprint footprint : footprintList){
                    footprintRepository.deleteById(footprint.getFootprintIdx());
                }

                // Walk ????????? - ?????? ????????? S3 ????????? ??????
                String decryptedImageUrl = new AES128(encryptProperties.getKey()).decrypt(walk.getPathImageUrl());
                String fileName = decryptedImageUrl.substring(decryptedImageUrl.lastIndexOf("/") + 1); // ?????? ????????? ?????????
                awsS3Service.deleteFile(fileName);

                // Walk ??????
                walkRepository.deleteById(walk.getWalkIdx());
            }


            // User ?????????
            userRepository.deleteById(userIdx);

        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    public GetUserTodayRes getUserToday(int userIdx) throws BaseException{

        List<Walk> userWalkList = walkRepository.findAllByStatusAndUserIdx("ACTIVE", userIdx);
        System.out.println("userWalkList.size() = " + userWalkList.size());
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        double todayGoalRate = 0;
        int todayTotalTime = 0;
        double todayTotalDist = 0;
        int todayTotalCal = 0;

        for(Walk userWalk : userWalkList){
            if(userWalk.getStartAt().toLocalDate().isEqual(today)){
                // 1. ?????? ?????? ????????? ??????
                todayGoalRate += userWalk.getGoalRate();

                // 2. ?????? ?????? ?????? ?????? ??????
                Duration timeDiff = Duration.between(userWalk.getStartAt(),userWalk.getEndAt());
                todayTotalTime += timeDiff.getSeconds()/60;

                // 3. ?????? ?????? ?????? ?????? ??????
                todayTotalDist += userWalk.getDistance();

                // 4. ?????? ?????? ?????? ????????? ??????
                todayTotalCal += userWalk.getCalorie();
            }
        }
        // 1-1. ?????? ????????? 100 ????????? 100?????? ??????
        if(todayGoalRate > 100)
            todayGoalRate = 100;

        // 3-1. ?????? ?????? ????????? ???????????? ??????
        todayTotalDist = Math.floor(todayTotalDist*100)/100.0;

        // 5. ?????? ?????? ?????? ?????? ??????
        List<GoalDay> userGoalDayList = goalDayRepository.findByUserIdx(userIdx);
        GoalDay goalDay = GoalDay.builder().build();
        for(GoalDay gd : userGoalDayList ){
            LocalDate goalDayCreateAt = gd.getCreateAt().toLocalDate();
            if(goalDayCreateAt.getMonth().equals(LocalDate.now().getMonth()) && goalDayCreateAt.getYear() == LocalDate.now().getYear()){
                goalDay = gd;
                break;
            }
        }

//        /** ????????? ????????? ????????? ?????? ????????? ???????????? ??? ????????? ?????? ????????? 0?????? ?????? */
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


        List<Goal> userGoalList = goalRepository.findByUserIdx(userIdx);
        Goal userGoal = Goal.builder().build();
        for(Goal goal : userGoalList ){
            LocalDate goalCreateAt = goal.getCreateAt().toLocalDate();
            if(goalCreateAt.getMonth().equals(LocalDate.now().getMonth()) && goalCreateAt.getYear() == LocalDate.now().getYear()){
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

    public List<GetUserDateRes> getUserDate(int userIdx, String date) throws BaseException {

        try {

            List<Walk> userWalkList = walkRepository.findAllByStatusAndUserIdx("ACTIVE", userIdx);
            List<GetUserDateRes> getUserDateResList = new ArrayList<>();
            UserDateWalk userDateWalk;

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            int count = 0;

            for (Walk userWalk : userWalkList) {
                count ++;

                if(!userWalk.getStartAt().toLocalDate().equals(LocalDate.parse(date,formatter)))
                    continue;

                userDateWalk = UserDateWalk.builder()
                        .walkIdx(count)
                        .startTime(userWalk.getStartAt().format(DateTimeFormatter.ofPattern("HH:mm")))
                        .endTime(userWalk.getEndAt().format(DateTimeFormatter.ofPattern("HH:mm")))
                        .pathImageUrl(new AES128(encryptProperties.getKey()).decrypt(userWalk.getPathImageUrl()))
                        .build();

                List<Footprint> footprintList = userWalk.getFootprintList();
                ArrayList<String> tagString = new ArrayList<>();
                for (Footprint footprint : footprintList) {
                    List<Tag> tagList = footprint.getTagList();

                    for (Tag tag : tagList) {
                        if (tag.getStatus().equals("ACTIVE")) {
                            tagString.add(new AES128(encryptProperties.getKey()).decrypt(tag.getHashtag().getHashtag()));
                        }
                    }
                }
                getUserDateResList.add(GetUserDateRes.builder()
                        .hashtag(tagString)
                        .userDateWalk(userDateWalk)
                        .build());

            }

            return getUserDateResList;
        } catch(Exception exception){
            throw new BaseException(DATABASE_ERROR);
        }

    }

    public GetUserGoalRes getUserGoal(int userIdx) throws BaseException{

        /** 1. ????????? ?????? ????????? */
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        String month = today.format(DateTimeFormatter.ofPattern("yyyy??? MM???"));

        /** 2. get UserGoalDay -> dayIdxList */
        List<GoalDay> userGoalDayList = goalDayRepository.findByUserIdx(userIdx);
        GoalDay userGoalDay = GoalDay.builder().build();
        for(GoalDay goalDay : userGoalDayList ){
            LocalDate goalDayCreateAt = goalDay.getCreateAt().toLocalDate();
            if(goalDayCreateAt.getMonth().equals(LocalDate.now().getMonth()) && goalDayCreateAt.getYear() == LocalDate.now().getYear()){
                userGoalDay = goalDay;
                break;
            }
        }

        List<Integer> dayIdxList = new ArrayList<>();
        if(userGoalDay.getMon().equals(1))
            dayIdxList.add(1);
        if(userGoalDay.getTue().equals(1))
            dayIdxList.add(2);
        if(userGoalDay.getWed().equals(1))
            dayIdxList.add(3);
        if(userGoalDay.getThu().equals(1))
            dayIdxList.add(4);
        if(userGoalDay.getFri().equals(1))
            dayIdxList.add(5);
        if(userGoalDay.getSat().equals(1))
            dayIdxList.add(6);
        if(userGoalDay.getSun().equals(1))
            dayIdxList.add(7);

        /** 3. get UserGoalTime */
        List<Goal> userGoalList = goalRepository.findByUserIdx(userIdx);

        Goal userGoal = Goal.builder().build();
        for(Goal goal : userGoalList ){
            LocalDate goalCreateAt = goal.getCreateAt().toLocalDate();
            if(goalCreateAt.getMonth().equals(LocalDate.now().getMonth()) && goalCreateAt.getYear() == LocalDate.now().getYear()){
                userGoal = goal;
                break;
            }
        }

        UserGoalTime userGoalTime = UserGoalTime.builder()
                .walkGoalTime(userGoal.getWalkGoalTime())
                .walkTimeSlot(userGoal.getWalkTimeSlot())
                .build();

        /** 4. Check GoalNext Modified */
        boolean goalNextModified;
        Optional<GoalNext> userGoalNext = goalNextRepository.findByUserIdx(userIdx);

        LocalDateTime goalNextCreateAt = userGoalNext.get().getCreateAt();
        LocalDateTime goalNextUpdateAt = userGoalNext.get().getUpdateAt();

        if(today.getYear() == goalNextCreateAt.getYear() && today.getMonth() == goalNextCreateAt.getMonth()){
            System.out.println("goalNextCreateAt.equals(goalNextUpdateAt) = " + goalNextCreateAt.equals(goalNextUpdateAt));
            if(goalNextCreateAt.equals(goalNextUpdateAt)){
                goalNextModified = false;
            } else {
              goalNextModified = true;
            }

        } else{
            if(today.getYear() == goalNextUpdateAt.getYear() && today.getMonth() == goalNextUpdateAt.getMonth()){
                goalNextModified = true;
            } else {
                goalNextModified = false;
            }

        }

        return GetUserGoalRes.builder()
                .month(month)
                .dayIdx(dayIdxList)
                .userGoalTime(userGoalTime)
                .goalNextModified(goalNextModified)
                .build();


    }

    public GetUserGoalRes getUserGoalNext(int userIdx) throws BaseException{

        /** 1. ????????? ?????? ????????? */
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        String month = today.plusMonths(1).format(DateTimeFormatter.ofPattern("yyyy??? MM???"));

        /** 2. get UserGoalDay -> dayIdxList */
        Optional<GoalDayNext> userGoalDayNext = goalDayNextRepository.findByUserIdx(userIdx);

        List<Integer> dayIdxList = new ArrayList<>();
        if(userGoalDayNext.get().getMon().equals(1))
            dayIdxList.add(1);
        if(userGoalDayNext.get().getTue().equals(1))
            dayIdxList.add(2);
        if(userGoalDayNext.get().getWed().equals(1))
            dayIdxList.add(3);
        if(userGoalDayNext.get().getThu().equals(1))
            dayIdxList.add(4);
        if(userGoalDayNext.get().getFri().equals(1))
            dayIdxList.add(5);
        if(userGoalDayNext.get().getSat().equals(1))
            dayIdxList.add(6);
        if(userGoalDayNext.get().getSun().equals(1))
            dayIdxList.add(7);

        /** 3. get UserGoalTime */
        Optional<GoalNext> userGoalNext = goalNextRepository.findByUserIdx(userIdx);
        UserGoalTime userGoalTime = UserGoalTime.builder()
                    .walkGoalTime(userGoalNext.get().getWalkGoalTime())
                    .walkTimeSlot(userGoalNext.get().getWalkTimeSlot())
                    .build();

        /** 4. Check GoalNext Modified */
        boolean goalNextModified;

        LocalDateTime goalNextCreateAt = userGoalNext.get().getCreateAt();
        LocalDateTime goalNextUpdateAt = userGoalNext.get().getUpdateAt();

        if(today.getYear() == goalNextCreateAt.getYear() && today.getMonth() == goalNextCreateAt.getMonth()){
            System.out.println("goalNextCreateAt.equals(goalNextUpdateAt) = " + goalNextCreateAt.equals(goalNextUpdateAt));
            if(goalNextCreateAt.equals(goalNextUpdateAt)){
                goalNextModified = false;
            } else {
                goalNextModified = true;
            }

        } else{
            if(today.getYear() == goalNextUpdateAt.getYear() && today.getMonth() == goalNextUpdateAt.getMonth()){
                goalNextModified = true;
            } else {
                goalNextModified = false;
            }

        }

        return GetUserGoalRes.builder()
                .month(month)
                .dayIdx(dayIdxList)
                .userGoalTime(userGoalTime)
                .goalNextModified(goalNextModified)
                .build();
    }

    public UserInfoAchieve getUserInfoAchieve(int userIdx){

        List<Walk> walkList = walkRepository.findAllByStatusAndUserIdx("ACTIVE", userIdx);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        System.out.println("CHECK POINT 1");

        List<Walk> todayWalkList = walkList.stream()
                                    .filter(s -> s.getStartAt().toLocalDate().equals(today))
                                    .collect(Collectors.toList());

        System.out.println("CHECK POINT 2");

        /** 1. ?????? ?????? ????????? ?????? = todayGoalRate */
        double totalGoalRate = 0;
        for(Walk todayWalk : todayWalkList){
            totalGoalRate += todayWalk.getGoalRate();
        }

        System.out.println("CHECK POINT 3");

        /** 2. ????????? ?????? ????????? ?????? = monthGoalRate */
        int monthGoalRate = calcMonthGoalRate(userIdx,0);

        System.out.println("CHECK POINT 4");

        /** 3. ?????? ?????? ?????? = userWalkCount */
        int userWalkCount = walkList.size();

        System.out.println("CHECK POINT 5");

        return new UserInfoAchieve((int)totalGoalRate,monthGoalRate,userWalkCount);
    }

    public UserInfoStat getUserInfoStat(int userIdx){
        // [ 1. ?????? 3??? ?????? ????????? ?????? ?????? ] = List<String> mostWalkDay & List<Double> userWeekDayRate

        // 1-1. ?????? 3??? ?????? ????????? ?????? ??? ??????
        List<Integer> userWeekDayCount = new ArrayList<>(Arrays.asList(0,0,0,0,0,0,0));
        List<Double> userWeekDayRate = new ArrayList<>();

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        List<Walk> userWalkList = walkRepository.findAllByStatusAndUserIdx("ACTIVE", userIdx);

        for(Walk userWalk : userWalkList){
            if(userWalk.getStartAt().toLocalDate().isAfter(today.minusMonths(3))){
                DayOfWeek dayOfWeek = userWalk.getStartAt().getDayOfWeek();
                if(dayOfWeek.equals(DayOfWeek.SUNDAY)){
                    userWeekDayCount.set(0,userWeekDayCount.get(0)+1);
                } else if(dayOfWeek.equals(DayOfWeek.MONDAY)){
                    userWeekDayCount.set(1,userWeekDayCount.get(1)+1);
                } else if(dayOfWeek.equals(DayOfWeek.TUESDAY)){
                    userWeekDayCount.set(2,userWeekDayCount.get(2)+1);
                } else if(dayOfWeek.equals(DayOfWeek.WEDNESDAY)){
                    userWeekDayCount.set(3,userWeekDayCount.get(3)+1);
                } else if(dayOfWeek.equals(DayOfWeek.THURSDAY)){
                    userWeekDayCount.set(4,userWeekDayCount.get(4)+1);
                } else if(dayOfWeek.equals(DayOfWeek.FRIDAY)){
                    userWeekDayCount.set(5,userWeekDayCount.get(5)+1);
                } else if(dayOfWeek.equals(DayOfWeek.SATURDAY)){
                    userWeekDayCount.set(6,userWeekDayCount.get(6)+1);
                }
            }
        }

        // 1-2. ?????? 3??? ?????? ?????? ?????? ??? ?????????
        int entireCount = 0;
        for(Integer dayCount : userWeekDayCount){
            entireCount += dayCount;
        }
        // 1-3. ?????? ????????? ?????? ?????? ?????? (?????? max ????????? ?????? return) = List<String> mostWalkDay
        List<String> mostWalkDay = new ArrayList<>();

        if(entireCount == 0) // ?????? 3????????? ?????? ????????? ?????????
            mostWalkDay.add("?????? 3????????? ????????? ?????? ????????????");
        else{
            int max = Collections.max(userWeekDayCount);

            for (int i=0; i<userWeekDayCount.size(); i++){
                if(userWeekDayCount.get(i) == max) {
                    switch (i) {
                        case 0:
                            mostWalkDay.add("???");
                            break;
                        case 1:
                            mostWalkDay.add("???");
                            break;
                        case 2:
                            mostWalkDay.add("???");
                            break;
                        case 3:
                            mostWalkDay.add("???");
                            break;
                        case 4:
                            mostWalkDay.add("???");
                            break;
                        case 5:
                            mostWalkDay.add("???");
                            break;
                        case 6:
                            mostWalkDay.add("???");
                            break;
                    }
                }
            }
        }

        // 1-4. ????????? ?????? ?????????
        // *** ?????? : ??? ??? ??? ??? ??? ??? ??? ***
        // *** ?????? : ??? ??? ??? ??? ??? ??? ??? ***
        for(Integer dayCount : userWeekDayCount){
            if (entireCount == 0)
                userWeekDayRate.add(0.0);
            else
                userWeekDayRate.add(dayCount/(double)entireCount*100);
        }

        // [ 2. ?????? 6??? ?????? ?????? ?????? ?????? ] = thisMonthWalkCount + List<Integer> monthlyWalkCount
        // List ?????? : -6??? , -5??? , ... , ?????? , ????????? (??? 7??? element)

        // 2-1. ????????? ?????? ?????? ????????????
        int thisMonthWalkCount = userWalkList.size();

        // 2-2. ?????? 6??? ?????? ?????? ?????? ?????? ?????? ????????????
        List<Integer> monthlyWalkCount = new ArrayList<>(Arrays.asList(0,0,0,0,0,0,0));
        for(Walk userWalk : userWalkList){
            if(userWalk.getStartAt().getMonth().equals(today.minusMonths(5).getMonth())){
                monthlyWalkCount.set(1,monthlyWalkCount.get(1)+1);
            }else if(userWalk.getStartAt().getMonth().equals(today.minusMonths(4).getMonth())){
                monthlyWalkCount.set(2,monthlyWalkCount.get(2)+1);
            }else if(userWalk.getStartAt().getMonth().equals(today.minusMonths(3).getMonth())){
                monthlyWalkCount.set(3,monthlyWalkCount.get(3)+1);
            }else if(userWalk.getStartAt().getMonth().equals(today.minusMonths(2).getMonth())){
                monthlyWalkCount.set(4,monthlyWalkCount.get(4)+1);
            }else if(userWalk.getStartAt().getMonth().equals(today.minusMonths(1).getMonth())){
                monthlyWalkCount.set(5,monthlyWalkCount.get(5)+1);
            }else if(userWalk.getStartAt().getMonth().equals(today.getMonth())){
                monthlyWalkCount.set(6,monthlyWalkCount.get(6)+1);
            }
        }

        // [ 3. ?????? 5??? ?????? ?????? ????????? & ?????? ????????? ] = List<Integer>monthlyGoalRate + avgGoalRate
        // List ?????? : ??????, -5??? , ... , ?????? , ????????? (??? 7??? element)
        List<Integer> monthlyGoalRate = new ArrayList<>();
        int sumGoalRate = 0;

        // ?????? ??? + ?????? 5??? ????????? ?????? ????????? ?????? by getMonthGoalRate()
        for(int i=5; i>=0 ; i--) {
            int goalRate = calcMonthGoalRate(userIdx, i); // Method getMonthGoalRate
            if (goalRate > 100) // 100 ????????? 100?????? ??????
                goalRate = 100;
            monthlyGoalRate.add(goalRate);
            sumGoalRate += monthlyGoalRate.get(5-i);
        }

        int avgGoalRate = (int)((double)sumGoalRate / 6);

        // monthlyGoalRate index 0??? avgGoalRate ??????
        monthlyGoalRate.add(0,avgGoalRate);

        return new UserInfoStat(mostWalkDay,userWeekDayRate,thisMonthWalkCount,monthlyWalkCount,monthlyGoalRate.get(6),monthlyGoalRate);

    }

    // UserSchedule
    // ?????? ?????? ?????????
    // GoalNext -> Goal
    public void updateGoal(){

        // 1. GoalNext ????????? ?????? ????????? ??????
        List<GoalNext> entireGoalNext = goalNextRepository.findAll();

        // 2. Goal ???????????? 1??? ?????? ????????? ??????
        for( GoalNext goalNext : entireGoalNext){
            goalRepository.save(Goal.builder()
                    .walkGoalTime(goalNext.getWalkGoalTime())
                    .walkTimeSlot(goalNext.getWalkTimeSlot())
                    .userIdx(goalNext.getUserIdx())
                    .createAt(LocalDateTime.now())
                    .build());
        }

    }

    // UserSchedule
    // ?????? ?????? ?????? ?????????
    // GoalDayNext -> GoalDay
    public void updateGoalDay(){

        // 1. GoalDayNext ????????? ?????? ????????? ??????
        List<GoalDayNext> entireGoalDayNext = goalDayNextRepository.findAll();

        // 2. GoalDay ???????????? 1??? ?????? ????????? ??????
        for( GoalDayNext goalDayNext : entireGoalDayNext){
            goalDayRepository.save(GoalDay.builder()
                            .mon(goalDayNext.getMon())
                            .tue(goalDayNext.getTue())
                            .wed(goalDayNext.getWed())
                            .thu(goalDayNext.getThu())
                            .fri(goalDayNext.getFri())
                            .sat(goalDayNext.getSat())
                            .sun(goalDayNext.getSun())
                            .userIdx(goalDayNext.getUserIdx())
                            .createAt(LocalDateTime.now())
                            .build());
        }

    }

    public int calcMonthGoalRate(int userIdx, int beforeMonth){

        // 0. ?????? ?????? ????????? ?????? ????????? ????????? ??????
        List<Goal> userGoalList = goalRepository.findByUserIdx(userIdx);
        boolean isGoalExist = false;
        for(Goal goal : userGoalList){
            LocalDate goalCreateAt = goal.getCreateAt().toLocalDate();
            if(goalCreateAt.getMonth().equals(LocalDate.now().minusMonths(beforeMonth).getMonth()) && goalCreateAt.getYear() == LocalDate.now(ZoneId.of("Asia/Seoul")).minusMonths(beforeMonth).getYear()){
                isGoalExist = true;
                break;
            }
        }
        if(isGoalExist == false){
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

        // 1. ???????????? ????????? ??? ?????? ?????? ?????? ?????? (??? ??????)
        int userMonthWalkTime = 0;
        for(Walk monthWalk : monthWalkList){
            System.out.println("Duration.between(monthWalk.getStartAt(),monthWalk.getEndAt()) = " + Duration.between(monthWalk.getStartAt(),monthWalk.getEndAt()).getSeconds());
            userMonthWalkTime += (int)Duration.between(monthWalk.getStartAt(),monthWalk.getEndAt()).getSeconds();
        }

        System.out.println("CHECK POINT. calc 4");

        // 2. ????????? ???????????? ??????
        List<GoalDay> userGoalDayList = goalDayRepository.findByUserIdx(userIdx);
        GoalDay userGoalDay = GoalDay.builder().build();
        for(GoalDay goalDay : userGoalDayList ){
            LocalDate goalDayCreateAt = goalDay.getCreateAt().toLocalDate();
            if(goalDayCreateAt.getMonth().equals(LocalDate.now().getMonth()) && goalDayCreateAt.getYear() == LocalDate.now().getYear()){
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
        if(userGoalDay.getMon().equals(1))
            dayIdxList.add(1);
        if(userGoalDay.getTue().equals(1))
            dayIdxList.add(2);
        if(userGoalDay.getWed().equals(1))
            dayIdxList.add(3);
        if(userGoalDay.getThu().equals(1))
            dayIdxList.add(4);
        if(userGoalDay.getFri().equals(1))
            dayIdxList.add(5);
        if(userGoalDay.getSat().equals(1))
            dayIdxList.add(6);
        if(userGoalDay.getSun().equals(1))
            dayIdxList.add(7);

        System.out.println("CHECK POINT. calc 6");

        // 2-2. ????????? ??? ????????? ?????? ?????? ??????
        // 2-2-1. ????????? ??? ?????? ?????? ????????????
        LocalDate month = LocalDate.now().minusMonths(beforeMonth);
        int monthLength = month.lengthOfMonth();

        // 2-2-2. ?????? ??? ??? ?????? ????????????
        // *** 1:??? / 2:??? / ... / 7:??? ***
        LocalDate firstDay = LocalDate.of(month.getYear(), month.getMonth(),1);
        DayOfWeek dayOfWeek = firstDay.getDayOfWeek();
        int firstDayIdx = dayOfWeek.getValue(); // ??? ?????? ?????????

        // 2-2-3. ??? ?????? ?????? ??? ?????? ?????? ??? ??????
        int weekNum = monthLength/7;
        int moreDay = monthLength%7;

        // 2-2-4. ?????? ?????? ????????? ?????? ??????
        // dayCountArray[0] = ?????? ?????? ??? ??????(2021??? 1??? ?????? ?????????)
        int[] dayCountArray = {weekNum,weekNum,weekNum,weekNum,weekNum,weekNum,weekNum};
        for (int i=0; i<moreDay; i++){
            dayCountArray[i]++;
        }

        System.out.println("CHECK POINT. calc 7");

        // 2-3. ?????? ??? ?????? ?????? ??????
        // 2-3-1. GoalDay Table??? true ??? ????????? countDay??? sum
        int countDay =0 ; // ?????? ?????? ????????? ?????? ??????
        int loopIdx = firstDayIdx; // firstDayIdx ??? ???????????? loop ??? ??? idx
        for (int i=0; i<7; i++) {
            switch (loopIdx % 7) {
                case 1: // ?????????
                    if(userGoalDay.getMon() == 1)
                        countDay += dayCountArray[i];
                    break;
                case 2: // ?????????
                    if(userGoalDay.getTue() == 1)
                        countDay += dayCountArray[i];
                    break;
                case 3: // ?????????
                    if(userGoalDay.getWed() == 1)
                        countDay += dayCountArray[i];
                    break;
                case 4: // ?????????
                    if(userGoalDay.getThu() == 1)
                        countDay += dayCountArray[i];
                    break;
                case 5: // ?????????
                    if(userGoalDay.getFri() == 1)
                        countDay += dayCountArray[i];
                    break;
                case 6: // ?????????
                    if(userGoalDay.getSat() == 1)
                        countDay += dayCountArray[i];
                    break;
                case 0: // ?????????
                    if(userGoalDay.getSun() == 1)
                        countDay += dayCountArray[i];
                    break;
            }
            loopIdx++;
        }

        // 2-3-2. ?????? ?????? ?????? ?????? ??????
        Goal userGoal = Goal.builder().build();
        for(Goal goal : userGoalList ){
            LocalDate goalCreateAt = goal.getCreateAt().toLocalDate();
            System.out.println("beforeMonth = " + beforeMonth);
            if(goalCreateAt.getMonth().equals(LocalDate.now().minusMonths(beforeMonth).getMonth()) && goalCreateAt.getYear() == LocalDate.now(ZoneId.of("Asia/Seoul")).minusMonths(beforeMonth).getYear()){
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

        // 2-3-3. ?????? ?????? ??????
        int userMonthGoalTime = countDay * userWalkGoalTime;
        System.out.println("userMonthGoalTime = " + userMonthGoalTime);

        // 3. ????????? ?????? ????????? ??????
        int monthGoalRate = (int)((userMonthWalkTime/ (double)( userMonthGoalTime*60 )) * 100);
        System.out.println("monthGoalRate = " + monthGoalRate);

        if(monthGoalRate > 100)
            monthGoalRate = 100;

        return monthGoalRate;
    }

    public GetUserRes getUser(int userIdx) throws BaseException {
        try{

            Optional<User> user = userRepository.findByUserIdx(userIdx);

            if(user == null){
                throw new BaseException(INVALID_USERIDX);
            }

            String status = user.get().getStatus();
            if (status.equals("INACTIVE")) {
                throw new BaseException(INACTIVE_USER);
            }
            else if (status.equals("BLACK")) {
                throw new BaseException(BLACK_USER);
            }

            List<Walk> walkList = walkRepository.findAllByStatusAndUserIdx("ACTIVE", userIdx);

            // Badge Url
            Optional<Badge> badge = badgeRepository.findById(user.get().getBadgeIdx());
            String badgeUrl = "";
            if(badge.get().getBadgeUrl() != null)
                badgeUrl = badge.get().getBadgeUrl();

            // Birth
            Timestamp birth = Timestamp.valueOf("1900-01-01 00:00:00");
            if(user.get().getBirth() != null)
                birth = Timestamp.valueOf(user.get().getBirth());

            return GetUserRes.builder()
                    .userIdx(user.get().getUserIdx())
                    .nickname(user.get().getNickname())
                    .username(new AES128(encryptProperties.getKey()).decrypt(user.get().getUsername()))
                    .email(new AES128(encryptProperties.getKey()).decrypt(user.get().getEmail()))
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

            List<String> goalDayList = getUserGoalDays(user.getUserIdx(), nowYear, nowMonth);
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


    public List<String> getUserGoalDays(int userIdx, int year, int month) throws BaseException {
        GetGoalDays goalDay = new GetGoalDays(goalDayRepository.selectOnlyGoalDayByQuery(userIdx, year, month)
                .orElseThrow(()->new BaseException(NOT_EXIST_USER_IN_GOAL)));

        return convertGoalDayBoolToString(goalDay);
    }

    public List<String> convertGoalDayBoolToString(GetGoalDays goalDay) {
        List<String> goalDayString = new ArrayList<>();

        if(goalDay.getSun()==1) {
            goalDayString.add("SUN");
        }
        if(goalDay.getMon()==1) {
            goalDayString.add("MON");
        }
        if(goalDay.getTue()==1) {
            goalDayString.add("TUE");
        }
        if(goalDay.getWed()==1) {
            goalDayString.add("WED");
        }
        if(goalDay.getThu()==1) {
            goalDayString.add("THU");
        }
        if(goalDay.getFri()==1) {
            goalDayString.add("FRI");
        }
        if(goalDay.getSat()==1) {
            goalDayString.add("SAT");
        }
        return goalDayString;
    }

    public List<GetFootprintCount> getMonthFootprints(String userId, int year, int month) throws BaseException {
        try {
            User user = checkUserStatus(userId);
            List<GetFootprintCount> getFootprintCounts = new ArrayList<>();

            LocalDate nowDate = LocalDate.now();
            LocalDate paramDate = LocalDate.of(year,month,1);
            if(month<1 || month>12) {
                throw new BaseException(INVALID_DATE);
            }
            if(nowDate.isBefore(paramDate)) {
                return getFootprintCounts;
            }

            List<GetFootprintCountInterface> getMonthFootprints = walkRepository.getMonthFootCountByQuery(
                    user.getUserIdx(),
                    year,
                    month
            );

            for(GetFootprintCountInterface i : getMonthFootprints) {
                getFootprintCounts.add(
                        new GetFootprintCount(i.getDay(), i.getWalkCount())
                );
            }

            return getFootprintCounts;
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    public GetUserBadges getUserBadges(String userId) throws BaseException {
        try {
            User user = checkUserStatus(userId);

            BadgeInfo badgeInfo = new BadgeInfo(badgeRepository.getByBadgeIdx(user.getBadgeIdx())); //?????? ??????
            List<UserBadge> userBadges = userBadgeRepository.findAllByUserIdxAndStatus(user.getUserIdx(), "ACTIVE")
                    .orElseThrow(() -> new BaseException(NO_BADGE_USER));

            List<Badge> badges = badgeRepository.findByBadgeIdx(
                    userBadges.stream().map(UserBadge::getBadgeIdx).collect(Collectors.toList())
            );

            List<BadgeOrder> badgeOrders = new ArrayList<>();

            for(Badge b : badges) {
                int badgeOrderNum = 0;
                if(b.getBadgeDate().toString().startsWith("1900")) {
                    if(b.getBadgeIdx()==0) continue;
                    badgeOrderNum = b.getBadgeIdx()-1;
                } else {
                    LocalDate badgeDate = b.getBadgeDate();
                    if(LocalDate.now().getYear()!= badgeDate.getYear()) continue; //?????? ????????? ????????? ?????? X
                    badgeOrderNum = badgeDate.getMonthValue()+7;
                }
                badgeOrders.add(
                        BadgeOrder.builder()
                                .badgeIdx(b.getBadgeIdx())
                                .badgeName(b.getBadgeName())
                                .badgeUrl(b.getBadgeUrl())
                                .badgeDate(b.getBadgeDate().toString())
                                .badgeOrder(badgeOrderNum)
                                .build()
                );
            }
            return GetUserBadges.builder()
                    .repBadgeInfo(badgeInfo)
                    .badgeList(badgeOrders)
                    .build();
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    @Transactional(propagation = Propagation.NESTED, rollbackFor = Exception.class)
    public BadgeInfo modifyRepBadge(String userId, int badgeIdx) throws BaseException {
        try {
            User user = checkUserStatus(userId);

            // ?????? ????????? Badge ???????????? ???????????? ?????????????
            if(!badgeRepository.existsByBadgeIdx(badgeIdx)) {
                throw new BaseException(INVALID_BADGEIDX);
            }

            // ????????? ?????? ????????? ?????? ??????, ACTIVE ?????????????
            if(userBadgeRepository.checkUserHasBadge(user.getUserIdx(), badgeIdx)==0) {
                throw new BaseException(NOT_EXIST_USER_BADGE);
            }

            user.setBadgeIdx(badgeIdx);
            userRepository.save(user);

            return new BadgeInfo(badgeRepository.getByBadgeIdx(user.getBadgeIdx()));
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    @Transactional(readOnly = true)
    public User checkUserStatus(String userId) throws BaseException{
        User user = userRepository.getByUserId(userId)
                .orElseThrow(()-> new BaseException(INVALID_USERIDX));

        if (user.getStatus().equals("INACTIVE")) {
            throw new BaseException(INACTIVE_USER);
        }
        else if (user.getStatus().equals("BLACK")) {
            throw new BaseException(BLACK_USER);
        }
        return user;
    }

    public BadgeInfo getMonthlyBadgeStatus(String userId) throws BaseException {
        try {
            User user = checkUserStatus(userId);

            int rate = calcMonthGoalRate(user.getUserIdx(), 1); //????????? ?????????

            LocalDate now = LocalDate.now();
            int year = now.getYear();
            int month = now.getMonthValue();

            if(month == 1) { //????????? 1????????? ???????????? ?????? 12?????? ??????
                year--;
                month = 12;
            } else {
                month--;
            }

            GetGoalDays getGoalDays = new GetGoalDays(
                    goalDayRepository.selectOnlyGoalDayByQuery(
                            user.getUserIdx(),
                            year,
                            month
                    ).orElseThrow(()->new BaseException(NOT_EXIST_USER_IN_PREV_GOAL))
            );

            double goalCount = 0; //???????????? ????????? ???????????? ?????? ??????
            Calendar cal = new GregorianCalendar(year, month-1, 1); //0-11?????? ??????
            do {
                int day = cal.get(Calendar.DAY_OF_WEEK);
                if (day == Calendar.SUNDAY && getGoalDays.getSun()==1) {
                    goalCount++;
                }
                else if (day == Calendar.MONDAY && getGoalDays.getMon()==1) {
                    goalCount++;
                }
                else if (day == Calendar.TUESDAY && getGoalDays.getTue()==1) {
                    goalCount++;
                }
                else if (day == Calendar.WEDNESDAY && getGoalDays.getWed()==1) {
                    goalCount++;
                }
                else if (day == Calendar.THURSDAY && getGoalDays.getThu()==1) {
                    goalCount++;
                }
                else if (day == Calendar.FRIDAY && getGoalDays.getFri()==1) {
                    goalCount++;
                }
                else if (day == Calendar.SATURDAY && getGoalDays.getSat()==1) {
                    goalCount++;
                }
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }  while (cal.get(Calendar.MONTH) == month);

            // ????????? ?????? ????????? ????????? ??????
            List<Integer> walkDays = walkRepository.getDayOfWeekByQuery(user.getUserIdx());

            // ?????? ???????????? ????????????
            // ?????? ????????? ?????? ???????????? ????????? count
            double walkCount = 0; //??????????????? ????????? ??????

            for(int i=0;i<walkDays.size();i++) {
                int walkday = (int) walkDays.get(i);
                switch (walkday) {
                    case 1:
                        if(getGoalDays.getSun()==1) {
                            walkCount++;
                        }
                        break;
                    case 2:
                        if(getGoalDays.getMon()==1) {
                            walkCount++;
                        }
                        break;
                    case 3:
                        if(getGoalDays.getTue()==1) {
                            walkCount++;
                        }
                        break;
                    case 4:
                        if(getGoalDays.getWed()==1) {
                            walkCount++;
                        }
                        break;
                    case 5:
                        if(getGoalDays.getThu()==1) {
                            walkCount++;
                        }
                        break;
                    case 6:
                        if(getGoalDays.getFri()==1) {
                            walkCount++;
                        }
                        break;
                    case 7:
                        if(getGoalDays.getSat()==1) {
                            walkCount++;
                        }
                        break;
                }
            }

            double walkRate = (walkCount/goalCount) * 100;

            /*
             * MASTER - ?????? ?????? ??? 80% ?????? / ????????? 90%
             * PRO - ?????? ?????? ??? 50% ?????? / ????????? 70%
             * LOVER - ?????? ?????? ?????? ?????? / ????????? 50%
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

            if(badgeNum == -1) { //?????? ?????? ????????? ????????? ?????? ??????
                return null;
            }

            LocalDate badgeDate = LocalDate.of(year, month, badgeNum); // ????????? ????????? date

            BadgeInfo badgeInfo = new BadgeInfo(badgeRepository.getByBadgeDate(badgeDate));


            //TODO: ??????????????? ?????? ??? ?????? ????????? ?????? - ??? ?????? ??????, ??????, ????????? ??? ????????? ACTIVE??? ???????????? ???!


            //UserBadge ???????????? ?????? ?????? ??????
            userBadgeRepository.save(
                    UserBadge.builder()
                            .badgeIdx(badgeInfo.getBadgeIdx())
                            .userIdx(user.getUserIdx())
                            .status("ACTIVE")
                    .build()
            );

            return badgeInfo;
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

}
