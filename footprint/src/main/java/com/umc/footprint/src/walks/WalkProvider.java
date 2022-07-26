package com.umc.footprint.src.walks;

import com.umc.footprint.config.BaseException;
import com.umc.footprint.config.EncryptProperties;
import com.umc.footprint.src.walks.model.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.umc.footprint.config.BaseResponseStatus.DATABASE_ERROR;

@Slf4j
@Service
public class WalkProvider {
    private final WalkDao walkDao;
    private final EncryptProperties encryptProperties;

    @Autowired
    public WalkProvider(WalkDao walkDao, EncryptProperties encryptProperties) {
        this.walkDao = walkDao;
        this.encryptProperties = encryptProperties;
    }


    public List<Integer> getAcquiredBadgeIdxList(int userIdx) throws BaseException {
        try {
            // 조건에 부합하는 뱃지 조회
            GetBadgeIdx getBadgeIdx = walkDao.getAcquiredBadgeIdxList(userIdx);
            // 원래 가지고 있던 뱃지 조회
            List<Integer> getOriginBadgeIdxList = walkDao.getOriginBadgeIdxList(userIdx);
            log.debug("원래 가지고 있던 뱃지들: {}", getOriginBadgeIdxList);

            // 얻은 뱃지
            List<Integer> acquiredBadgeIdxList = new ArrayList<>();

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

            if (getOriginBadgeIdxList.size() == 0) {
                acquiredBadgeIdxList.add(1);
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


    public int checkFirstWalk(int userIdx) throws BaseException {
        try {
            return walkDao.checkFirstWalk(userIdx);
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    public int getWalkWholeIdx(int walkIdx, int userIdx) throws BaseException {
        try {
            log.debug("walkIdx: {}", walkIdx);
            log.debug("userIdx: {}", userIdx);
            return walkDao.getWalkWholeIdx(walkIdx, userIdx);
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

}
