package com.umc.footprint.config;

import lombok.Getter;

/**
 * [ 1000단위 ]
 *  1000 : 요청 성공
 *  2 : Request 오류
 *  3 : Reponse 오류
 *  4 : DB, Server 오류
 *
 * [ 100단위 ]
 *  0 : 공통 오류
 *  1 : users 오류
 *  2 : walks 오류
 *  3 : notice 오류
 *
 * [10단위]
 *  0~19 : Common
 *  20~39 : GET
 *  40~59 : POST
 *  60~79 : PATCH
 *  80~99 : else
 */

@Getter
public enum BaseResponseStatus {
    /**
     * 1000 : 요청 성공
     */
    SUCCESS(true, 1000, "요청에 성공하였습니다."),

    /**
     * 2000 : Request 오류
     */

    INVALID_DATE(false,2000, "잘못된 날짜 형식입니다."),
    EMPTY_JWT(false, 2001, "JWT를 입력해주세요."),
    INVALID_JWT(false, 2002, "유효하지 않은 JWT입니다."),
    INVALID_USER_JWT(false,2003,"권한이 없는 유저의 접근입니다."),
    EXPIRED_JWT(false, 2004, "만료된 JWT입니다."),
    INVALID_ENCRYPT_STRING(false, 2005, "복호화를 할 수 없습니다."),
    INVALID_USERIDX(false,2100,"잘못된 유저 인덱스입니다."),
    NOT_EXIST_USER(false, 2101, "존재하지 않는 유저입니다."),
    NOT_EXIST_COURSE(false, 2102, "존재하지 않는 코스입니다."),
    NOT_EXIST_USER_IN_PREV_GOAL(false,2118, "이전 달 설정한 목표가 없습니다."),
    NOT_EXIST_USER_IN_GOAL(false,2119, "아직 목표 설정을 하지 않은 사용자입니다."),
    NOT_EXIST_USER_IN_WALK(false,2120, "아직 산책 기록이 없는 사용자입니다."),
    NOT_EXIST_WALK(false,2121, "해당하는 산책을 찾을 수 없습니다."),
    INACTIVE_USER(false, 2122, "비활성화된 유저입니다."),
    BLACK_USER(false, 2123, "블랙 유저입니다."),
    NEED_TAG_INFO(false, 2124, "검색하고자 하는 태그를 입력해주세요."),
    NO_EXIST_RESULT(false, 2125, "검색 결과가 존재하지 않습니다."),
    EXIST_USER_ERROR(false, 2140,"이미 존재하는 유저입니다."),
    MIN_DAYIDX(false, 2141,"산책요일을 최소 하나 이상 선택해야 합니다."),
    MAX_DAYIDX(false, 2142,"선택된 산책 요일이 너무 많습니다."),
    INVALID_DAYIDX(false, 2143,"잘못된 요일 번호가 속해 있습니다."),
    OVERLAP_DAYIDX(false, 2144,"중복되는 요일 번호가 속해 있습니다."),
    MIN_WALK_GOAL_TIME(false, 2145,"목표산책시간이 최소시간 미만입니다."),
    MAX_WALK_GOAL_TIME(false, 2146,"목표산책시간이 최대시간 초과입니다."),
    INVALID_WALK_TIME_SLOT(false, 2147,"잘못된 산책 시간대입니다."),
    POST_USERS_EMPTY_EMAIL(false, 2148, "이메일을 입력해주세요."),
    POST_USERS_INVALID_EMAIL(false, 2149, "이메일 형식을 확인해주세요."),
    POST_USERS_EMPTY_USERID(false, 2150, "유저 ID를 입력해주세요."),
    DUPLICATED_COURSE_NAME(false, 2151, "중복된 코스 이름입니다."),
    MAX_NICKNAME_LENGTH(false, 2160, "닉네임은 8자를 초과할 수 없습니다."),
    INVALID_BIRTH(false, 2161, "생년월일 값은 0000-00-00이 될 수 없습니다."),

    INVALID_WALKIDX(false, 2200, "잘못된 산책 인덱스입니다."),
    INVALID_FOOTPRINTIDX(false, 2201, "잘못된 발자국 인덱스입니다."),
    INVALID_COORDINATES(false, 2202, "잘못된 산책 인덱스입니다."),
    NO_FOOTPRINT_IN_WALK(false, 2221, "해당 산책 기록에는 발자국이 존재하지 않습니다."),

    EXCEED_FOOTPRINT_SIZE(false, 2241, "photoMatchNumList를 확인해주십시요."),
    EMPTY_WALK_PHOTO(false, 2242, "산책 이미지를 입력해주세요."),
    DELETED_FOOTPRINT(false, 2260, "이미 삭제된 발자국입니다."),
    NO_EXIST_FOOTPRINT(false, 2261, "존재하지 않는 발자국입니다."),
    REQUEST_ERROR(false, 2262, "잘못된 산책 인덱스입니다."), // 임시 추가
    DELETED_WALK(false, 2263, "이미 삭제된 산책입니다."),

    INVALID_BADGEIDX(false,2270, "존재하지 않는 뱃지입니다."),
    NOT_EXIST_USER_BADGE(false, 2271, "해당 사용자가 획득하지 못한 뱃지입니다."),
    NO_BADGE_USER(false, 2272, "사용자가 획득한 뱃지가 없습니다."),

    INVALID_NOTICE_IDX(false,2320,"잘못된 인덱스입니다."),

    NOT_EXIST_MARK_COURSE(false, 2410, "찜한 코스가 존재하지 않습니다."),

    /**
     * 3000 : Response 오류
     */
    NO_MONTHLY_BADGE(false, 3030, "이번 달에 획득한 뱃지가 없습니다. (PRO, LOVER, MASTER)"),

    /**
     * 4000 : Database, Server 오류
     */
    DATABASE_ERROR(false, 4000, "데이터베이스 연결에 실패하였습니다."),
    SERVER_ERROR(false, 4001, "서버와의 연결에 실패하였습니다."),
    MODIFY_WKT_FAIL(false, 4002, "WKT 변환을 실패하였습니다."),
    ENCRYPT_FAIL(false, 4003, "암호화를 실패하였습다."),
    MODIFY_OBJECT_FAIL(false, 4004, "Request 객체 변환에 실패하였습니다."),
    DECRYPT_FAIL(false, 4005, "복호화를 실패하였습다."),
    S3UPLOAD_ERROR(false, 4080, "파일 업로드에 실패하였습니다."),
    MODIFY_USER_GOAL_FAIL(false, 4160, "사용자 목표 변경에 실패하였습니다."),
    MODIFY_USERINFO_FAIL(false, 4161, "유저 정보 변경에 실패하였습니다."),
    DELETE_FOOTPRINT_FAIL(false, 4262, "발자국 삭제에 실패하였습니다."),
    INTERNAL_SERVER_ERROR(false, 4263, "서버 내부 에러"),
    MODIFY_FOOTPRINT_FAIL(false, 4261, "발자국 수정에 실패하였습니다.");

    private final boolean isSuccess;
    private final int code;
    private final String message;

    private BaseResponseStatus(boolean isSuccess, int code, String message) { //BaseResponseStatus 에서 각 해당하는 코드를 생성자로 맵핑
        this.isSuccess = isSuccess;
        this.code = code;
        this.message = message;
    }
}
