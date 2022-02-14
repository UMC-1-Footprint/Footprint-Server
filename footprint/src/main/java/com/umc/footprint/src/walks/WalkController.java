package com.umc.footprint.src.walks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.umc.footprint.config.BaseException;
import com.umc.footprint.config.BaseResponse;
import com.umc.footprint.config.BaseResponseStatus;
import com.umc.footprint.src.users.UserProvider;
import com.umc.footprint.src.walks.model.*;

import com.umc.footprint.utils.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import static com.umc.footprint.config.BaseResponseStatus.EMPTY_WALK_PHOTO;

@Slf4j
@RestController
@RequestMapping("/walks")
public class WalkController {

    private final UserProvider userProvider;
    private final WalkProvider walkProvider;
    private final WalkService walkService;
    private final JwtService jwtService;

    @Autowired
    public WalkController(UserProvider userProvider, WalkService walkService, WalkProvider walkProvider, JwtService jwtService) {
        this.userProvider = userProvider;
        this.walkService = walkService;
        this.walkProvider = walkProvider;
        this.jwtService = jwtService;
    }

    /**
     *  실시간 처리 API
     *  [Post] /walks
     */
    @ResponseBody
    @PostMapping("") // (POST) 127.0.0.1:3000/walks/
    public BaseResponse<List<PostWalkRes>> saveRecord(
            @RequestPart(value = "walk") SaveWalk walk,
            @RequestPart(value = "footprintList") List<SaveFootprint> footprintList,
            @RequestPart(value = "photos") List<MultipartFile> photos
            ) throws BaseException {
        // userId(구글이나 카카오에서 보낸 ID) 추출 (복호화)
        String userId = jwtService.getUserId();
        log.info("userId = " + userId);
        // userId로 userIdx 추출
        int userIdx = userProvider.getUserIdx(userId);
        walk.setUserIdx(userIdx);


        log.info("walk startAt: {}", walk.getStartAt());
        log.info("walk endAt: {}", walk.getEndAt());
        log.info("walk distance: {}", walk.getDistance());
        log.info("walk userIdx: {}", walk.getUserIdx());
        log.info("walk coordinate: {}", walk.getCoordinates());
        log.info("walk calorie: {}", walk.getCalorie());
        log.info("walk photoMatchNumList: {}", walk.getPhotoMatchNumList());



        try {

            if (walk.getPhotoMatchNumList().size() != footprintList.size()) {
                return new BaseResponse<>(BaseResponseStatus.NOT_MATCH_IMAGE_COUNT);
            }
            List<PostWalkRes> postWalkResList = walkService.saveRecord(
                    PostWalkReq.builder()
                            .walk(walk)
                            .footprintList(footprintList)
                            .photos(photos)
                            .build()
            );
            return new BaseResponse<>(postWalkResList);

        } catch (BaseException exception) {
            return new BaseResponse<>(exception.getStatus());
        }
    }

    //yummy 20
    @ResponseBody
    @GetMapping("/{walkIdx}") // (GET) 127.0.0.1:3000/walks/{walkIdx}
    public BaseResponse<GetWalkInfo> getWalkInfo(@PathVariable("walkIdx") int walkIdx) {
        try {
            // userId(구글이나 카카오에서 보낸 ID) 추출 (복호화)
            String userId = jwtService.getUserId();
            log.info("userId: {}", userId);
            // userId로 userIdx 추출
            int userIdx = userProvider.getUserIdx(userId);

            // Walk 테이블 전체에서 인덱스
            int wholeWalkIdx = walkProvider.getWalkWholeIdx(walkIdx, userIdx);
            log.info("wholeWalkIdx: {}", wholeWalkIdx);

            GetWalkInfo getWalkInfo = walkProvider.getWalkInfo(wholeWalkIdx);
            return new BaseResponse<>(getWalkInfo);
        } catch (BaseException exception) {
            return new BaseResponse<>((exception.getStatus()));
        }
    }

    //yummy 21
    //해당 산책의 기록(발자국) 전체 삭제
    @ResponseBody
    @PatchMapping("/{walkIdx}/status") // (Patch) 127.0.0.1:3000/walks/{walkIdx}/status
    public BaseResponse<String> deleteWalk(@PathVariable("walkIdx") int walkIdx) {
        try {
            // userId(구글이나 카카오에서 보낸 ID) 추출 (복호화)
            String userId = jwtService.getUserId();
            log.info("userId: {}", userId);
            // userId로 userIdx 추출
            int userIdx = userProvider.getUserIdx(userId);

            // Walk 테이블 전체에서 인덱스
            int wholeWalkIdx = walkProvider.getWalkWholeIdx(walkIdx, userIdx);
            log.info("wholeWalkIdx: {}", wholeWalkIdx);

            if (wholeWalkIdx == 0) {
                return new BaseResponse<>(BaseResponseStatus.REQUEST_ERROR);
            }
            String result = walkService.deleteWalk(wholeWalkIdx);
            return new BaseResponse<>(result);
        } catch (BaseException exception) {
            return new BaseResponse<>((exception.getStatus()));
        }
    }
}
