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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
        log.debug("Validation 1. 동선 이미지가 안왔을 경우");
        if (request.getWalk().getThumbnail().isEmpty()) {
            throw new BaseException(EMPTY_WALK_PHOTO);
        }

        // userIdx 추출
        int userIdx = userRepository.findByUserId(userId).getUserIdx();


        try {
            String encryptImage = new AES128(encryptProperties.getKey()).encrypt(request.getWalk().getThumbnail());

            // 라인에 좌표가 올 때 나는 오류 방지 (복제)
            String safeCoordinate = new AES128(encryptProperties.getKey()).encrypt(convert2DListToString(changeSafeCoordinate(request.getWalk().getCoordinates())));
            Double goalRate = getGoalRate(request.getWalk(), userIdx);

            // Walk Table에 삽입 후 생성된 walkIdx return
            log.debug("3. Walk 테이블에 insert 후 walkIdx 반환");
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

            // badge 획득 여부 확인 및 id 반환
            log.debug("10. badge 획득 여부 확인 후 얻은 badgeIdxList 반환");
            List<PostWalkRes> postWalkResList = new ArrayList<>();
            List<Integer> acquiredBadgeIdxList = getAcquiredBadgeIdxList(userIdx);
            Collections.sort(acquiredBadgeIdxList);

            // UserBadge 테이블에 획득한 뱃지 삽입
            log.debug("11. 얻은 뱃지 리스트 UserBadge 테이블에 삽입");
            if (!acquiredBadgeIdxList.isEmpty()) { // 획득한 뱃지가 있을 경우 삽입
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

            // 처음 산책인지 확인
            if (checkFirstWalk(userIdx)) {
                User byUserId = userRepository.findByUserId(userId);
                byUserId.setBadgeIdx(1);
                userRepository.save(byUserId);
            }

            log.debug("새롭게 얻은 뱃지 리스트: {}", acquiredBadgeIdxList);


            //획득한 뱃지 넣기 (뱃지 아이디로 뱃지 이름이랑 그림 반환)
            log.debug("12. 뱃지 리스트로 이름과 url 반환 후 request 객체에 저장");
            postWalkResList = getBadgeInfo(acquiredBadgeIdxList);

            log.debug("response로 반환할 뱃지 이름: {}", postWalkResList.stream().map(PostWalkRes::getBadgeName).collect(Collectors.toList()));

            return postWalkResList;

        } catch (Exception exception) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw new BaseException(DATABASE_ERROR);
        }
    }

    public Double getGoalRate(SaveWalk walk, int userIdx) throws BaseException {
        try {
            // 산책 시간
            Long walkTime = Duration.between(walk.getStartAt(), walk.getEndAt()).getSeconds();
            log.debug("walkTime: {}", walkTime);
            // 산책 목표 시간
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
            // (산책 끝 시간 - 산책 시작 시간) / 산책 목표 시간
            Double goalRate =(walkTime.doubleValue() / walkGoalTime.doubleValue())*100.0;

            // 100퍼 넘을 시 100으로 고정
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
            // 조건에 부합하는 뱃지 조회
            ObtainedBadgeInterface obtainedBadgeInterface = walkRepository.getAcquiredBadgeIdxList(userIdx);
            // 원래 가지고 있던 뱃지 조회
            Optional<List<UserBadge>> userBadgeList = userBadgeRepository.findAllByUserIdxAndStatus(userIdx, "ACTIVE");

            List<Integer> beforeSavingWalkBadgeList = new ArrayList<>();

            if (userBadgeList.isPresent()) {
                for (UserBadge userBadge : userBadgeList.get()) {
                    beforeSavingWalkBadgeList.add(userBadge.getBadgeIdx());
                }
            }

            // 얻은 뱃지
            List<Integer> acquiredBadgeIdxList = new ArrayList<>();

            // 원래 갖고 있던 뱃지(2~5)의 가장 큰 값
            int originMaxDistanceBadgeIdx = 1;
            // 원래 갖고 있던 뱃지(6~8)의 가장 큰 값
            int originMaxRecordBadgeIdx = 1;
            for (Integer originBadgeIdx : beforeSavingWalkBadgeList) {
                if (originBadgeIdx >= 2 && originBadgeIdx <= 5) {
                    originMaxDistanceBadgeIdx = originBadgeIdx;
                }
                if (originBadgeIdx >= 6 && originBadgeIdx <= 8) {
                    originMaxRecordBadgeIdx = originBadgeIdx;
                }
            }
            // 거리 관련 얻은 뱃지 리스트에 저장
            if (obtainedBadgeInterface.getDistanceBadgeIdx() > originMaxDistanceBadgeIdx) {
                // 누적 거리 뱃지를 여러 개 달성할 경우
                for (int i = originMaxDistanceBadgeIdx + 1; i <= obtainedBadgeInterface.getDistanceBadgeIdx(); i++) {
                    acquiredBadgeIdxList.add(i);
                }
            }

            if (beforeSavingWalkBadgeList.size() == 0) {
                acquiredBadgeIdxList.add(1);
            }

            // 기록 관련 얻은 뱃지 리스트에 저장
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
            // 좌표가 하나만 있는 라인이 있을 때
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

        log.debug("String으로 변환할 리스트: {}", inputList);

        StringBuilder str = new StringBuilder();
        str.append("(");
        int count = 0;  // 1차원 범위의 List 경과 count (마지막 "," 빼기 위해)
        for(List<Double> list : inputList){
            str.append("(");
            for(int i=0;i<list.size();i++){
                str.append(list.get(i));

                if(i == list.size()-1) {    // 마지막은 " " , "," 추가하지 않고 ")"
                    str.append(")");
                    break;
                }

                if (i%2 == 0)   // 짝수 번째 인덱스는 " " 추가
                    str.append(" ");
                else            // 홀수 번째 인덱스는 "," 추가
                    str.append(",");
            }
            count++;
            if(count != inputList.size())    // 1차원 범위의 List에서 마지막을 제외하고 "," 추가
                str.append(",");
        }
        str.append(")");
        String result = str.toString();

        return result;
    }

    public String convertListToString(List<Double> inputList) {
        log.debug("string 형으로 바꿀 list: {} ", inputList);

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
        } catch (Exception exception) { // DB에 이상이 있는 경우 에러 메시지를 보냅니다.
            throw new BaseException(DATABASE_ERROR);
        }
    }

    // 발자국 좌표 암호화된 문자열을 리스트로 변환하는 함수
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

        decryptTest = decryptTest.substring(1, decryptTest.length()-1); // 앞 뒤 괄호 제거
        String[] strArr = decryptTest.split("\\),");

        for(String coor : strArr) {
            log.info("hello loop");
            coor = coor.replace("(", "");
            coor = coor.replace(")", "");

            String[] comma = coor.split(",");
            ArrayList<Double> temp = new ArrayList<>();
            for(String com : comma) {
                String[] space = com.split(" ");
                temp.add(Double.parseDouble(space[0]));
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
            String diffStr = "";
            if (diff.getSeconds() >= 3600) {
                String hours = String.format("%02d", diff.toHoursPart());
                String minutes = String.format("%02d", diff.toMinutesPart());

                diffStr = hours + ":" + minutes;
            } else {
                String minutes = String.format("%02d", diff.toMinutesPart());
                String seconds = String.format("%02d", diff.toSecondsPart());

                diffStr = minutes + ":" + seconds;
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
