package com.umc.footprint.src.repository;

import com.umc.footprint.src.model.Walk;
import com.umc.footprint.src.users.model.GetDayRateRes;
import com.umc.footprint.src.walks.model.GetFootprintCountInterface;
import com.umc.footprint.src.walks.model.GetMonthTotalInterface;
import com.umc.footprint.src.walks.model.ObtainedBadgeInterface;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;

import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WalkRepository extends JpaRepository<Walk, Integer> {
    @Query(value = "SELECT \n" +
            "       CASE\n" +
            "            WHEN (sum(Walk.distance) between 10 and 30) then 2\n" +
            "            when (sum(Walk.distance) between 30 and 50) then 3\n" +
            "            WHEN (sum(Walk.distance) between 50 and 100) then 4\n" +
            "            WHEN (sum(Walk.distance) > 100) then 5\n" +
            "        else 0\n" +
            "        end as distanceBadgeIdx,\n" +
            "       CASE\n" +
            "            when (count(Walk.walkIdx) = 1) then 1" +
            "            when (count(Walk.walkIdx) between 10 and 19) then 6\n" +
            "            when (count(Walk.walkIdx) between 20 and 29) then 7\n" +
            "            when (count(Walk.walkIdx) >= 30) then 8\n" +
            "        else 0\n" +
            "        end as recordBadgeIdx\n" +
            "From Walk\n" +
            "Where userIdx = ?1\n and status = 'ACTIVE'" +
            "group by Walk.userIdx", nativeQuery = true)
    ObtainedBadgeInterface getAcquiredBadgeIdxList(int userIdx);

    boolean existsByUserIdx(Integer userIdx);

    List<Walk> findAllByStatusAndUserIdx(String status,int userIdx);

    Optional<Walk> findByWalkIdx(Integer walkIdx);

    List<Walk> findAllByUserIdx(int userIdx);

    List<Walk> findAllByUserIdxOrderByWalkIdx(Integer userIdx);

    Optional<Walk> findTopByUserIdxAndStatusOrderByStartAtAsc(Integer userIdx, String status);

    Page<Walk> findByUserIdxAndStatusOrderByStartAtAsc(Integer userIdx, String status, Pageable pageable);

    @Query(value = "select day(startAt) as day, sum(goalRate) as rate from Walk " +
            "where userIdx=? and status = 'ACTIVE' and year(startAt)=? " +
            "and month(startAt)=? group by day(startAt)",
            nativeQuery = true
    )
    List<GetDayRateRes> getRateByUserIdxAndStartAt(int userIdx, int year, int month);

    @Query(value = "SELECT IFNULL(sum((timestampdiff(SECOND ,startAt, endAt))),0) AS monthTotalMin, " +
            "IFNULL(sum(distance),0) AS monthTotalDistance," +
            "IFNULL(sum(calorie),0) AS monthPerCal " +
            "FROM Walk " +
            "WHERE userIdx=:userIdx AND status = 'ACTIVE' AND YEAR(startAt)=:nowYear AND MONTH(startAt)=:nowMonth",
    nativeQuery = true)
    GetMonthTotalInterface getMonthTotalByQuery(@Param(value = "userIdx") int userIdx,
                                                @Param(value = "nowYear") int nowYear,
                                                @Param(value = "nowMonth") int nowMonth);

    @Query(value = "SELECT DAY(startAt) AS day, COUNT(walkIdx) AS walkCount FROM Walk " +
            "WHERE userIdx=:userIdx AND YEAR(startAt)=:yy AND MONTH(startAt)=:mm AND status='ACTIVE' GROUP BY DAY(startAt);",
    nativeQuery = true)
    List<GetFootprintCountInterface> getMonthFootCountByQuery(@Param(value = "userIdx") int userIdx,
                                                              @Param(value = "yy") int yy,
                                                              @Param(value = "mm") int mm);
                                                              
    @Query(value = "SELECT dayofweek(startAt) AS DAY FROM Walk " +
            "WHERE userIdx=:userIdx " +
            "AND status = 'ACTIVE' " +
            "AND MONTH(startAt) = MONTH(DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 MONTH));"
    ,nativeQuery = true)
    List<Integer> getDayOfWeekByQuery(@Param(value = "userIdx") int userIdx);

}