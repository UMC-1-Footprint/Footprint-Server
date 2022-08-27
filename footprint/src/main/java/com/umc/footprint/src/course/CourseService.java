package com.umc.footprint.src.course;

import com.umc.footprint.config.BaseException;
import com.umc.footprint.src.model.Course;
import com.umc.footprint.src.model.CourseTag;
import com.umc.footprint.src.model.Mark;
import com.umc.footprint.src.model.UserCourse;
import com.umc.footprint.src.repository.*;
import com.umc.footprint.src.walks.WalkService;
import com.umc.footprint.src.course.model.GetCourseInfoRes;
import com.umc.footprint.src.course.model.GetCourseListReq;
import com.umc.footprint.src.course.model.GetCourseListRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {

    private final WalkService walkService;
    private final HashtagRepository hashtagRepository;
    private final CourseRepository courseRepository;
    private final CourseTagRepository courseTagRepository;
    private final UserCourseRepository userCourseRepository;
    private final MarkRepository markRepository;

    /** API.32 사용자 디바이스 지도 좌표안에 존재하는 모든 코스들을 가져온다. */
    public List<GetCourseListRes> getCourseList(GetCourseListReq getCourseListReq, int userIdx){

        // response로 보낼 코스 정보를 저장할 List
        List<GetCourseListRes> courseListResList = new ArrayList<>();

        // 1. DB에 존재하는 모든 코스 정보중에서 디바이스 지도 좌표안에 존재하는 코스 추출
        List<Course> allCourseInDB = courseRepository.findAllByStatus("ACTIVE");

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
                List<CourseTag> courseTagMappingList = courseTagRepository.findAllByCourseIdx(course.getCourseIdx());

                List<String> courseTags = new ArrayList<>();
                for(CourseTag courseTag: courseTagMappingList){
                    courseTags.add(hashtagRepository.findByHashtagIdx(courseTag.getHashtagIdx()).get().getHashtag());
                }

                // 2-2. 코스 경험 횟수 계산
                List<UserCourse> userCourseList = userCourseRepository.findByCourseIdx(course.getCourseIdx());

                int courseCountSum = 0;
                for(UserCourse userCourse: userCourseList) {
                    courseCountSum += userCourse.getCourseCount();
                }

                // 2-2. 유저가 해당 코스를 mark 했는지 확인
                System.out.println("Check Point 3");
                List<Mark> markList = markRepository.findByUserIdx(userIdx);
                boolean userCourseMark = false;
                for(Mark mark : markList) {
                    if (mark.getCourseIdx().equals(course.getCourseIdx())) {
                        userCourseMark = true;
                    }
                }

                // 2-3. 해당 코스에 사진이 들어있는지 확인
                // 사진이 없다면 기본 이미지 URL 입력
                String courseImgUrl = course.getCourseImg();
                if(courseImgUrl.isBlank()){
                    courseImgUrl = "https://mystepsbucket.s3.ap-northeast-2.amazonaws.com/BaseImage/%E1%84%8F%E1%85%A9%E1%84%89%E1%85%B3%E1%84%80%E1%85%B5%E1%84%87%E1%85%A9%E1%86%AB%E1%84%8B%E1%85%B5%E1%84%86%E1%85%B5%E1%84%8C%E1%85%B5.png"; //* 기본 이미지 URL
                }

                // 2-3. courseListResList에 해당 추천 코스 정보 add
                courseListResList.add(GetCourseListRes.builder()
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

        return courseListResList;
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
}
