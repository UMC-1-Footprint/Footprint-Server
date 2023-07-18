package com.umc.footprint.src.course;

import static com.umc.footprint.config.BaseResponseStatus.*;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.umc.footprint.config.BaseException;
import com.umc.footprint.src.common.model.entity.Hashtag;
import com.umc.footprint.src.common.model.entity.Photo;
import com.umc.footprint.src.common.model.entity.Tag;
import com.umc.footprint.src.common.model.vo.HashtagInfo;
import com.umc.footprint.src.common.repository.PhotoRepository;
import com.umc.footprint.src.common.repository.TagRepository;
import com.umc.footprint.src.course.model.dto.GetCourseDetailsRes;
import com.umc.footprint.src.course.model.dto.GetCourseInfoRes;
import com.umc.footprint.src.course.model.dto.GetCourseListReq;
import com.umc.footprint.src.course.model.dto.GetCourseListRes;
import com.umc.footprint.src.course.model.dto.GetWalkDetailsRes;
import com.umc.footprint.src.course.model.dto.PatchCourseDetailsReq;
import com.umc.footprint.src.course.model.dto.PostCourseDetailsReq;
import com.umc.footprint.src.course.model.dto.projection.CourseHashTagProjection;
import com.umc.footprint.src.course.model.dto.projection.HashTagProjection;
import com.umc.footprint.src.course.model.entity.Course;
import com.umc.footprint.src.course.model.entity.CourseTag;
import com.umc.footprint.src.course.model.entity.Mark;
import com.umc.footprint.src.course.model.entity.UserCourse;
import com.umc.footprint.src.course.model.vo.CourseInfo;
import com.umc.footprint.src.course.model.vo.CourseStatus;
import com.umc.footprint.src.course.repository.CourseRepository;
import com.umc.footprint.src.course.repository.CourseTagRepository;
import com.umc.footprint.src.course.repository.MarkRepository;
import com.umc.footprint.src.course.repository.UserCourseRepository;
import com.umc.footprint.src.footprints.model.entity.Footprint;
import com.umc.footprint.src.users.UserService;
import com.umc.footprint.src.users.model.dto.GetUserDateRes;
import com.umc.footprint.src.walks.WalkService;
import com.umc.footprint.src.walks.model.dto.GetWalksRes;
import com.umc.footprint.src.walks.model.entity.Walk;
import com.umc.footprint.src.walks.model.vo.UserDateWalk;
import com.umc.footprint.src.walks.repository.WalkRepository;
import com.umc.footprint.utils.AES128;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {

    private final WalkService walkService;
    private final CourseRepository courseRepository;
    private final CourseTagRepository courseTagRepository;
    private final UserCourseRepository userCourseRepository;
    private final MarkRepository markRepository;
    private final WalkRepository walkRepository;
    private final TagRepository tagRepository;
    private final PhotoRepository photoRepository;
    private final UserService userService;

    @Value("${image.course}")
    private String defaultCourseImage;


    /** API.32 사용자 디바이스 지도 좌표안에 존재하는 모든 코스들을 가져온다. */
    public GetCourseListRes getCourseList(GetCourseListReq getCourseListReq, int userIdx){

        // response로 보낼 코스 정보를 저장할 List
        List<CourseInfo> courseListResList = new ArrayList<>();

        // 1. DB에 존재하는 모든 코스 정보중에서 디바이스 지도 좌표안에 존재하는 코스 추출
        List<Course> allCourseInDB = courseRepository.findAllByStatus(CourseStatus.ACTIVE);

        System.out.println("allCourseInDB.size() = " + allCourseInDB.size());

        for(Course course : allCourseInDB){
            System.out.println("course = " + course);
            double courseLat = course.getStartCoordinate().getX();
            double courseLong = course.getStartCoordinate().getY();

            // 2. DB에 있는 코스들중 보내준 위도 / 경도 사이에 존재하는 코스들만 추출
            if(courseLat < getCourseListReq.getNorth() && courseLat > getCourseListReq.getSouth()
                    && courseLong < getCourseListReq.getEast() && courseLong > getCourseListReq.getWest()){

                // 2-1. 코스 태그 추출
                // CourseTag 테이블에서 hashtagIdx 추출후 HashTag 테이블에서 인덱스에 해당하는 해시태그들 List화
                List<String> courseTags = getCourseTags(course);

                // 2-2. 코스 경험 횟수 계산
                int courseCountSum = getCourseCount(course.getCourseIdx());

                // 2-2. 유저가 해당 코스를 mark 했는지 확인
                Optional<Mark> userMark = markRepository.findByCourseIdxAndUserIdx(course.getCourseIdx(), userIdx);

                boolean userCourseMark = false;

                if (userMark.isPresent()) {
                    userCourseMark = userMark.get().getMark();
                }

                // 2-3. 해당 코스에 사진이 들어있는지 확인
                // 사진이 없다면 기본 이미지 URL 입력
                String courseImgUrl = getCourseImage(course.getCourseImg());

                // 2-4. courseListResList에 해당 추천 코스 정보 add
                courseListResList.add(CourseInfo.builder()
                        .courseIdx(course.getCourseIdx())
                        .startLat(courseLat)
                        .startLong(courseLong)
                        .courseName(course.getCourseName())
                        .courseDist(course.getLength())
                        .courseTime(course.getCourseTime())
                        .courseCount(courseCountSum)
                        .courseLike(course.getLikeNum())
                        .courseImg(courseImgUrl)
                        .courseTags(courseTags)
                        .userCourseMark(userCourseMark)
                        .build());
            }
        }

        // 2. courseListResList DTO List를 courseDist로 정렬
        Collections.sort(courseListResList);

        return GetCourseListRes.builder()
                .getCourseLists(courseListResList)
                .build();
    }

    public GetCourseListRes getMarkCourses(String userId) throws BaseException {
        Integer userIdx = userService.getUserIdxByUserId(userId);
        List<Integer> courseIdxes = markRepository.getCourseIdxByUserIdxAndMark(userIdx, Boolean.TRUE);

        if(courseIdxes.size()==0) {
            return new GetCourseListRes(new ArrayList<>());
        }

        List<Course> courses = courseRepository.getAllByCourseIdx(courseIdxes);

        List<CourseInfo> getCourses = new ArrayList<>();
        for(Course course : courses) {
            List<String> courseTags = getCourseTags(course);
            int courseCountSum = getCourseCount(course.getCourseIdx());
            String courseImgUrl = getCourseImage(course.getCourseImg());

            getCourses.add(
                    CourseInfo.of(course,
                            courseCountSum,
                            courseImgUrl,
                            courseTags,
                            Boolean.TRUE)
            );
        }
        return new GetCourseListRes(getCourses);
    }

    public GetCourseListRes getMyRecommendCourses(String userId) throws BaseException {
        Integer userIdx = userService.getUserIdxByUserId(userId);
        List<Course> courses = courseRepository.getAllByUserIdxAndStatus(userIdx, CourseStatus.ACTIVE);

        List<CourseInfo> getCourses = new ArrayList<>();
        for(Course course : courses) {
            List<String> courseTags = getCourseTags(course);
            int courseCountSum = getCourseCount(course.getCourseIdx());
            String courseImgUrl = getCourseImage(course.getCourseImg());

            Optional<Mark> userMark = markRepository.findByCourseIdxAndUserIdx(course.getCourseIdx(), userIdx);
            boolean userCourseMark = false;
            if (userMark.isPresent()) {
                userCourseMark = userMark.get().getMark();
            }

            getCourses.add(
                    CourseInfo.of(course, courseCountSum, courseImgUrl, courseTags, userCourseMark)
            );
        }
        return new GetCourseListRes(getCourses);
    }

    @SneakyThrows
    public GetWalksRes getMyAllCourse(String userId) throws BaseException {
        Integer userIdx = userService.getUserIdxByUserId(userId);
        List<Walk> walks = walkService.getMyAllWalk(userIdx);

        if(walks==null) {
            return null;
        }

        List<GetUserDateRes> getUserDateResList = new ArrayList<>();

        int walkIndex = 0;
        for (Walk userWalk : walks) {
            walkIndex++;
            if (!userWalk.getStatus().equals("ACTIVE")) {
                continue;
            }

            UserDateWalk userDateWalk = UserDateWalk.builder()
                .walkIdx(walkIndex)
                .startTime(userWalk.getStartAt().format(DateTimeFormatter.ofPattern("HH:mm")))
                .endTime(userWalk.getEndAt().format(DateTimeFormatter.ofPattern("HH:mm")))
                .pathImageUrl(AES128.decrypt(userWalk.getPathImageUrl()))
                    .build();

            List<Footprint> footprintList = userWalk.getFootprintList();
            List<String> tagString = new ArrayList<>();
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

        return new GetWalksRes(getUserDateResList);
    }

    // 해당 코스의 해시태그 목록 조회
    public List<String> getCourseTags(Course course) {
        List<CourseTag> courseTagMappingList = courseTagRepository.findAllByCourseAndStatus(course, CourseStatus.ACTIVE);
        List<String> courseTags = new ArrayList<>();
        for(CourseTag courseTag: courseTagMappingList){
            courseTags.add(courseTag.getHashtag().getHashtag());
        }
        return courseTags;
    }

    // 해당 코스 경험 횟수 조회
    public int getCourseCount(int courseIdx) {
        List<UserCourse> userCourseList = userCourseRepository.findByCourseIdx(courseIdx);
        int courseCountSum = 0;
        for(UserCourse userCourse: userCourseList) {
            courseCountSum += userCourse.getCourseCount();
        }
        return courseCountSum;
    }

    // 해당 코스 이미지 조회 및 복호화
    @SneakyThrows
    public String getCourseImage(String courseImg) {
        if(courseImg.length()==0 || courseImg.equals("")) {
            return defaultCourseImage;
        } else if(courseImg.startsWith("https://")) {
            return courseImg;
        }
        courseImg = AES128.decrypt(courseImg);
        if(courseImg.equals("")) {
            courseImg = defaultCourseImage;
        }
        return courseImg;
    }

    /** API.34 원하는 코스의 경로 좌표와 상세 설명을 가져온다. */
    public GetCourseInfoRes getCourseInfo(int courseIdx) throws BaseException {

        // 1. courseIdx로 코스 정보 가져오기
        Optional<Course> course = courseRepository.findByCourseIdx(courseIdx);

        // 2. response 생성 후 reutrn(coordinate 복호화)
        return GetCourseInfoRes.builder()
                .coordinate(walkService.convertStringTo2DList(course.get().getCoordinate()))
                .courseDisc(course.get().getDescription())
                .build();

    }

    public String modifyMark(int courseIdx, String userId) throws BaseException {
        // userIdx 추출
        Integer userIdx = userService.getUserIdxByUserId(userId);

        // 저장된 코스 불러오기
        Course savedCourse = courseRepository.findById(courseIdx).get();

        Optional<Mark> optionalMark = markRepository.findByCourseIdxAndUserIdx(courseIdx, userIdx);

        if (optionalMark.isPresent()) {
            Mark savedMark = optionalMark.get();

            savedMark.modifyMark();

            Mark modifiedMark = markRepository.save(savedMark);
            if (modifiedMark.getMark()) {
                return "찜하기";
            } else {
                return "찜하기 취소";
            }
        } else {
            Mark newMark = Mark.builder()
                    .courseIdx(courseIdx)
                    .userIdx(userIdx)
                    .mark(true)
                    .build();

            markRepository.save(newMark);

            return "찜하기";
        }
    }

    /**
     *
     * @param courseName String
     * @return 코스 상세 정보들
     * @throws BaseException
     */
    public GetCourseDetailsRes getCourseDetails(String courseName) throws BaseException {
        CourseHashTagProjection courseDetails = courseRepository.findCourseDetails(courseName);

        if (courseDetails == null) {
            log.info("코스 이름에 해당하는 코스가 없습니다.");
            throw new BaseException(NOT_EXIST_COURSE);
        }

        List<HashTagProjection> courseAllTags = walkRepository.findCourseAllTags(courseDetails.getWalkIdx());
        List<HashTagProjection> courseSelectedTags = courseTagRepository.findCourseSelectedTags(courseDetails.getCourseIdx());

        Duration between = Duration.between(courseDetails.getStartAt(), courseDetails.getEndAt());
        Integer courseTime = (int) between.getSeconds()/60;

        String decryptedImg;
        if (courseDetails.getCourseImg() == null || courseDetails.getCourseImg().isEmpty()) {
            decryptedImg = "";
        } else {
            try {
                decryptedImg = AES128.decrypt(courseDetails.getCourseImg());
            } catch (Exception exception) {
                throw new BaseException(DECRYPT_FAIL);
            }
        }

        return new GetCourseDetailsRes(
                courseDetails.getCourseIdx(),
                courseDetails.getAddress(),
                courseDetails.getDescription(),
                courseDetails.getWalkIdx(),
                courseTime,
                courseDetails.getDistance(),
                decryptedImg,
                courseAllTags,
                courseSelectedTags
        );
    }

    @Transactional(propagation = Propagation.NESTED, rollbackFor = Exception.class)
    public String postCourseDetails(PostCourseDetailsReq postCourseDetailsReq, String userId) throws BaseException {
        Integer userIdx = userService.getUserIdxByUserId(userId);

        String encryptedCoordinates = "";
        String courseImg;

        // 좌표 암호화
        try {
            encryptedCoordinates = AES128.encrypt(
                walkService.convert2DListToString(postCourseDetailsReq.getCoordinates()));
        } catch (Exception exception) {
            log.info("좌표 암호화 실패");
            throw new BaseException(ENCRYPT_FAIL);
        }

        //코스 이미지 암호화
        if (postCourseDetailsReq.getCourseImg() == null || postCourseDetailsReq.getCourseImg().isEmpty()) {
            courseImg = "";
        } else {
            try {
                courseImg = AES128.encrypt(postCourseDetailsReq.getCourseImg());
            } catch (Exception exception) {
                log.info("요청한 코스 이미지 암호화 실패");
                throw new BaseException(ENCRYPT_FAIL);
            }
        }

        // 코스 이름 중복 확인
        if (courseRepository.existsByCourseNameAndStatus(postCourseDetailsReq.getCourseName(), CourseStatus.ACTIVE)) {
            log.info("코스 이름 중복");
            throw new BaseException(DUPLICATED_COURSE_NAME);
        }

        // Course Entity 에 저장
        Course savedCourse = courseRepository.save(Course.builder()
                .courseName(postCourseDetailsReq.getCourseName())
                .courseImg(courseImg)
                .startCoordinate(extractStartCoordinate(postCourseDetailsReq.getCoordinates()))
                .coordinate(encryptedCoordinates)
                .address(postCourseDetailsReq.getAddress())
                .length(postCourseDetailsReq.getLength())
                .courseTime(postCourseDetailsReq.getCourseTime())
                .description(postCourseDetailsReq.getDescription())
                .walkIdx(postCourseDetailsReq.getWalkIdx())
                .likeNum(0)
                .userIdx(userIdx)
                .status(CourseStatus.ACTIVE)
                .build());

        if (savedCourse == null) {
            log.info("Course 저장 실패");
            throw new BaseException(DATABASE_ERROR);
        }

        // CourseTag Entity 에 저장

        if (!postCourseDetailsReq.getHashtags().isEmpty()) {
            List<CourseTag> courseTags = new ArrayList<>();
            for (HashtagInfo hashtagInfo : postCourseDetailsReq.getHashtags()) {
                CourseTag courseTag = CourseTag.builder()
                        .status(CourseStatus.ACTIVE)
                        .build();

                courseTag.setCourse(savedCourse);

                courseTag.setHashtag(Hashtag.builder()
                        .hashtagIdx(hashtagInfo.getHashtagIdx())
                        .hashtag(hashtagInfo.getHashtag())
                        .build());

                courseTags.add(courseTag);
            }
            List<CourseTag> savedCourseTags = courseTagRepository.saveAll(courseTags);

            if (savedCourseTags.isEmpty()) {
                log.info("CourseTag 저장 실패");
                throw new BaseException(DATABASE_ERROR);
            }
        }

        return "코스가 등록되었습니다.";
    }

    public Point extractStartCoordinate(List<List<Double>> coordinates) throws BaseException {
        List<Double> firstSection = coordinates.get(0);

        if (firstSection == null) {
            log.info("잘못된 좌표입니다.");
            throw new BaseException(INVALID_COORDINATES);
        }

        StringBuilder st = new StringBuilder();
        st.append("Point")
                .append(" (").append(firstSection.get(0)).append(" ").append(firstSection.get(1)).append(")");
        return (Point) wktToGeometry(st.toString());
    }

    public Geometry wktToGeometry(String wellKnownText) throws BaseException {
        try {
            return new WKTReader().read(wellKnownText);
        } catch (Exception exception) {
            log.info("잘못된 well known text 입니다.");
            throw new BaseException(MODIFY_WKT_FAIL);
        }
    }

    public String modifyCourseLike(Integer courseIdx, String userId, Integer evaluate) throws BaseException {
        Integer userIdx = userService.getUserIdxByUserId(userId);

        Optional<Course> OptionalCourse = courseRepository.findById(courseIdx);

        if (OptionalCourse.isEmpty()) {
            throw new BaseException(NOT_EXIST_COURSE);
        }

        Optional<UserCourse> byCourseIdxAndUserIdx = userCourseRepository.findByCourseIdxAndUserIdx(courseIdx, userIdx);

        UserCourse savedUserCourse;
        Course savedCourse = OptionalCourse.get();

        // UserCourse에 저장
        if (byCourseIdxAndUserIdx.isPresent()) {
            UserCourse rawUserCourse = byCourseIdxAndUserIdx.get();

            UserCourse modifiedUserCourse = UserCourse.builder()
                    .userCourseIdx(rawUserCourse.getUserCourseIdx())
                    .userIdx(rawUserCourse.getUserIdx())
                    .courseIdx(rawUserCourse.getCourseIdx())
                    .walkIdx(rawUserCourse.getWalkIdx())
                    .courseCount(rawUserCourse.getCourseCount() + 1L)
                    .build();

            savedUserCourse = userCourseRepository.save(modifiedUserCourse);
        } else {
            savedUserCourse = userCourseRepository.save(
                    UserCourse.builder()
                            .userIdx(userIdx)
                            .courseIdx(courseIdx)
                            .walkIdx(savedCourse.getWalkIdx())
                            .courseCount(1L)
                            .build()
            );
        }

        if (evaluate == 1) { // 좋았어요 눌렀을 때
            // likeNum 1 증가
            savedCourse.addLikeNum();

            // Course 저장
            courseRepository.save(savedCourse);
        }

        if (savedUserCourse == null) {
            log.info("UserCourse 저장 실패");
            throw new BaseException(DATABASE_ERROR);
        }

        return "좋아요";
    }

    public String modifyCourseDetails(PatchCourseDetailsReq patchCourseDetailsReq, String userId) throws BaseException {
        Integer userIdx = userService.getUserIdxByUserId(userId);

        Course savedCourse = courseRepository.findById(patchCourseDetailsReq.getCourseIdx()).get();

        if (savedCourse == null) {
            throw new BaseException(NOT_EXIST_COURSE);
        }

        // 코스 이름 변경 시 중복 확인
        if (!savedCourse.getCourseName().equals(patchCourseDetailsReq.getCourseName())) {
            if (courseRepository.existsByCourseNameAndStatus(patchCourseDetailsReq.getCourseName(), CourseStatus.ACTIVE)) {
                log.info("코스 이름 중복");
                throw new BaseException(DUPLICATED_COURSE_NAME);
            }
        }

        if (patchCourseDetailsReq.getCourseImg() == null) {
            patchCourseDetailsReq.setCourseImg(defaultCourseImage);
        }

        String encryptCourseImg;
        try {
            encryptCourseImg = AES128.encrypt(patchCourseDetailsReq.getCourseImg());
        } catch (Exception exception) {
            throw new BaseException(ENCRYPT_FAIL);
        }

        // Course Entity 에 저장
        savedCourse.updateCourse(patchCourseDetailsReq, encryptCourseImg);
        Course modifiedCourse = courseRepository.save(savedCourse);

        if (modifiedCourse == null) {
            log.info("수정된 코스 저장 실패");
            throw new BaseException(DATABASE_ERROR);
        }

        // 해당 코스 태그 entity 불러와서 INACTIVE 로 만들기
        List<CourseTag> savedCourseTags = courseTagRepository.findAllByCourseAndStatus(modifiedCourse, CourseStatus.ACTIVE);
        if (savedCourseTags != null) {
            List<CourseTag> inactiveCourseTags = new ArrayList<>();
            for (CourseTag courseTag : savedCourseTags) {
                CourseTag inactiveCourseTag = CourseTag.builder()
                        .courseTagIdx(courseTag.getCourseTagIdx())
                        .status(CourseStatus.INACTIVE)
                        .build();
                inactiveCourseTag.setCourse(courseTag.getCourse());
                inactiveCourseTag.setHashtag(courseTag.getHashtag());
                inactiveCourseTags.add(inactiveCourseTag);
            }
            courseTagRepository.saveAll(inactiveCourseTags);
        }

        // CourseTag Entity 에 새로 저장
        if (!patchCourseDetailsReq.getHashtags().isEmpty()) {
            List<CourseTag> beforeSavedCourseTags = new ArrayList<>();

            // 코스, 해시 태그 매핑
            for (HashtagInfo hashtagInfo : patchCourseDetailsReq.getHashtags()) {
                CourseTag beforeSavedCourseTag = CourseTag.builder()
                        .status(CourseStatus.ACTIVE)
                        .build();

                beforeSavedCourseTag.setCourse(modifiedCourse);

                beforeSavedCourseTag.setHashtag(
                        Hashtag.builder()
                                .hashtagIdx(hashtagInfo.getHashtagIdx())
                                .hashtag(hashtagInfo.getHashtag())
                                .build()
                );
                beforeSavedCourseTags.add(beforeSavedCourseTag);
            }
            List<CourseTag> modifiedCourseTags = courseTagRepository.saveAll(beforeSavedCourseTags);

            if (modifiedCourseTags.isEmpty()) {
                log.info("수정된 코스 태그 저장 실패");
                throw new BaseException(DATABASE_ERROR);
            }
        }

        return "코스가 수정되었습니다.";
    }

    public GetWalkDetailsRes getWalkDetails(Integer walkNumber, String userId) throws BaseException {
//        Course savedCourse = courseRepository.findByCourseNameAndStatus(courseName, "ACTIVE");

        Integer userIdx = userService.getUserIdxByUserId(userId);

        Walk savedWalk = walkService.getWalkByNumber(walkNumber, userIdx);

        if (savedWalk == null) {
            log.info("{} 번째 산책을 찾을 수 없습니다.", walkNumber);
            throw new BaseException(NOT_EXIST_WALK);
        }

        List<ArrayList<Double>> coordinates;

        // 좌표 변환
        try {
            coordinates = walkService.convertStringTo2DList(savedWalk.getCoordinate());
        } catch (Exception exception) {
            throw new BaseException(INVALID_ENCRYPT_STRING);
        }

        ArrayList<HashtagInfo> hashtags = new ArrayList<>();
        ArrayList<String> photos = new ArrayList<>();
        for (Footprint footprint : savedWalk.getFootprintList()) {
            // 해시태그 불러오기
            List<Tag> savedTags = tagRepository.findAllByFootprintAndStatus(footprint, "ACTIVE");
            for (Tag savedTag : savedTags) {
                hashtags.add(HashtagInfo.builder()
                        .hashtagIdx(savedTag.getHashtag().getHashtagIdx())
                        .hashtag(savedTag.getHashtag().getHashtag())
                        .build());
            }

            // 사진 불러오기
            List<Photo> savedPhotos = photoRepository.findAllByFootprintAndStatus(footprint, "ACTIVE");
            for (Photo savedPhoto : savedPhotos) {
                photos.add(savedPhoto.getImageUrl());
            }
        }

        Duration between = Duration.between(savedWalk.getStartAt(), savedWalk.getEndAt());
        Integer walkTime = (int) between.getSeconds()/60;

        return GetWalkDetailsRes.builder()
                .walkIdx(savedWalk.getWalkIdx())
                .walkTime(walkTime)
                .distance(savedWalk.getDistance())
                .coordinates(coordinates)
                .hashtags(hashtags)
                .photos(photos)
                .build();
    }

    public GetWalkDetailsRes getWalkDetails_v2(Integer walkNumber, String userId) throws BaseException {
        Integer userIdx = userService.getUserIdxByUserId(userId);

        Walk savedWalk = walkService.getWalkByNumber(walkNumber, userIdx);


        if (savedWalk == null) {
            log.info("{} 번째 산책을 찾을 수 없습니다.", walkNumber);
            throw new BaseException(NOT_EXIST_WALK);
        }


        List<ArrayList<Double>> coordinates;

        // 좌표 변환
        try {
            coordinates = walkService.convertStringTo2DList(savedWalk.getCoordinate());
        } catch (Exception exception) {
            throw new BaseException(INVALID_ENCRYPT_STRING);
        }

        List<HashTagProjection> walkTags = walkRepository.findCourseAllTags(savedWalk.getWalkIdx());
        List<String> walkPhotos = photoRepository.findByWalkIdx(savedWalk.getWalkIdx());

        ArrayList<HashtagInfo> hashtags = new ArrayList<>();
        for (HashTagProjection hashTagProjection : walkTags) {
            hashtags.add(
                    HashtagInfo.builder()
                            .hashtagIdx(hashTagProjection.getHashtagIdx())
                            .hashtag(hashTagProjection.getHashtag())
                            .build()
            );
        }
        ArrayList<String> photos = new ArrayList<>();
        try {
            for (String walkPhoto : walkPhotos) {
                photos.add(AES128.decrypt(walkPhoto));
            }
        } catch (Exception exception) {
            log.info("사진 암호화 실패");
            throw new BaseException(DECRYPT_FAIL);
        }

        Duration between = Duration.between(savedWalk.getStartAt(), savedWalk.getEndAt());
        Integer walkTime = (int) between.getSeconds()/60;

        return GetWalkDetailsRes.builder()
                .walkIdx(savedWalk.getWalkIdx())
                .walkTime(walkTime)
                .distance(savedWalk.getDistance())
                .coordinates(coordinates)
                .hashtags(hashtags)
                .photos(photos)
                .build();
    }

    @Transactional(propagation = Propagation.NESTED, rollbackFor = Exception.class)
    public void deleteCourse(int courseIdx, String userId) throws BaseException {
        Course course = courseRepository.findByCourseIdx(courseIdx).orElseThrow(() -> new BaseException(NOT_EXIST_COURSE));
        Integer userIdx = userService.getUserIdxByUserId(userId);

        if(course.getUserIdx() != userIdx) {
            throw new BaseException(INVALID_USERIDX);
        }

        course.updateStatus(CourseStatus.INACTIVE);
        courseRepository.save(course);
    }

}
