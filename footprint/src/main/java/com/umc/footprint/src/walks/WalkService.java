package com.umc.footprint.src.walks;

import com.umc.footprint.config.BaseException;

import com.umc.footprint.config.EncryptProperties;
import com.umc.footprint.src.AwsS3Service;
import com.umc.footprint.src.model.GoalNext;
import com.umc.footprint.src.repository.GoalNextRepository;
import com.umc.footprint.src.users.UserService;
import com.umc.footprint.src.walks.model.*;

import com.umc.footprint.utils.AES128;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

import static com.umc.footprint.config.BaseResponseStatus.*;


@Slf4j
@Service
public class WalkService {
    private final WalkDao walkDao;
    private final WalkProvider walkProvider;
    private final UserService userService;
    private final AwsS3Service awsS3Service;
    private final EncryptProperties encryptProperties;
    private final GoalNextRepository goalNextRepository;

    @Autowired
    public WalkService(WalkDao walkDao, WalkProvider walkProvider, UserService userService, AwsS3Service awsS3Service, EncryptProperties encryptProperties, GoalNextRepository goalNextRepository) {
        this.walkDao = walkDao;
        this.walkProvider = walkProvider;
        this.userService = userService;
        this.awsS3Service = awsS3Service;
        this.encryptProperties = encryptProperties;
        this.goalNextRepository = goalNextRepository;
    }

    @Transactional(propagation = Propagation.NESTED, rollbackFor = Exception.class)
    public List<PostWalkRes> saveRecord(PostWalkReq request) throws BaseException {
        log.debug("Validation 1. 사진이 하나도 안왔을 때");
        if (request.getPhotos().size() == 1 && ("".equals(request.getPhotos().get(0).getOriginalFilename()))){
            log.debug("request photos: {}", request.getPhotos());
            throw new BaseException(EMPTY_WALK_PHOTO);
        }

        try {
            log.debug("1. 동선 이미지: file -> url ");
            // 경로 이미지 URL 생성 및 S3 업로드
            String pathImgUrl = awsS3Service.uploadFile(request.getPhotos().get(0));
            String encryptImage = new AES128(encryptProperties.getKey()).encrypt(pathImgUrl);
            // 동선 이미지 photos에서 pop
            List<MultipartFile> removedPathImgPhotos = request.getPhotos();
            removedPathImgPhotos.remove(0);
            log.debug("동선 이미지가 빠진 사진 리스트: {}", removedPathImgPhotos);
            request.setRemovedPathImgPhotos(removedPathImgPhotos);
            log.debug("동선 이미지 url: {}", pathImgUrl);
            log.debug("암호화된 동선 이미지 url: {}", encryptImage);

            // 라인에 좌표가 올 때 나는 오류 방지 (복제)
            List<List<Double>> safeCoordinate = changeSafeCoordinate(request.getWalk().getCoordinates());

            log.debug("2. url로 바꾼 동선 이미지 SaveWalk 객체에 저장");
            // string으로 변환한 동선 저장
            request.setWalkStrCoordinate(
                    SaveWalk.builder()
                            .startAt(request.getWalk().getStartAt())
                            .endAt(request.getWalk().getEndAt())
                            .distance(request.getWalk().getDistance())
                            .strCoordinates(new AES128(encryptProperties.getKey()).encrypt(convert2DListToString(safeCoordinate)))
                            .userIdx(request.getWalk().getUserIdx())
                            .goalRate(walkProvider.getGoalRate(request.getWalk()))
                            .calorie(request.getWalk().getCalorie())
                            .photoMatchNumList(request.getWalk().getPhotoMatchNumList())
                            .build()
            );

            // Walk Table에 삽입 후 생성된 walkIdx return
            log.debug("3. Walk 테이블에 insert 후 walkIdx 반환");
            int walkIdx = walkDao.addWalk(request.getWalk(), encryptImage);


            log.debug("생성된 walkIdx: {}", walkIdx);



            log.debug("발자국 기록이 존재할 때");
            if (!request.getFootprintList().isEmpty()) {
                log.debug("4. 발자국 기록 사진들 List<MultipartFile> -> List<String> 으로 변환");
                List<String> imgUrlList = new ArrayList<>();
                if (request.getPhotos().size() != 0) {
                    imgUrlList = awsS3Service.uploadFile(request.getPhotos());
                }

                //  발자국 Photo 이미지 URL 생성 및 S3 업로드
                log.debug("5. 발자국 좌표 List<Double> -> String 으로 변환 후 SaveFootprint 객체에 저장");
                ArrayList<SaveFootprint> convertedFootprints = new ArrayList<>();
                int imgInputStartIndex = 0;
                for (int i = 0; i < request.getWalk().getPhotoMatchNumList().size(); i++) {
                    String convertedCoordinate = convertListToString(request.getFootprintList().get(i).getCoordinates());
                    log.debug("String으로 변환된 발자국 좌표", convertedCoordinate);
                    int imgInputEndIndex = imgInputStartIndex + request.getWalk().getPhotoMatchNumList().get(i);
                    if (imgInputEndIndex > request.getPhotos().size()) {
                        throw new BaseException(EXCEED_FOOTPRINT_SIZE);
                    }
                    List<String> imgInputList = new ArrayList<String>(imgUrlList.subList(imgInputStartIndex, imgInputEndIndex));
                    List<String> encryptedImageList = new ArrayList<>();
                    for (String img : imgInputList) {
                        encryptedImageList.add(new AES128(encryptProperties.getKey()).encrypt(img));
                    }
                    List<String> encryptedHashtagList = new ArrayList<>();
                    for (String hashtag : request.getFootprintList().get(i).getHashtagList()) {
                        encryptedHashtagList.add(new AES128(encryptProperties.getKey()).encrypt(hashtag));
                    }

                    SaveFootprint convertedFootprint = SaveFootprint.builder()
                            .strCoordinate(new AES128(encryptProperties.getKey()).encrypt(convertedCoordinate))
                            .write(new AES128(encryptProperties.getKey()).encrypt(request.getFootprintList().get(i).getWrite()))
                            .recordAt(request.getFootprintList().get(i).getRecordAt())
                            .walkIdx(request.getFootprintList().get(i).getWalkIdx())
                            .hashtagList(encryptedHashtagList)
                            .imgUrlList(encryptedImageList)
                            .onWalk(request.getFootprintList().get(i).getOnWalk())
                            .build();

                    convertedFootprints.add(convertedFootprint);
                    imgInputStartIndex = imgInputEndIndex;
                }
                request.setConvertedFootprints(convertedFootprints);

                // Footprint Table에 삽입 후 생성된 footprintIdx Footprint에 초기화
                log.debug("6. Footprint 테이블에 삽입후 footprintIdx SaveFootprint 테이블에 삽입");
                walkDao.addFootprint(request.getFootprintList(), walkIdx);

                // Photo Table에 삽입
                log.debug("7. Photo 테이블에 삽입");
                walkDao.addPhoto(request.getWalk().getUserIdx(), request.getFootprintList());

                // Hashtag Table에 삽입 후 tagIdx(hashtagIdx, footprintIdx) mapping pair 반환
                log.debug("8. Hashtag 테이블에 삽입 후 매핑된 Idx 반환 ");
                List<Pair<Integer, Integer>> tagIdxList = walkDao.addHashtag(request.getFootprintList());

                if (tagIdxList.size() != 0){// Tag Table에 삽입
                    log.debug("9. Tag 테이블에 삽입");
                    walkDao.addTag(tagIdxList, request.getWalk().getUserIdx());
                }
            }

            // badge 획득 여부 확인 및 id 반환
            log.debug("10. badge 획득 여부 확인 후 얻은 badgeIdxList 반환");
            List<PostWalkRes> postWalkResList = new ArrayList<>();
            List<Integer> acquiredBadgeIdxList = walkProvider.getAcquiredBadgeIdxList(request.getWalk().getUserIdx());
            Collections.sort(acquiredBadgeIdxList);

            // UserBadge 테이블에 획득한 뱃지 삽입
            log.debug("11. 얻은 뱃지 리스트 UserBadge 테이블에 삽입");
            if (!acquiredBadgeIdxList.isEmpty()) { // 획득한 뱃지가 있을 경우 삽입
                walkDao.addUserBadge(acquiredBadgeIdxList, request.getWalk().getUserIdx());
            }

            // 처음 산책인지 확인
            if (walkProvider.checkFirstWalk(request.getWalk().getUserIdx()) == 0) {
                userService.modifyRepBadge(request.getWalk().getUserIdx(), 1); //대표 뱃지로 설정
            }

            log.debug("새롭게 얻은 뱃지 리스트: {}", acquiredBadgeIdxList);


            //획득한 뱃지 넣기 (뱃지 아이디로 뱃지 이름이랑 그림 반환)
            log.debug("12. 뱃지 리스트로 이름과 url 반환 후 request 객체에 저장");
            postWalkResList = walkProvider.getBadgeInfo(acquiredBadgeIdxList);

            log.debug("response로 반환할 뱃지 이름: {}", postWalkResList.stream().map(PostWalkRes::getBadgeName).collect(Collectors.toList()));

            return postWalkResList;

        } catch (Exception exception) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
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
        str.append("MULTILINESTRING(");
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

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("POINT(");
        stringBuilder.append(inputList.get(0));
        stringBuilder.append(" ");
        stringBuilder.append(inputList.get(1));
        stringBuilder.append(")");
        String result = stringBuilder.toString();

        return result;
    }

    @Transactional(propagation = Propagation.NESTED, rollbackFor = Exception.class)
    public String deleteWalk(int walkIdx) throws BaseException {
        try {
            //walkIdx 로 footprintIdx 모두 얻어오기
            List<Integer> footprintIdxList = walkDao.getFootprintIdxList(walkIdx);
            for(int footprintIdx : footprintIdxList) {
                //footprintIdx에 해당하는 photo, tag 모두 INACTIVE
                walkDao.inactivePhoto(footprintIdx);
                walkDao.inactiveTag(footprintIdx);
            }

            String result = walkDao.deleteWalk(walkIdx);

            return result;
        } catch (Exception exception) { // DB에 이상이 있는 경우 에러 메시지를 보냅니다.
            throw new BaseException(DATABASE_ERROR);
        }
    }

    public String checkEncryptWalk(String encryptString) throws BaseException{
        try{
            String encryptResult = new AES128(encryptProperties.getKey()).encrypt(encryptString);

            log.info("encryptResult = {}",encryptResult );

            return encryptResult;
        } catch(Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    public String checkDecryptWalk(String decryptString) throws BaseException{
        try{
            System.out.println("decryptString = " + decryptString);
            String decryptResult = new AES128(encryptProperties.getKey()).decrypt(decryptString);

            System.out.println("decryptResult = " + decryptResult);
            
            return decryptResult;
        } catch(Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    public void testJpa() {
        Optional<GoalNext> example = goalNextRepository.findById(3);
        log.debug("example: {}" ,example.get().toString());

    }

    public GetWalkInfo getWalkInfo(int walkIdx) throws BaseException {
        try {
            int check = walkDao.checkWalkVal(walkIdx);
            if(check!=1) { //산책 INACTIVE
                throw new BaseException(INVALID_WALKIDX);
            }
            GetWalkInfo getWalkInfo = walkDao.getWalkInfo(walkIdx);
            getWalkInfo.changePathImageUrl(getWalkInfo,new AES128(encryptProperties.getKey()).decrypt(getWalkInfo.getPathImageUrl()));
            return getWalkInfo;
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }
}
