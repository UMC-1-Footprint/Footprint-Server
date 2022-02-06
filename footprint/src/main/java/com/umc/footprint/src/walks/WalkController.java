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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

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
    @PostMapping("") // (POST) 127.0.0.1:3000/walks/
    public BaseResponse<List<PostWalkRes>> saveRecord(
            @RequestPart(value = "walk") SaveWalk walk,
            @RequestPart(value = "footprintList") List<SaveFootprint> footprintList,
            @RequestPart(value = "photos") List<MultipartFile> photos
            ) throws BaseException {
        // userId(구글이나 카카오에서 보낸 ID) 추출 (복호화)
        String userId = jwtService.getUserId();
        System.out.println("userId = " + userId);
        // userId로 userIdx 추출
        int userIdx = userProvider.getUserIdx(userId);
        walk.setUserIdx(userIdx);


        System.out.println("walk.getStartAt() = " + walk.getStartAt());
        System.out.println("walk.getEndAt() = " + walk.getEndAt());
        System.out.println("walk.getDistance() = " + walk.getDistance());
        System.out.println("walk.getUserIdx() = " + walk.getUserIdx());
        System.out.println("walk.getCoordinates() = " + walk.getCoordinates());
        System.out.println("walk.getCalorie() = " + walk.getCalorie());

        System.out.println("walk.getPhotoMatchNumList() = " + walk.getPhotoMatchNumList());
//        System.out.println("footprintList = " + footprintList.get(0).getWrite());
//        System.out.println("footprintList = " + footprintList.get(0).getCoordinates());
//        System.out.println("footprintList = " + footprintList.get(0).getHashtagList());
//        System.out.println("footprintList = " + footprintList.get(0).getRecordAt());
//
//        System.out.println("photos = " + photos.get(0).getOriginalFilename());
//        System.out.println("photos.get(0).getContentType() = " + photos.get(0).getContentType());

        System.out.println("Validation 1. ");

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
            GetWalkInfo getWalkInfo = walkProvider.getWalkInfo(walkIdx);
            return new BaseResponse<>(getWalkInfo);
        } catch (BaseException exception) {
            return new BaseResponse<>((exception.getStatus()));
        }
    }

    //해당 산책의 기록(발자국) 전체 삭제
    @ResponseBody
    @PatchMapping("/{walkIdx}/status") // (Patch) 127.0.0.1:3000/walks/{walkIdx}/status
    public BaseResponse<String> deleteWalk(@PathVariable("walkIdx") int walkIdx) {
        if (walkIdx == 0) {
            return new BaseResponse<>(BaseResponseStatus.REQUEST_ERROR);
        }
        try {
            String result = walkService.deleteWalk(walkIdx);
            return new BaseResponse<>(result);
        } catch (BaseException exception) {
            return new BaseResponse<>((exception.getStatus()));
        }
    }
}
