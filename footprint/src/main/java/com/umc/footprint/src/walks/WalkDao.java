package com.umc.footprint.src.walks;

import com.umc.footprint.src.walks.model.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class WalkDao {
    private JdbcTemplate jdbcTemplate;



    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }


    public GetWalkInfo getWalkInfo(int walkIdx) {
        String getTimeQuery = "select date_format(date(startAt), '%Y.%m.%d') as date, \n" +
                "       date_format(time(startAt),'%H:%i') as startAt,\n" +
                "       date_format(time(endAt),'%H:%i') as endAt, \n" +
                "       (timestampdiff(second, startAt, endAt)) as timeString from Walk where walkIdx=?;";
        GetWalkTime getWalkTime = this.jdbcTemplate.queryForObject(getTimeQuery,
                (rs, rowNum) -> new GetWalkTime(
                        rs.getString("date"),
                        rs.getString("startAt"),
                        rs.getString("endAt"),
                        rs.getString("timeString")
                ),walkIdx);

        getWalkTime.convTimeString();

        String getFootCountQuery = "select count(footprintIdx) as footCount from Footprint where walkIdx=? and status='ACTIVE';";
        Integer footCount = this.jdbcTemplate.queryForObject(getFootCountQuery,
                (rs, rowNum) -> rs.getInt("footCount"), walkIdx);


        String getWalkInfoQuery = "select walkIdx, calorie, distance, pathImageUrl from Walk where walkIdx=?;";
        GetWalkInfo getWalkInfo = this.jdbcTemplate.queryForObject(getWalkInfoQuery,
                (rs,rowNum) -> new GetWalkInfo(
                        rs.getInt("walkIdx"),
                        getWalkTime,
                        rs.getInt("calorie"),
                        rs.getDouble("distance"),
                        footCount,
                        rs.getString("pathImageUrl")), walkIdx);

        return getWalkInfo;
    }

    public String deleteWalk(int walkIdx) {
        String deleteWalkQuery = "update Footprint set status='INACTIVE' where walkIdx=? and status='ACTIVE';"; // 실행될 동적 쿼리문
        this.jdbcTemplate.update(deleteWalkQuery, walkIdx);
        //String checkDeleteQuery = "select count(footprintIdx) as footCount from footprint where walkIdx=? and status='ACTIVE';"; // 전체 삭제 확인
        //Integer footCount = this.jdbcTemplate.queryForObject(checkDeleteQuery,
        //        (rs, rowNum) -> rs.getInt("footCount"), walkIdx);

        return "Success Delete walk record!";
    }
  
    //Walk 테이블에 insert
    public int addWalk(SaveWalk walk, String pathImgUrl) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        String walkInsertQuery = "insert into Walk(startAt, endAt, distance, coordinate, pathImageUrl, userIdx, goalRate, calorie) " +
                "values (?,?,?,ST_GeomFromText(?),?,?,?,?)";

        System.out.println("walk.getStartAt() = " + walk.getStartAt());
        System.out.println("walk.getEndAt() = " + walk.getEndAt());
        System.out.println("walk.getDistance() = " + walk.getDistance());
        System.out.println("walk.getUserIdx() = " + walk.getUserIdx());
        System.out.println("walk.getStr_coordinates() = " + walk.getStr_coordinates());
        System.out.println("pathImgUrl = " + pathImgUrl);
        System.out.println("walk.getUserIdx() = " + walk.getUserIdx());
        System.out.println("walk.getGoalRate() = " + walk.getGoalRate());
        System.out.println("walk.getPhotoMatchNumList() = " + walk.getPhotoMatchNumList());
        System.out.println("walk.getCalorie() = " + walk.getCalorie());

        this.jdbcTemplate.update(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                PreparedStatement preparedStatement = con.prepareStatement(walkInsertQuery, Statement.RETURN_GENERATED_KEYS);
                preparedStatement.setTimestamp(1, Timestamp.valueOf(walk.getStartAt()));
                preparedStatement.setTimestamp(2, Timestamp.valueOf(walk.getEndAt()));
                preparedStatement.setDouble(3, walk.getDistance());
                preparedStatement.setString(4, walk.getStr_coordinates());
                preparedStatement.setString(5, pathImgUrl);
                preparedStatement.setInt(6, walk.getUserIdx());
                preparedStatement.setFloat(7, walk.getGoalRate());
                preparedStatement.setInt(8, walk.getCalorie());

                return preparedStatement;
            }
        }, keyHolder);

        // 생성된 id값 int형으로 변환해서 반환
        return keyHolder.getKey().intValue();
    }

    public void addFootprint(List<SaveFootprint> footprintList, int walkIdx) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        String footprintInsertQuery = "insert into `Footprint`(`coordinate`, `write`, `recordAt`, `walkIdx`, `updateAt`)" +
                "values (ST_GeomFromText(?),?,?,?,?)";

        System.out.println("footprintList.get(i).getStrCoordinate() = " + footprintList.get(0).getStrCoordinate());
        System.out.println("footprintList.get(i).getWrite() = " + footprintList.get(0).getWrite());
        System.out.println("footprintList.get(i).getRecordAt() = " + footprintList.get(0).getRecordAt());
        System.out.println("footprintList.get(i).getWalkIdx() = " + walkIdx);

        for (SaveFootprint footprint : footprintList){
            this.jdbcTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                    PreparedStatement preparedStatement = con.prepareStatement(footprintInsertQuery, Statement.RETURN_GENERATED_KEYS);
                    preparedStatement.setString(1, footprint.getStrCoordinate());
                    preparedStatement.setString(2, footprint.getWrite());
                    preparedStatement.setTimestamp(3, Timestamp.valueOf(footprint.getRecordAt()));
                    preparedStatement.setInt(4, walkIdx);
                    preparedStatement.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
                    return preparedStatement;
                }
            }, keyHolder);
            // 자동 생성되는 인덱스 리스트에 추가
            footprint.setFootprintIdx(keyHolder.getKey().intValue());

            System.out.println("footprint.getFootprintIdx() = " + footprint.getFootprintIdx());
            System.out.println("footprint.getWrite() = " + footprint.getWrite());
            System.out.println("footprint.getStrCoordinate() = " + footprint.getStrCoordinate());
            System.out.println("footprint.getWalkIdx() = " + footprint.getWalkIdx());
            System.out.println("footprint.getRecordAt() = " + footprint.getRecordAt());
        }
    }

        //Photo 테이블에 insert
    public void addPhoto(int userIdx, List<SaveFootprint> footprintList) {
        String photoInsertQuery = "insert into `Photo`(`imageUrl`, `userIdx`, `footprintIdx`) values (?,?,?)";

        for (SaveFootprint footprint : footprintList) {
            this.jdbcTemplate.batchUpdate(photoInsertQuery,
                    new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setString(1, footprint.getImgUrlList().get(i));
                        ps.setInt(2, userIdx);
                        ps.setInt(3, footprint.getFootprintIdx());
                    }

                    @Override
                    public int getBatchSize() {
                        return footprint.getImgUrlList().size();
                    }
            });
        }

    }

    public List<Pair<Integer, Integer>> addHashtag(List<SaveFootprint> footprintList) {
        String hashtagInsertQuery = "insert into Hashtag(hashtag) values (?)";
        System.out.println("footprintList.get(0).getHashtagList() = " + footprintList.get(0).getHashtagList());

        KeyHolder keyHolder = new GeneratedKeyHolder();

        //Pair<hashtagIdx, footprintIdx> mapping (tag) idx list
        List<Pair<Integer, Integer>> tagIdxList = new ArrayList<Pair<Integer, Integer>>();

        // footprint당 hashtag list 삽입
        for (SaveFootprint f : footprintList) {
            if (f.getHashtagList().size() != 0){
                for (String hashtag : f.getHashtagList()) {
                    this.jdbcTemplate.update(new PreparedStatementCreator() {
                        @Override
                        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                            PreparedStatement preparedStatement = con.prepareStatement(hashtagInsertQuery, Statement.RETURN_GENERATED_KEYS);
                            preparedStatement.setString(1, hashtag);
                            return preparedStatement;
                        }
                    }, keyHolder);
                }
                // tag list에 삽입
                tagIdxList.add(Pair.of(keyHolder.getKey().intValue(), f.getFootprintIdx()));
            }
        }

        return tagIdxList;
    }

    public void addTag(List<Pair<Integer, Integer>> tagIdxList, int userIdx) {
        String tagInsertQuery = "insert into Tag(hashtagIdx, footprintIdx, userIdx) values (?,?,?)";

        this.jdbcTemplate.batchUpdate(tagInsertQuery,
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setInt(1, tagIdxList.get(i).getFirst());
                        ps.setInt(2, tagIdxList.get(i).getSecond());
                        ps.setInt(3, userIdx);
                    }

                    @Override
                    public int getBatchSize() {
                        return tagIdxList.size();
                    }
                });

    }

    // 획득한 뱃지 매핑 테이블에 삽입
    public void addUserBadge(List<Integer> acquiredBadgeIdxList, int userIdx) {
        String userBadgeInsertQuery = "insert into UserBadge(userIdx, badgeIdx) values (?,?)";

        this.jdbcTemplate.batchUpdate(userBadgeInsertQuery,
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setInt(1, userIdx);
                        ps.setInt(2, acquiredBadgeIdxList.get(i));
                    }

                    @Override
                    public int getBatchSize() {
                        return acquiredBadgeIdxList.size();
                    }
                });
    }

    // 유저의 목표 시간 반환
    public int getWalkGoalTime(int userIdx) {
        String getTimeQuery = "select walkGoalTime from Goal where userIdx = ? and MONTH(createAt) = MONTH(NOW())";
        int getTimeParam = userIdx;
        return this.jdbcTemplate.queryForObject(getTimeQuery, int.class, getTimeParam);
    }

    public GetBadgeIdx getAcquiredBadgeIdxList(int userIdx) {
        // 거리, 기록 관련 쿼리

        System.out.println("WalkDao.getAcquiredBadgeIdxList");
        String getDisRecBadgeQuery = "SELECT \n" +
                "       CASE\n" +
                "            WHEN (sum(Walk.distance) between 10 and 30) then 2\n" +
                "            when (sum(Walk.distance) between 30 and 50) then 3\n" +
                "            WHEN (sum(Walk.distance) between 50 and 100) then 4\n" +
                "            WHEN (sum(Walk.distance) > 100) then 5\n" +
                "        else 0\n" +
                "        end as distanceBadgeIdx,\n" +
                "       CASE\n" +
                "            when (count(Walk.walkIdx) between 10 and 30) then 6\n" +
                "            when (count(Walk.walkIdx) between 30 and 50) then 7\n" +
                "            when (count(Walk.walkIdx) > 50) then 8\n" +
                "        else 0\n" +
                "        end as recordBadgeIdx\n" +
                "From Walk\n" +
                "Where userIdx = ?\n" +
                "group by Walk.userIdx";

        System.out.println("before query");
        GetBadgeIdx getBadgeIdx = this.jdbcTemplate.queryForObject(getDisRecBadgeQuery,
                (rs, rowNum) -> GetBadgeIdx.builder()
                        .distanceBadgeIdx(rs.getInt("distanceBadgeIdx"))
                        .recordBadgeIdx(rs.getInt("recordBadgeIdx"))
                        .build()
                , userIdx);
        System.out.println("getBadgeIdx.getRecordBadgeIdx() = " + getBadgeIdx.getRecordBadgeIdx());
        System.out.println("getBadgeIdx.getDistanceBadgeIdx() = " + getBadgeIdx.getDistanceBadgeIdx());
        return getBadgeIdx;
    }

    // 원래 가지고 있던 뱃지 조회
    public List<Integer> getOriginBadgeIdxList(int userIdx) {
        String getBadgeIdxListQuery = "select badgeIdx from UserBadge where userIdx = ?";

        return this.jdbcTemplate.queryForList(getBadgeIdxListQuery, int.class, userIdx);
    }

    // 뱃지 정보 조회
    public List<PostWalkRes> getBadgeInfo(List<Integer> badgeIdxList) {
        String getBadgeInfoQuery = "select badgeIdx, badgeName, badgeUrl from Badge where badgeIdx = ?";
        List<PostWalkRes> postWalkResList = new ArrayList<PostWalkRes>();
        for (Integer badgeIdx : badgeIdxList) {
            postWalkResList.add(this.jdbcTemplate.queryForObject(getBadgeInfoQuery,
                    (rs, rowNum) -> new PostWalkRes(
                            rs.getInt("badgeIdx"),
                            rs.getString("badgeName"),
                            rs.getString("badgeUrl")
                    ), badgeIdx));
        }

        return postWalkResList;
    }
}
