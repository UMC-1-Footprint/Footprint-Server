package com.umc.footprint.src.walks;

import com.umc.footprint.src.walks.model.*;

import lombok.extern.slf4j.Slf4j;
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
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

@Slf4j
@Repository
public class WalkDao {
    private JdbcTemplate jdbcTemplate;



    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

//    public GetWalkTime getWalkTime(int walkIdx) {
//        String getTimeQuery = "select date_format(date(startAt), '%Y.%m.%d') as date, \n" +
//                "       date_format(time(startAt),'%H:%i') as startAt,\n" +
//                "       date_format(time(endAt),'%H:%i') as endAt, \n" +
//                "       (timestampdiff(second, startAt, endAt)) as timeString from Walk where walkIdx=? and status='ACTIVE';";
//        GetWalkTime getWalkTime = this.jdbcTemplate.queryForObject(getTimeQuery,
//                (rs, rowNum) -> new GetWalkTime(
//                        rs.getString("date"),
//                        rs.getString("startAt"),
//                        rs.getString("endAt"),
//                        rs.getString("timeString")
//                ),walkIdx);
//
//        getWalkTime.convTimeString();
//
//        return getWalkTime;
//    }

    public Integer getFootCount(int walkIdx) {
        String getFootCountQuery = "select count(footprintIdx) as footCount from Footprint where walkIdx=? and status='ACTIVE';";
        Integer footCount = this.jdbcTemplate.queryForObject(getFootCountQuery,
                (rs, rowNum) -> rs.getInt("footCount"), walkIdx);
        return footCount;
    }

    public String deleteWalk(int walkIdx) {
        String deleteFootprintQuery = "update Footprint set status='INACTIVE' where walkIdx=? and status='ACTIVE';"; // ????????? INACTIVE
        this.jdbcTemplate.update(deleteFootprintQuery, walkIdx);

        String deleteWalkQuery = "update Walk set status='INACTIVE' where walkIdx=? and status='ACTIVE';"; // ?????? INACTIVE
        this.jdbcTemplate.update(deleteWalkQuery, walkIdx);

        return "Success Delete walk record!";
    }


        //Photo ???????????? insert
    public void addPhoto(int userIdx, List<SaveFootprint> footprintList) {
        String photoInsertQuery = "insert into `Photo`(`imageUrl`, `userIdx`, `footprintIdx`) values (?,?,?)";

        for (SaveFootprint footprint : footprintList) {
            this.jdbcTemplate.batchUpdate(photoInsertQuery,
                    new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setString(1, footprint.getPhotos().get(i));
                        ps.setInt(2, userIdx);
                        ps.setInt(3, footprint.getFootprintIdx());
                    }

                    @Override
                    public int getBatchSize() {
                        return footprint.getPhotos().size();
                    }
            });
        }

    }

    public List<Pair<Integer, Integer>> addHashtag(List<SaveFootprint> footprintList) {
        String hashtagInsertQuery = "insert into Hashtag(hashtag) values (?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        //Pair<hashtagIdx, footprintIdx> mapping (tag) idx list
        List<Pair<Integer, Integer>> tagIdxList = new ArrayList<>();

        // footprint??? hashtag list ??????
        for (SaveFootprint footprint : footprintList) {
            if (footprint.getHashtagList().size() != 0){
                for (String hashtag : footprint.getHashtagList()) {
                    this.jdbcTemplate.update(new PreparedStatementCreator() {
                        @Override
                        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                            PreparedStatement preparedStatement = con.prepareStatement(hashtagInsertQuery, Statement.RETURN_GENERATED_KEYS);
                            preparedStatement.setString(1, hashtag);
                            return preparedStatement;
                        }
                    }, keyHolder);
                    // tag list??? ??????
                    tagIdxList.add(Pair.of(keyHolder.getKey().intValue(), footprint.getFootprintIdx()));
                }
            }
            log.debug("????????? ???????????????: {}" + footprint.getHashtagList());
        }

        log.debug("tag ????????????: {}", tagIdxList);
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

    // ????????? ?????? ?????? ???????????? ??????
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

    // ????????? ?????? ?????? ??????
    public Long getWalkGoalTime(int userIdx) {
        log.debug("userIdx: {}", userIdx);
        String getTimeQuery = "select walkGoalTime from Goal where userIdx = ? and MONTH(createAt) = MONTH(NOW())";
        int getTimeParam = userIdx;
        return this.jdbcTemplate.queryForObject(getTimeQuery, Long.class, getTimeParam);
    }

    public GetBadgeIdx getAcquiredBadgeIdxList(int userIdx) {
        // ??????, ?????? ?????? ??????

        String getDisRecBadgeQuery = "SELECT \n" +
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
                "Where userIdx = ?\n and status = 'ACTIVE'" +
                "group by Walk.userIdx";

        GetBadgeIdx getBadgeIdx = this.jdbcTemplate.queryForObject(getDisRecBadgeQuery,
                (rs, rowNum) -> GetBadgeIdx.builder()
                        .distanceBadgeIdx(rs.getInt("distanceBadgeIdx"))
                        .recordBadgeIdx(rs.getInt("recordBadgeIdx"))
                        .build()
                , userIdx);
        log.debug("????????? ?????? ?????? ?????? ????????? ?????? ??? ?????????: {}", getBadgeIdx.getRecordBadgeIdx());
        log.debug("????????? ?????? ?????? ?????? ????????? ?????? ??? ?????????: {}", getBadgeIdx.getDistanceBadgeIdx());
        return getBadgeIdx;
    }

    // ?????? ????????? ?????? ?????? ??????
    public List<Integer> getOriginBadgeIdxList(int userIdx) {
        String getBadgeIdxListQuery = "select badgeIdx from UserBadge where userIdx = ?";

        return this.jdbcTemplate.queryForList(getBadgeIdxListQuery, int.class, userIdx);
    }

    // ?????? ?????? ??????
    public List<PostWalkRes> getBadgeInfo(List<Integer> badgeIdxList) {
        String getBadgeInfoQuery = "select badgeIdx, badgeName, badgeUrl from Badge where badgeIdx = ?";
        List<PostWalkRes> postWalkResList = new ArrayList<PostWalkRes>();
        for (Integer badgeIdx : badgeIdxList) {
            postWalkResList.add(this.jdbcTemplate.queryForObject(getBadgeInfoQuery,
                            (rs, rowNum) -> PostWalkRes.builder()
                                    .badgeIdx(rs.getInt("badgeIdx"))
                                    .badgeName(rs.getString("badgeName"))
                                    .badgeUrl(rs.getString("badgeUrl"))
                                    .build()
                    , badgeIdx));
        }

        return postWalkResList;
    }

    // ????????? ?????? ?????? ??????
    public int checkFirstWalk(int userIdx) {
        String checkFirstWalkQuery = "select count(walkIdx) from Walk where userIdx = ?";
        return this.jdbcTemplate.queryForObject(checkFirstWalkQuery, int.class, userIdx);
    }

    public int getWalkWholeIdx(int walkIdx, int userIdx) {
        String getWalkWholeIdxQuery = "select walkIdx from Walk where userIdx = ? and status = 'ACTIVE' ORDER BY startAt ASC LIMIT ?,1";
        return this.jdbcTemplate.queryForObject(getWalkWholeIdxQuery, int.class, userIdx, walkIdx-1);
    }

    public int checkWalkVal(int walkIdx) {
        log.debug("WalkDao.checkWalkVal");
        String checkWalkValQuery = "select EXISTS (select walkIdx from Walk where walkIdx=? and status='ACTIVE') as success;";
        return this.jdbcTemplate.queryForObject(checkWalkValQuery, int.class, walkIdx);
    }

    public List<Integer> getFootprintIdxList(int walkIdx) {
        String getFootprintQuery = "select footprintIdx from Footprint where walkIdx=?;";
        List<Integer> footprintIdxList = jdbcTemplate.queryForList(getFootprintQuery, int.class, walkIdx);
        return footprintIdxList;
    }

    // ?????? ???????????? ?????? inactive
    public void inactivePhoto(int footprintIdx) {
        String inactivePhotoQuery = "update Photo set status='INACTIVE' where footprintIdx=? and status='ACTIVE';"; // ?????? INACTIVE
        this.jdbcTemplate.update(inactivePhotoQuery, footprintIdx);
    }

    // ?????? ???????????? ?????? inactive
    public void inactiveTag(int footprintIdx) {
        String inactiveTagQuery = "update Tag set status='INACTIVE' where footprintIdx=? and status='ACTIVE';"; // ?????? INACTIVE
        this.jdbcTemplate.update(inactiveTagQuery, footprintIdx);
    }
}
