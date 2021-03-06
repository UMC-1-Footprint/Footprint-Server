package com.umc.footprint.src.walks;

import com.umc.footprint.config.BaseException;
import com.umc.footprint.config.EncryptProperties;
import com.umc.footprint.src.footprints.FootprintDao;
import com.umc.footprint.src.model.*;
import com.umc.footprint.src.repository.*;
import com.umc.footprint.src.walks.model.*;
import com.umc.footprint.utils.AES128;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.umc.footprint.config.BaseResponseStatus.*;
import static com.umc.footprint.config.Constant.MINUTES_TO_SECONDS;


@Slf4j
@Service
@RequiredArgsConstructor
public class WalkService {
    private final WalkDao walkDao;
    private final UserRepository userRepository;
    private final WalkRepository walkRepository;
    private final FootprintRepository footprintRepository;
    private final PhotoRepository photoRepository;
    private final HashtagRepository hashtagRepository;
    private final TagRepository tagRepository;
    private final GoalRepository goalRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final BadgeRepository badgeRepository;
    private final EncryptProperties encryptProperties;
    private final FootprintDao footprintDao;

    @Transactional(propagation = Propagation.NESTED, rollbackFor = Exception.class)
    public List<PostWalkRes> saveRecord(String userId,PostWalkReq request) throws BaseException {
        log.debug("Validation 1. ?????? ???????????? ????????? ??????");
        if (request.getWalk().getThumbnail().isEmpty()) {
            throw new BaseException(EMPTY_WALK_PHOTO);
        }

        // userIdx ??????
        int userIdx = userRepository.findByUserId(userId).getUserIdx();


        try {
            String encryptImage = new AES128(encryptProperties.getKey()).encrypt(request.getWalk().getThumbnail());

            // ????????? ????????? ??? ??? ?????? ?????? ?????? (??????)
            String safeCoordinate = new AES128(encryptProperties.getKey()).encrypt(convert2DListToString(changeSafeCoordinate(request.getWalk().getCoordinates())));
            Double goalRate = getGoalRate(request.getWalk(), userIdx);

            // Walk Table??? ?????? ??? ????????? walkIdx return
            log.debug("3. Walk ???????????? insert ??? walkIdx ??????");
            Walk beforeSaveWalk = Walk.builder()
                    .startAt(request.getWalk().getStartAt())
                    .endAt(request.getWalk().getEndAt())
                    .distance(request.getWalk().getDistance())
                    .coordinate(safeCoordinate)
                    .pathImageUrl(encryptImage)
                    .userIdx(userIdx)
                    .goalRate(goalRate)
                    .calorie(request.getWalk().getCalorie())
                    .status("ACTIVE")
                    .build();
            walkRepository.save(beforeSaveWalk);

            if (!request.getFootprintList().isEmpty()) {
                for (SaveFootprint footprint : request.getFootprintList()) {

                    String strCoordinates = convertListToString(footprint.getCoordinates());
                    Footprint beforeSaveFootprint = Footprint.builder()
                            .coordinate(new AES128(encryptProperties.getKey()).encrypt(strCoordinates))
                            .record(new AES128(encryptProperties.getKey()).encrypt(footprint.getWrite()))
                            .recordAt(footprint.getRecordAt())
                            .onWalk(footprint.getOnWalk())
                            .updateAt(LocalDateTime.now())
                            .status("ACTIVE")
                            .build();
                    beforeSaveFootprint.setWalk(beforeSaveWalk);
                    beforeSaveWalk.addFootprint(beforeSaveFootprint);
                    List<Tag> beforeSaveTagList = new ArrayList<>();

                    for (String hashtag : footprint.getHashtagList()) {
                        Hashtag beforeSaveHashtag = Hashtag.builder()
                                .hashtag(new AES128(encryptProperties.getKey()).encrypt(hashtag))
                                .build();
                        hashtagRepository.save(beforeSaveHashtag);
                        Tag beforeSaveTag = Tag.builder()
                                .userIdx(userIdx)
                                .status("ACTIVE")
                                .build();
                        beforeSaveTag.setHashtag(beforeSaveHashtag);
                        beforeSaveTag.setFootprint(beforeSaveFootprint);

                        beforeSaveFootprint.addTagList(beforeSaveTag);
                        beforeSaveTagList.add(beforeSaveTag);
                    }

                    footprintRepository.save(beforeSaveFootprint);
                    tagRepository.saveAll(beforeSaveTagList);

                    for (String photo : footprint.getPhotos()) {
                        Photo beforeSavePhoto = Photo.builder()
                                .userIdx(userIdx)
                                .imageUrl(new AES128(encryptProperties.getKey()).encrypt(photo))
                                .status("ACTIVE")
                                .build();
                        beforeSavePhoto.setFootprint(beforeSaveFootprint);
                        photoRepository.save(beforeSavePhoto);
                    }
                }
            }

            // badge ?????? ?????? ?????? ??? id ??????
            log.debug("10. badge ?????? ?????? ?????? ??? ?????? badgeIdxList ??????");
            List<PostWalkRes> postWalkResList = new ArrayList<>();
            List<Integer> acquiredBadgeIdxList = getAcquiredBadgeIdxList(userIdx);
            Collections.sort(acquiredBadgeIdxList);

            // UserBadge ???????????? ????????? ?????? ??????
            log.debug("11. ?????? ?????? ????????? UserBadge ???????????? ??????");
            if (!acquiredBadgeIdxList.isEmpty()) { // ????????? ????????? ?????? ?????? ??????
                for (Integer acquiredBadgeIdx : acquiredBadgeIdxList) {
                    userBadgeRepository.save(
                            UserBadge.builder()
                                    .badgeIdx(acquiredBadgeIdx)
                                    .userIdx(userIdx)
                                    .status("ACTIVE")
                                    .build()
                    );
                }
            }

            // ?????? ???????????? ??????
            if (!checkFirstWalk(userIdx)) {
                User byUserId = userRepository.findByUserId(userId);
                byUserId.setBadgeIdx(1);
                userRepository.save(byUserId);
            }

            log.debug("????????? ?????? ?????? ?????????: {}", acquiredBadgeIdxList);


            //????????? ?????? ?????? (?????? ???????????? ?????? ???????????? ?????? ??????)
            log.debug("12. ?????? ???????????? ????????? url ?????? ??? request ????????? ??????");
            postWalkResList = getBadgeInfo(acquiredBadgeIdxList);

            log.debug("response??? ????????? ?????? ??????: {}", postWalkResList.stream().map(PostWalkRes::getBadgeName).collect(Collectors.toList()));

            return postWalkResList;

        } catch (Exception exception) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw new BaseException(DATABASE_ERROR);
        }
    }

    public Double getGoalRate(SaveWalk walk, int userIdx) throws BaseException {
        try {
            // ?????? ??????
            Long walkTime = Duration.between(walk.getStartAt(), walk.getEndAt()).getSeconds();
            log.debug("walkTime: {}", walkTime);
            // ?????? ?????? ??????
            List<Goal> userGoalList = goalRepository.findByUserIdx(userIdx);
            Goal userGoal = Goal.builder().build();
            for(Goal goal : userGoalList ){
                LocalDate goalCreateAt = goal.getCreateAt().toLocalDate();
                if(goalCreateAt.getMonth().equals(LocalDate.now().getMonth()) && goalCreateAt.getYear() == LocalDate.now().getYear()){
                    userGoal = goal;
                    break;
                }
            }
            Long walkGoalTime = userGoal.getWalkGoalTime() * MINUTES_TO_SECONDS;
            log.debug("walkGoalTime: {}", walkGoalTime);
            // (?????? ??? ?????? - ?????? ?????? ??????) / ?????? ?????? ??????
            Double goalRate =(walkTime.doubleValue() / walkGoalTime.doubleValue())*100.0;

            // 100??? ?????? ??? 100?????? ??????
            if (goalRate >= 100.0) {
                goalRate = 100.0;
            }

            return goalRate;
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    public boolean checkFirstWalk(int userIdx) throws BaseException {
        try {
            return walkRepository.existsByUserIdx(userIdx);
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    public List<PostWalkRes> getBadgeInfo(List<Integer> acquiredBadgeIdxList) throws BaseException {
        try {
            List<PostWalkRes> postWalkResList = new ArrayList<>();
            List<Badge> allById = badgeRepository.findAllById(acquiredBadgeIdxList);
            for (Badge badge : allById) {
                postWalkResList.add(
                        PostWalkRes.builder()
                                .badgeIdx(badge.getBadgeIdx())
                                .badgeName(badge.getBadgeName())
                                .badgeUrl(badge.getBadgeUrl())
                                .build()
                );
            }
            return postWalkResList;
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    public List<Integer> getAcquiredBadgeIdxList(int userIdx) throws BaseException {
        try {
            // ????????? ???????????? ?????? ??????
            ObtainedBadgeInterface obtainedBadgeInterface = walkRepository.getAcquiredBadgeIdxList(userIdx);
            // ?????? ????????? ?????? ?????? ??????
            Optional<List<UserBadge>> userBadgeList = userBadgeRepository.findAllByUserIdxAndStatus(userIdx, "ACTIVE");

            List<Integer> beforeSavingWalkBadgeList = new ArrayList<>();

            if (userBadgeList.isPresent()) {
                for (UserBadge userBadge : userBadgeList.get()) {
                    beforeSavingWalkBadgeList.add(userBadge.getBadgeIdx());
                }
            }

            // ?????? ??????
            List<Integer> acquiredBadgeIdxList = new ArrayList<>();

            // ?????? ?????? ?????? ??????(2~5)??? ?????? ??? ???
            int originMaxDistanceBadgeIdx = 1;
            // ?????? ?????? ?????? ??????(6~8)??? ?????? ??? ???
            int originMaxRecordBadgeIdx = 1;
            for (Integer originBadgeIdx : beforeSavingWalkBadgeList) {
                if (originBadgeIdx >= 2 && originBadgeIdx <= 5) {
                    originMaxDistanceBadgeIdx = originBadgeIdx;
                }
                if (originBadgeIdx >= 6 && originBadgeIdx <= 8) {
                    originMaxRecordBadgeIdx = originBadgeIdx;
                }
            }
            // ?????? ?????? ?????? ?????? ???????????? ??????
            if (obtainedBadgeInterface.getDistanceBadgeIdx() > originMaxDistanceBadgeIdx) {
                // ?????? ?????? ????????? ?????? ??? ????????? ??????
                for (int i = originMaxDistanceBadgeIdx + 1; i <= obtainedBadgeInterface.getDistanceBadgeIdx(); i++) {
                    acquiredBadgeIdxList.add(i);
                }
            }

            if (beforeSavingWalkBadgeList.size() == 0) {
                acquiredBadgeIdxList.add(1);
            }

            // ?????? ?????? ?????? ?????? ???????????? ??????
            if (obtainedBadgeInterface.getRecordBadgeIdx() > originMaxRecordBadgeIdx) {
                acquiredBadgeIdxList.add(obtainedBadgeInterface.getRecordBadgeIdx());
            }

            return acquiredBadgeIdxList;
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }

    }


    private ArrayList<List<Double>> changeSafeCoordinate(List<List<Double>> coordinates) {
        ArrayList<List<Double>> safeCoordinate = new ArrayList<>();
        for (List<Double> line : coordinates) {
            // ????????? ????????? ?????? ????????? ?????? ???
            log.debug("line: {}", line);
            if (line.size() == 2) {
                line.add(line.get(0));
                line.add(line.get(1));
            }
            safeCoordinate.add(line);
        }
        return safeCoordinate;
    }

    // List<List<>> -> String in WalkDao
    public String convert2DListToString(List<List<Double>> inputList){

        log.debug("String?????? ????????? ?????????: {}", inputList);

        StringBuilder str = new StringBuilder();
        str.append("(");
        int count = 0;  // 1?????? ????????? List ?????? count (????????? "," ?????? ??????)
        for(List<Double> list : inputList){
            str.append("(");
            for(int i=0;i<list.size();i++){
                str.append(list.get(i));

                if(i == list.size()-1) {    // ???????????? " " , "," ???????????? ?????? ")"
                    str.append(")");
                    break;
                }

                if (i%2 == 0)   // ?????? ?????? ???????????? " " ??????
                    str.append(" ");
                else            // ?????? ?????? ???????????? "," ??????
                    str.append(",");
            }
            count++;
            if(count != inputList.size())    // 1?????? ????????? List?????? ???????????? ???????????? "," ??????
                str.append(",");
        }
        str.append(")");
        String result = str.toString();

        return result;
    }

    public String convertListToString(List<Double> inputList) {
        log.debug("string ????????? ?????? list: {} ", inputList);

        if (inputList.isEmpty()) {
            return "(?  ?)";
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("(");
        stringBuilder.append(inputList.get(0));
        stringBuilder.append(" ");
        stringBuilder.append(inputList.get(1));
        stringBuilder.append(")");
        String result = stringBuilder.toString();

        return result;
    }

    @Transactional(propagation = Propagation.NESTED, rollbackFor = Exception.class)
    public String deleteWalk(int walkIdx, String userId) throws BaseException {
        try {
            Integer userIdx = userRepository.findByUserId(userId).getUserIdx();

            Walk walkByNumber = getWalkByNumber(walkIdx, userIdx);

            List<Footprint> allByWalk = footprintRepository.findAllByWalkAndStatus(walkByNumber, "ACTIVE");

            for (Footprint footprint : allByWalk) {
                footprint.changeStatus("INACTIVE");
                List<Photo> photoList = photoRepository.findAllByFootprintAndStatus(footprint, "ACTIVE");
                for (Photo photo : photoList) {
                    photo.changeStatus("INACTIVE");
                }
                photoRepository.saveAll(photoList);

                List<Tag> tagList = tagRepository.findAllByFootprintAndStatus(footprint, "ACTIVE");
                for (Tag tag : tagList) {
                    tag.changeStatus("INACTIVE");
                }
                tagRepository.saveAll(tagList);
            }
            footprintRepository.saveAll(allByWalk);
            walkByNumber.changeStatus("INACTIVE");
            walkRepository.save(walkByNumber);

            return "Success Delete walk record!";
        } catch (Exception exception) { // DB??? ????????? ?????? ?????? ?????? ???????????? ????????????.
            throw new BaseException(DATABASE_ERROR);
        }
    }

    // ????????? ?????? ???????????? ???????????? ???????????? ???????????? ??????
    @SneakyThrows
    public List<Double> convertStringToList(String str) {
        String test = new AES128(encryptProperties.getKey()).decrypt(str);
        if(test.startsWith("POINT")) {
            test = test.substring(5);
        }
        if (test.contains("?")) {
            return new ArrayList<>();
        }
        test = test.substring(1,test.length()-1);
        String[] sp = test.split(" ");
        List<Double> list = new ArrayList<>();
        list.add(Double.parseDouble(sp[0]));
        list.add(Double.parseDouble(sp[1]));
        return list;
    }

    @SneakyThrows
    public List<ArrayList<Double>> convertStringTo2DList(String inputString) {

        ArrayList<ArrayList<Double>> coordinate = new ArrayList<>();
        String decryptTest = new AES128(encryptProperties.getKey()).decrypt(inputString);

        if (decryptTest.contains("MULTILINESTRING")) {
            decryptTest = decryptTest.substring(17, decryptTest.length()-2); //MULTISTRING((, ) split
        }

        decryptTest = decryptTest.substring(1, decryptTest.length()-1); // ??? ??? ?????? ??????
        String[] strArr = decryptTest.split("\\),");

        for(String coor : strArr) {
            log.info("hello loop");
            coor = coor.replace("(", "");
            coor = coor.replace(")", "");

            String[] comma = coor.split(",");
            ArrayList<Double> temp = new ArrayList<>();
            for(String com : comma) {
                String[] space = com.split(" ");
                double x = Double.parseDouble(space[0]);
                if (x <= 10) {
                    x += 30;
                }
                temp.add(x);
                temp.add(Double.parseDouble(space[1]));
            }
            coordinate.add(temp);
        }
        return coordinate;
    }

    public GetWalkInfo getWalkInfo(int walkIdx, String userId) throws BaseException {
        try {
            log.debug("walkIdx: {}", walkIdx);
            Integer userIdx = userRepository.findByUserId(userId).getUserIdx();
            Walk walkByNumber = getWalkByNumber(walkIdx, userIdx);

            Duration diff = Duration.between(walkByNumber.getStartAt(), walkByNumber.getEndAt());
            String minutes = String.format("%02d", diff.toMinutesPart());
            String seconds = String.format("%02d", diff.toSecondsPart());
            String diffStr = minutes + ":" + seconds;
            if (diff.getSeconds() >= 3600) {
                String hours = String.format("%02d", diff.toHoursPart());

                diffStr = hours + ":" + diffStr;
            }

            GetWalkTime getWalkTime = GetWalkTime.builder()
                    .date(walkByNumber.getStartAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")))
                    .startAt(walkByNumber.getStartAt().format(DateTimeFormatter.ofPattern("HH:mm")))
                    .endAt(walkByNumber.getEndAt().format(DateTimeFormatter.ofPattern("HH:mm")))
                    .timeString(diffStr)
                    .build();

            List<List<Double>> footCoordinate = new ArrayList<>();

            List<Footprint> footprintList = footprintRepository.findAllByWalkAndStatus(walkByNumber, "ACTIVE");
            for (Footprint footprint : footprintList) {
                footCoordinate.add(convertStringToList(footprint.getCoordinate()));
            }

            GetWalkInfo getWalkInfo = GetWalkInfo.builder()
                    .walkIdx(walkByNumber.getWalkIdx())
                    .getWalkTime(getWalkTime)
                    .calorie(walkByNumber.getCalorie())
                    .distance(walkByNumber.getDistance())
                    .footCount(footprintList.size())
                    .footCoordinates(footCoordinate)
                    .pathImageUrl(new AES128(encryptProperties.getKey()).decrypt(walkByNumber.getPathImageUrl()))
                    .coordinate(convertStringTo2DList(walkByNumber.getCoordinate()))
                    .build();
            return getWalkInfo;
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    public Walk getWalkByNumber(int walkIdx, int userIdx) throws BaseException {
        PageRequest pageRequest = PageRequest.of(walkIdx - 1, 1);
        try {
            Page<Walk> walkPage = walkRepository.findByUserIdxAndStatusOrderByStartAtAsc(userIdx, "ACTIVE", pageRequest);
            if (walkPage.getTotalElements() == 0) {
                throw new BaseException(DELETED_WALK);
            }
            Walk walkByNumber = walkPage.get().collect(Collectors.toList()).get(0);
            return walkByNumber;
        } catch (Exception exception) {
            throw new BaseException(INVALID_WALKIDX);
        }
    }
}
