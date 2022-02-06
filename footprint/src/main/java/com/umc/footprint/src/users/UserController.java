package com.umc.footprint.src.users;

import com.umc.footprint.src.users.model.GetUserTodayRes;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.umc.footprint.src.users.model.*;
import com.umc.footprint.utils.JwtService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

import com.umc.footprint.config.BaseException;
import com.umc.footprint.config.BaseResponse;

import com.umc.footprint.config.BaseResponseStatus;

import static com.umc.footprint.config.BaseResponseStatus.*;
import static com.umc.footprint.utils.ValidationRegax.isRegexEmail;


@RestController
@RequestMapping("/users")
public class UserController {

    private final UserProvider userProvider;
    private final UserService userService;
    private final JwtService jwtService;

    @Autowired
    public UserController(UserProvider userProvider, UserService userService, JwtService jwtService) {
        this.userProvider = userProvider;
        this.userService = userService;
        this.jwtService = jwtService;
    }
    /**
     * 유저 로그인 API
     * [POST] /users/auth/login
     */
    @ResponseBody
    @PostMapping("/auth/login")
    public BaseResponse<PostLoginRes> postUser(@RequestBody PostLoginReq postLoginReq) {
        if (postLoginReq.getEmail() == null) {
            return new BaseResponse<>(POST_USERS_EMPTY_EMAIL);
        }
        //이메일 정규표현: 입력받은 이메일이 email@domain.xxx와 같은 형식인지 검사합니다. 형식이 올바르지 않다면 에러 메시지를 보냅니다.
        if (!isRegexEmail(postLoginReq.getEmail())) {
            return new BaseResponse<>(POST_USERS_INVALID_EMAIL);
        }
        try {
            PostLoginRes postLoginRes = userService.postUserLogin(postLoginReq);
            return new BaseResponse<>(postLoginRes);
        } catch (BaseException exception) {
            return new BaseResponse<>(exception.getStatus());
        }
    }

    /**
     * 유저 오늘 산책관련 정보 조회 API
     * [GET] /users/today
     */
    // Path-variable
    @ResponseBody
    @GetMapping("/today")
    public BaseResponse<GetUserTodayRes> getToday(){
        try{
            // userId(구글이나 카카오에서 보낸 ID) 추출 (복호화)
            String userId = jwtService.getUserId();
            System.out.println("userId = " + userId);
            // userId로 userIdx 추출
            int userIdx = userProvider.getUserIdx(userId);
            
            GetUserTodayRes userTodayRes = userProvider.getUserToday(userIdx);

            return new BaseResponse<>(userTodayRes);
        } catch (BaseException exception){
            return new BaseResponse<>(exception.getStatus());
        }
    }

    /**
     * 유저 날짜별 산책관련 정보 조회 API
     * [GET] /users/:date
     */
    // Path-variable
    @ResponseBody
    @GetMapping("/{date}")
    public BaseResponse<List<GetUserDateRes>> getDateWalk(@PathVariable("date") String date){

        // Validation 1. 날짜 형식 검사
        if(!date.matches("^\\d{4}\\-(0[1-9]|1[012])\\-(0[1-9]|[12][0-9]|3[01])$")){
            return new BaseResponse<>(new BaseException(BaseResponseStatus.INVALID_DATE).getStatus());
        }

        // Provider 연결
        try{
            // userId(구글이나 카카오에서 보낸 ID) 추출 (복호화)
            String userId = jwtService.getUserId();
            System.out.println("userId = " + userId);
            // userId로 userIdx 추출
            int userIdx = userProvider.getUserIdx(userId);

            List<GetUserDateRes> userDateRes = userProvider.getUserDate(userIdx,date);

            return new BaseResponse<>(userDateRes);
        } catch (BaseException exception){
            return new BaseResponse<>(exception.getStatus());
        }
    }

    /**
     * 유저 정보 조회 API
     * [GET] /users
     */
    // Path-variable
    @ResponseBody
    @GetMapping("") // (GET) 127.0.0.1:3000
    public BaseResponse<GetUserRes> getUser() {
        try {
            // userId(구글이나 카카오에서 보낸 ID) 추출 (복호화)
            String userId = jwtService.getUserId();
            System.out.println("userId = " + userId);
            // userId로 userIdx 추출
            int userIdx = userProvider.getUserIdx(userId);

            GetUserRes getUserRes = userProvider.getUser(userIdx);
            return new BaseResponse<>(getUserRes);
        } catch (BaseException exception) {
            exception.printStackTrace();
            return new BaseResponse<>((exception.getStatus()));
        }

    }

    /**
     * 유저 닉네임 변경 API
     * [PATCH] /users/nickname
     */
    @ResponseBody
    @PatchMapping("/nickname")
    public BaseResponse<String> modifyNickname(@RequestBody User user) {
        try {
            // userId(구글이나 카카오에서 보낸 ID) 추출 (복호화)
            String userId = jwtService.getUserId();
            System.out.println("userId = " + userId);
            // userId로 userIdx 추출
            int userIdx = userProvider.getUserIdx(userId);

            PatchNicknameReq patchNicknameReq = new PatchNicknameReq(userIdx, user.getNickname());
            if (user.getNickname().length() > 8) { // 닉네임 8자 초과
                throw new BaseException(BaseResponseStatus.MAX_NICKNAME_LENGTH);
            }
            userService.modifyNickname(patchNicknameReq);

            String result = "닉네임이 수정되었습니다.";
            
            return new BaseResponse<>(result);
        } catch (BaseException exception) {
            return new BaseResponse<>((exception.getStatus()));
        }
    }

    /*
     * 유저 "이번달" 목표 조회 API
     * [GET] /users/goals
     */
    // Path-variable
    @ResponseBody
    @GetMapping("/goals") // (GET) 127.0.0.1:3000/users/goals
    public BaseResponse<GetUserGoalRes> getUserGoal() {
        try {
            // userId(구글이나 카카오에서 보낸 ID) 추출 (복호화)
            String userId = jwtService.getUserId();
            System.out.println("userId = " + userId);
            // userId로 userIdx 추출
            int userIdx = userProvider.getUserIdx(userId);

            GetUserGoalRes getUserGoalRes = userProvider.getUserGoal(userIdx);
            return new BaseResponse<>(getUserGoalRes);
        } catch (BaseException exception) {
            return new BaseResponse<>((exception.getStatus()));
        }

    }

    /**
     * 유저 "다음달" 목표 조회 API
     * [GET] /users/goals/next
     */
    // Path-variable
    @ResponseBody
    @GetMapping("/goals/next") // (GET) 127.0.0.1:3000/users/goals/next
    public BaseResponse<GetUserGoalRes> getUserGoalNext() {
        try {
            // userId(구글이나 카카오에서 보낸 ID) 추출 (복호화)
            String userId = jwtService.getUserId();
            System.out.println("userId = " + userId);
            // userId로 userIdx 추출
            int userIdx = userProvider.getUserIdx(userId);

            GetUserGoalRes getUserGoalRes = userProvider.getUserGoalNext(userIdx);
            return new BaseResponse<>(getUserGoalRes);
        } catch (BaseException exception) {
            return new BaseResponse<>((exception.getStatus()));
        }

    }


    /** yummy 4
     * 이번달 정보 조회 API
     * [GET] /users/tmonth
     */
    @ResponseBody
    @GetMapping("/tmonth")
    public BaseResponse<GetMonthInfoRes> getMonthInfo() {
        // TO-DO-LIST
        // jwt 확인?
        // user테이블에 해당 userIdx가 존재하는지
        // GoalDay 테이블에 해당 userIdx가 존재하는지

        try {
            // userId(구글이나 카카오에서 보낸 ID) 추출 (복호화)
            String userId = jwtService.getUserId();
            System.out.println("userId = " + userId);
            // userId로 userIdx 추출
            int userIdx = userProvider.getUserIdx(userId);

            LocalDate now = LocalDate.now();
            int nowYear = now.getYear();
            int nowMonth = now.getMonthValue();

            GetMonthInfoRes getMonthInfoRes = userProvider.getMonthInfoRes(userIdx, nowYear, nowMonth);
            return new BaseResponse<>(getMonthInfoRes);
        } catch (BaseException exception) {
            return new BaseResponse<>((exception.getStatus()));
        }

    }
  

    /**
     * 목표 수정 API
     * [PATCH] /users/goals
     */
    // Path-variable
    @ResponseBody
    @PatchMapping("/goals") // [PATCH] /users/goals
    public BaseResponse<String> modifyGoal(@RequestBody PatchUserGoalReq patchUserGoalReq){

        // Validaion 1. dayIdx 길이 확인
        if(patchUserGoalReq.getDayIdx().size() == 0) // 요일 0개 선택
            return new BaseResponse<>(new BaseException(BaseResponseStatus.MIN_DAYIDX).getStatus());
        if(patchUserGoalReq.getDayIdx().size() > 7)  // 요일 7개 초과 선택
            return new BaseResponse<>(new BaseException(BaseResponseStatus.MAX_DAYIDX).getStatus());

        // Validaion 2. dayIdx 숫자 범위 확인
        for (Integer dayIdx : patchUserGoalReq.getDayIdx()){
            if (dayIdx > 7 || dayIdx < 1)
                return new BaseResponse<>(new BaseException(BaseResponseStatus.INVALID_DAYIDX).getStatus());
        }

        // Validaion 3. dayIdx 중복된 숫자 확인
        Set<Integer> setDayIDx = new HashSet<>(patchUserGoalReq.getDayIdx());
        if(patchUserGoalReq.getDayIdx().size() != setDayIDx.size()) // dayIdx 크기를 set으로 변형시킨 dayIdx 크기와 비교. 크기가 다르면 중복된 값 존재
            return new BaseResponse<>(new BaseException(BaseResponseStatus.OVERLAP_DAYIDX).getStatus());

        // Validaion 4. walkGoalTime 범위 확인
        if(patchUserGoalReq.getWalkGoalTime() < 10) // 최소 산책 목표 시간 미만
            return new BaseResponse<>(new BaseException(BaseResponseStatus.MIN_WALK_GOAL_TIME).getStatus());
        if(patchUserGoalReq.getWalkGoalTime() > 240) // 최대 산책 목표 시간 초과
            return new BaseResponse<>(new BaseException(BaseResponseStatus.MAX_WALK_GOAL_TIME).getStatus());

        // Validaion 5. walkTimeSlot 범위 확인
        if(patchUserGoalReq.getWalkTimeSlot() > 7 || patchUserGoalReq.getWalkTimeSlot() < 1)
            return new BaseResponse<>(new BaseException(BaseResponseStatus.INVALID_WALK_TIME_SLOT).getStatus());
       
       try {
            // userId(구글이나 카카오에서 보낸 ID) 추출 (복호화)
            String userId = jwtService.getUserId();
            System.out.println("userId = " + userId);
            // userId로 userIdx 추출
            int userIdx = userProvider.getUserIdx(userId);

            userService.modifyGoal(userIdx, patchUserGoalReq);

            String result ="목표가 수정되었습니다.";

            return new BaseResponse<>(result);
        } catch (BaseException exception) {
            return new BaseResponse<>((exception.getStatus()));
        }
    }


    /** yummy 5
     * 월별 발자국 개수 조회 API
     * [GET] /users/months/footprints?year=2021&month=2
     */
    @ResponseBody
    @GetMapping("/months/footprints")
    public BaseResponse<List<GetFootprintCount>> getMonthFootprints(@RequestParam(required = true) int year, @RequestParam(required = true) int month) throws BaseException {
        try {
            // userId(구글이나 카카오에서 보낸 ID) 추출 (복호화)
            String userId = jwtService.getUserId();
            System.out.println("userId = " + userId);
            // userId로 userIdx 추출
            int userIdx = userProvider.getUserIdx(userId);

            List<GetFootprintCount> getFootprintCounts = userProvider.getMonthFootprints(userIdx, year, month);
            return new BaseResponse<>(getFootprintCounts);
        }
        catch (BaseException exception) {
            return new BaseResponse<>((exception.getStatus()));
        }
    }

    /** yummy 13
     * 사용자 전체 뱃지 조회 API
     * [GET] /users/badges
     */
    @ResponseBody
    @GetMapping("/badges/status") //매달 첫 접속마다 요청되는 뱃지 확인 API - 이번달 획득 뱃지의 정보를 전달, 없으면 null 반환
    public BaseResponse<BadgeInfo> getMonthlyBadgeStatus() throws BaseException {
        try {
            // userId(구글이나 카카오에서 보낸 ID) 추출 (복호화)
            String userId = jwtService.getUserId();
            System.out.println("userId = " + userId);
            // userId로 userIdx 추출
            int userIdx = userProvider.getUserIdx(userId);

            BadgeInfo getBadgeInfo = userProvider.getMonthlyBadgeStatus(userIdx);
            return new BaseResponse<>(getBadgeInfo);
        }
        catch (BaseException exception) {
            return new BaseResponse<>((exception.getStatus()));
        }
    }




    /** yummy 11
     * 사용자 전체 뱃지 조회 API
     * [GET] /users/badges
     */
    @ResponseBody
    @GetMapping("/badges")
    public BaseResponse<GetUserBadges> getUsersBadges() throws BaseException {
        try {
            // userId(구글이나 카카오에서 보낸 ID) 추출 (복호화)
            String userId = jwtService.getUserId();
            System.out.println("userId = " + userId);
            // userId로 userIdx 추출
            int userIdx = userProvider.getUserIdx(userId);

            GetUserBadges getUserBadges = userProvider.getUserBadges(userIdx);
            return new BaseResponse<>(getUserBadges);
        }
        catch (BaseException exception) {
            return new BaseResponse<>((exception.getStatus()));
        }
    }

    /** yummy 12
     * 사용자 대표 뱃지 수정 API
     * [GET] /users/badges/title/:badgeidx
     */
    @ResponseBody
    @PatchMapping("/badges/title/{badgeIdx}")
    public BaseResponse<BadgeInfo> modifyRepBadge(@PathVariable("badgeIdx") int badgeIdx) throws BaseException {
        try {
            // userId(구글이나 카카오에서 보낸 ID) 추출 (복호화)
            String userId = jwtService.getUserId();
            System.out.println("userId = " + userId);
            // userId로 userIdx 추출
            int userIdx = userProvider.getUserIdx(userId);

            BadgeInfo patchRepBadgeInfo = userService.modifyRepBadge(userIdx, badgeIdx);
            return new BaseResponse<>(patchRepBadgeInfo);
        }
        catch (BaseException exception) {
            return new BaseResponse<>((exception.getStatus()));
        }
    }


    /**
     * 유저 세부 정보 조회 API
     * [GET] /users/infos
     */
    // Path-variable
    @ResponseBody
    @GetMapping("/infos") // (GET) 127.0.0.1:3000/users/:userIdx/infos
    public BaseResponse<GetUserInfoRes> getUserInfo() {
        try {
            // userId(구글이나 카카오에서 보낸 ID) 추출 (복호화)
            String userId = jwtService.getUserId();
            System.out.println("userId = " + userId);
            // userId로 userIdx 추출
            int userIdx = userProvider.getUserIdx(userId);

            GetUserInfoRes getUserInfoRes = userProvider.getUserInfo(userIdx);
            return new BaseResponse<>(getUserInfoRes);
        } catch (BaseException exception) {
            return new BaseResponse<>((exception.getStatus()));
        }

    }

   /**
     * 초기 정보 등록 API
     * [POST] /users/infos
     */
    // Path-variable
    @ResponseBody
    @PostMapping("/infos") // [POST] /users/infos
    public BaseResponse<String> postUserInfo(@RequestBody PatchUserInfoReq patchUserInfoReq){

        try {
            // userId(구글이나 카카오에서 보낸 ID) 추출 (복호화)
            String userId = jwtService.getUserId();
            System.out.println("userId = " + userId);
            // userId로 userIdx 추출
            int userIdx = userProvider.getUserIdx(userId);

            // Validaion 1. userIdx 가 0 이하일 경우 exception
            if(userIdx <= 0)
                return new BaseResponse<>(new BaseException(BaseResponseStatus.INVALID_USERIDX).getStatus());

            // Validaion 2. dayIdx 길이 확인
            if(patchUserInfoReq.getDayIdx().size() == 0) // 요일 0개 선택
                return new BaseResponse<>(new BaseException(BaseResponseStatus.MIN_DAYIDX).getStatus());
            if(patchUserInfoReq.getDayIdx().size() > 7)  // 요일 7개 초과 선택
                return new BaseResponse<>(new BaseException(BaseResponseStatus.MAX_DAYIDX).getStatus());

            // Validaion 3. dayIdx 숫자 범위 확인
            for (Integer dayIdx : patchUserInfoReq.getDayIdx()){
                if (dayIdx > 7 || dayIdx < 1)
                    return new BaseResponse<>(new BaseException(BaseResponseStatus.INVALID_DAYIDX).getStatus());
            }

            // Validaion 4. dayIdx 중복된 숫자 확인
            Set<Integer> setDayIDx = new HashSet<>(patchUserInfoReq.getDayIdx());
            if(patchUserInfoReq.getDayIdx().size() != setDayIDx.size()) // dayIdx 크기를 set으로 변형시킨 dayIdx 크기와 비교. 크기가 다르면 중복된 값 존재
                return new BaseResponse<>(new BaseException(BaseResponseStatus.OVERLAP_DAYIDX).getStatus());

            // Validaion 5. walkGoalTime 범위 확인
            if(patchUserInfoReq.getWalkGoalTime() < 10) // 최소 산책 목표 시간 미만
                return new BaseResponse<>(new BaseException(BaseResponseStatus.MIN_WALK_GOAL_TIME).getStatus());
            if(patchUserInfoReq.getWalkGoalTime() > 240) // 최대 산책 목표 시간 초과
                return new BaseResponse<>(new BaseException(BaseResponseStatus.MAX_WALK_GOAL_TIME).getStatus());

            // Validaion 6. walkTimeSlot 범위 확인
            if(patchUserInfoReq.getWalkTimeSlot() > 7 || patchUserInfoReq.getWalkTimeSlot() < 1)
                return new BaseResponse<>(new BaseException(BaseResponseStatus.INVALID_WALK_TIME_SLOT).getStatus());


            try {
                int result = userService.postUserInfo(userIdx, patchUserInfoReq);

                String resultMsg = "정보 저장에 성공하였습니다.";
                if(result == 0)
                    resultMsg = "정보 저장에 실패하였습니다.";

                return new BaseResponse<>(resultMsg);
            } catch (BaseException exception) {
                return new BaseResponse<>((exception.getStatus()));
            }

        } catch (BaseException exception) {
            return new BaseResponse<>(exception.getStatus());
        }


    }

    /**
     * 태그 검색 API
     * [GET] /users/tags?tag=""
     */
    // Query String
    @ResponseBody
    @GetMapping("/tags")
    public BaseResponse<List<GetTagRes>> getTags(@RequestParam(required = false) String tag) {
        try {
            // userId(구글이나 카카오에서 보낸 ID) 추출 (복호화)
            String userId = jwtService.getUserId();
            System.out.println("userId = " + userId);
            // userId로 userIdx 추출
            int userIdx = userProvider.getUserIdx(userId);

            if (tag == null) { // Query String(검색어)를 입력하지 않았을 경우
                return new BaseResponse<>(new BaseException(BaseResponseStatus.NEED_TAG_INFO).getStatus());
            }
            List<GetTagRes> tagResult = userProvider.getTagResult(userIdx, tag);
            return new BaseResponse<>(tagResult);
        } catch (BaseException exception) {
            return new BaseResponse<>((exception.getStatus()));
        }

    }

}
