package com.umc.footprint.src.walks;

import com.umc.footprint.config.BaseException;
import com.umc.footprint.src.walks.model.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static com.umc.footprint.config.BaseResponseStatus.DATABASE_ERROR;

@Service
public class WalkProvider {
    private final WalkDao walkDao;

    @Autowired
    public WalkProvider(WalkDao walkDao) {
        this.walkDao = walkDao;
    }


    public GetWalkInfo getWalkInfo(int walkIdx) throws BaseException {
        try {
            GetWalkInfo getWalkInfo = walkDao.getWalkInfo(walkIdx);
            return getWalkInfo;
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
  }

    //
    public Float getGoalRate(SaveWalk walk) throws BaseException {
        try {
            System.out.println("WalkProvider.getGoalRate");
            // 산책 시간
            Integer walkTime = Math.toIntExact(Duration.between(walk.getStartAt(), walk.getEndAt()).toMinutes());
            // 산책 목표 시간
            Integer walkGoalTime = walkDao.getWalkGoalTime(walk.getUserIdx());
            // (산책 끝 시간 - 산책 시작 시간) / 산책 목표 시간
            float goalRate =walkTime.floatValue() / walkGoalTime.floatValue();

            // 100퍼 넘을 시 100으로 고정
            if (goalRate >= 1.0) {
                goalRate = 1.0f;
            }

            return goalRate;
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }


    public List<Integer> getAcquiredBadgeIdxList(int userIdx) throws BaseException {
        try {
            // 조건에 부합하는 뱃지 조회
            GetBadgeIdx getBadgeIdx = walkDao.getAcquiredBadgeIdxList(userIdx);
            System.out.println("getBadgeIdx.getDistanceBadgeIdx() = " + getBadgeIdx.getDistanceBadgeIdx());
            System.out.println("getBadgeIdx.getRecordBadgeIdx() = " + getBadgeIdx.getRecordBadgeIdx());
            // 원래 가지고 있던 뱃지 조회
            List<Integer> getOriginBadgeIdxList = walkDao.getOriginBadgeIdxList(userIdx);
            for (Integer badgeIdx : getOriginBadgeIdxList) {
                System.out.println("badgeIdx = " + badgeIdx);
            }

            // 얻은 뱃지
            List<Integer> acquiredBadgeIdxList = new ArrayList<>();
            // 거리 관련해서 얻은 뱃지
            List<Integer> acquiredDistanceBadgeIdxList = new ArrayList<>();
            // 기록 관련해서 얻은 뱃지
            List<Integer> acquiredRecordBadgeIdxList = new ArrayList<>();

            // 원래 갖고 있던 뱃지(2~5)의 가장 큰 값
            int originMaxDistanceBadgeIdx = 1;
            // 원래 갖고 있던 뱃지(6~8)의 가장 큰 값
            int originMaxRecordBadgeIdx = 1;
            for (Integer originBadgeIdx : getOriginBadgeIdxList) {
                if (originBadgeIdx >= 2 && originBadgeIdx <= 5) {
                    originMaxDistanceBadgeIdx = originBadgeIdx;
                }
                if (originBadgeIdx >= 6 && originBadgeIdx <= 8) {
                    originMaxRecordBadgeIdx = originBadgeIdx;
                }
            }
            // 거리 관련 얻은 뱃지 리스트에 저장
            if (getBadgeIdx.getDistanceBadgeIdx() > originMaxDistanceBadgeIdx) {
                // 누적 거리 뱃지를 여러 개 달성할 경우
                for (int i = originMaxDistanceBadgeIdx + 1; i <= getBadgeIdx.getDistanceBadgeIdx(); i++) {
                    acquiredBadgeIdxList.add(i);
                }
            }

            // 기록 관련 얻은 뱃지 리스트에 저장
            if (getBadgeIdx.getRecordBadgeIdx() > originMaxRecordBadgeIdx) {
                acquiredBadgeIdxList.add(getBadgeIdx.getRecordBadgeIdx());
            }

            return acquiredBadgeIdxList;
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }

    }

    // 뱃지 idx에 해당하는 이름과 url 반환
    public List<PostWalkRes> getBadgeInfo(List<Integer> acquiredBadgeIdxList) throws BaseException {
        try {
            List<PostWalkRes> postWalkResList = walkDao.getBadgeInfo(acquiredBadgeIdxList);
            return postWalkResList;
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }


}
