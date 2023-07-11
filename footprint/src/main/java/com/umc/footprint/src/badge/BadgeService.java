package com.umc.footprint.src.badge;

import com.umc.footprint.config.BaseException;
import com.umc.footprint.src.badge.model.Badge;
import com.umc.footprint.src.badge.model.BadgeDateInfo;
import com.umc.footprint.src.badge.model.BadgeOrder;
import com.umc.footprint.src.badge.model.BadgeRepository;
import com.umc.footprint.src.badge.model.UserBadge;
import com.umc.footprint.src.badge.model.UserBadgeRepository;
import com.umc.footprint.src.badge.model.vo.TotalDistanceStatus;
import com.umc.footprint.src.badge.model.vo.TotalRecordStatus;
import com.umc.footprint.src.goal.model.vo.GetGoalDays;
import com.umc.footprint.src.goal.repository.GoalDayRepository;
import com.umc.footprint.src.users.UserService;
import com.umc.footprint.src.users.model.GetUserBadges;
import com.umc.footprint.src.users.model.entity.User;
import com.umc.footprint.src.users.repository.UserRepository;
import com.umc.footprint.src.walks.repository.WalkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.umc.footprint.config.BaseResponseStatus.DATABASE_ERROR;
import static com.umc.footprint.config.BaseResponseStatus.INVALID_BADGEIDX;
import static com.umc.footprint.config.BaseResponseStatus.NOT_EXIST_USER_BADGE;
import static com.umc.footprint.config.BaseResponseStatus.NOT_EXIST_USER_IN_PREV_GOAL;
import static com.umc.footprint.config.BaseResponseStatus.NO_BADGE_USER;

@Service
@RequiredArgsConstructor
public class BadgeService {
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserRepository userRepository;
    private final GoalDayRepository goalDayRepository;
    private final UserService userService;
    private final WalkRepository walkRepository;

    public BadgeDateInfo getMonthlyBadgeStatus(String userId) throws BaseException {
        try {
            User user = userService.checkUserStatus(userId);

            int rate = userService.calcMonthGoalRate(user.getUserIdx(), 1); //이전달 달성률

            LocalDate now = LocalDate.now();
            int year = now.getYear();
            int month = now.getMonthValue();

            if (month == 1) { //지금이 1월이면 저번달은 작년 12월로 조회
                year--;
                month = 12;
            } else {
                month--;
            }

            GetGoalDays getGoalDays = new GetGoalDays(
                    goalDayRepository.selectOnlyGoalDayByQuery(
                            user.getUserIdx(),
                            year,
                            month
                    ).orElseThrow(() -> new BaseException(NOT_EXIST_USER_IN_PREV_GOAL))
            );

            double goalCount = 0; //저번달의 설정한 목표요일 전체 횟수
            Calendar cal = new GregorianCalendar(year, month - 1, 1); //0-11월로 조회
            do {
                int day = cal.get(Calendar.DAY_OF_WEEK);
                if (day == Calendar.SUNDAY && getGoalDays.getSun() == 1) {
                    goalCount++;
                } else if (day == Calendar.MONDAY && getGoalDays.getMon() == 1) {
                    goalCount++;
                } else if (day == Calendar.TUESDAY && getGoalDays.getTue() == 1) {
                    goalCount++;
                } else if (day == Calendar.WEDNESDAY && getGoalDays.getWed() == 1) {
                    goalCount++;
                } else if (day == Calendar.THURSDAY && getGoalDays.getThu() == 1) {
                    goalCount++;
                } else if (day == Calendar.FRIDAY && getGoalDays.getFri() == 1) {
                    goalCount++;
                } else if (day == Calendar.SATURDAY && getGoalDays.getSat() == 1) {
                    goalCount++;
                }
                cal.add(Calendar.DAY_OF_YEAR, 1);
            } while (cal.get(Calendar.MONTH) == month);

            // 이번달 산책 날짜의 요일을 받기
            List<Integer> walkDays = walkRepository.getDayOfWeekByQuery(user.getUserIdx());

            // 목표 요일이랑 비교하기
            // 산책 요일이 목표 요일이랑 같으면 count
            double walkCount = 0; //목표요일에 산책한 횟수

            for (int i = 0; i < walkDays.size(); i++) {
                int walkday = (int) walkDays.get(i);
                switch (walkday) {
                    case 1:
                        if (getGoalDays.getSun() == 1) {
                            walkCount++;
                        }
                        break;
                    case 2:
                        if (getGoalDays.getMon() == 1) {
                            walkCount++;
                        }
                        break;
                    case 3:
                        if (getGoalDays.getTue() == 1) {
                            walkCount++;
                        }
                        break;
                    case 4:
                        if (getGoalDays.getWed() == 1) {
                            walkCount++;
                        }
                        break;
                    case 5:
                        if (getGoalDays.getThu() == 1) {
                            walkCount++;
                        }
                        break;
                    case 6:
                        if (getGoalDays.getFri() == 1) {
                            walkCount++;
                        }
                        break;
                    case 7:
                        if (getGoalDays.getSat() == 1) {
                            walkCount++;
                        }
                        break;
                }
            }

            double walkRate = (walkCount / goalCount) * 100;

            /*
             * MASTER - 목표 요일 중 80% 이상 / 달성률 90%
             * PRO - 목표 요일 중 50% 이상 / 달성률 70%
             * LOVER - 목표 요일 고려 안함 / 달성률 50%
             * */
            int badgeNum = -1;

            if (rate >= 50) {
                //LOVER - badgeIdx 0
                badgeNum = 0;
            }
            if (rate >= 70 && walkRate >= 50) {
                //PRO - badgeIdx 1
                badgeNum = 1;
            }
            if (rate >= 90 && walkRate >= 80) {
                //MASTER - badgeIdx 2
                badgeNum = 2;
            }

            if (badgeNum == -1) { //이번 달에 획득한 뱃지가 없는 경우
                return null;
            }

            LocalDate badgeDate = LocalDate.of(year, month, badgeNum); // 획득한 뱃지의 date

            BadgeDateInfo badgeDateInfo = new BadgeDateInfo(badgeRepository.getByBadgeDate(badgeDate));


            //TODO: 사용자에게 다른 달 뱃지 있는지 확인 - 한 달에 프로, 러버, 마스터 중 하나만 ACTIVE한 상태여야 함!


            //UserBadge 테이블에 얻은 뱃지 추가
            userBadgeRepository.save(
                    UserBadge.builder()
                            .badgeIdx(badgeDateInfo.getBadgeInfo().getBadgeIdx())
                            .userIdx(user.getUserIdx())
                            .status("ACTIVE")
                            .build()
            );

            return badgeDateInfo;
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    public GetUserBadges getUserBadges(String userId) throws BaseException {
        try {
            User user = userService.checkUserStatus(userId);

            BadgeDateInfo badgeDateInfo = new BadgeDateInfo(badgeRepository.getByBadgeIdx(user.getBadgeIdx())); //대표 뱃지
            List<UserBadge> userBadges = userBadgeRepository.findAllByUserIdxAndStatus(user.getUserIdx(), "ACTIVE")
                    .orElseThrow(() -> new BaseException(NO_BADGE_USER));

            List<Badge> badges = badgeRepository.findByBadgeIdx(
                    userBadges.stream().map(UserBadge::getBadgeIdx).collect(Collectors.toList())
            );

            List<BadgeOrder> badgeOrders = new ArrayList<>();

            for (Badge b : badges) {
                int badgeOrderNum = 0;
                if (b.getBadgeDate().toString().startsWith("1900")) {
                    if (b.getBadgeIdx() == 0) continue;
                    badgeOrderNum = b.getBadgeIdx() - 1;
                } else {
                    LocalDate badgeDate = b.getBadgeDate();
                    if (LocalDate.now().getYear() != badgeDate.getYear()) continue; //올해 뱃지가 아니면 조회 X
                    badgeOrderNum = badgeDate.getMonthValue() + 7;
                }
                badgeOrders.add(
                        BadgeOrder.builder()
                                .badgeIdx(b.getBadgeIdx())
                                .badgeName(b.getBadgeName())
                                .badgeUrl(b.getBadgeUrl())
                                .badgeDate(b.getBadgeDate().toString())
                                .badgeOrder(badgeOrderNum)
                                .build()
                );
            }
            return GetUserBadges.builder()
                    .repBadgeDateInfo(badgeDateInfo)
                    .badgeList(badgeOrders)
                    .build();
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    @Transactional(propagation = Propagation.NESTED, rollbackFor = Exception.class)
    public BadgeDateInfo modifyRepBadge(String userId, int badgeIdx) throws BaseException {
        try {
            User user = userService.checkUserStatus(userId);

            // 해당 뱃지가 Badge 테이블에 존재하는 뱃지인지?
            if (!badgeRepository.existsByBadgeIdx(badgeIdx)) {
                throw new BaseException(INVALID_BADGEIDX);
            }

            // 유저가 해당 뱃지를 갖고 있고, ACTIVE 뱃지인지?
            if (userBadgeRepository.checkUserHasBadge(user.getUserIdx(), badgeIdx) == 0) {
                throw new BaseException(NOT_EXIST_USER_BADGE);
            }

            user.setBadgeIdx(badgeIdx);
            userRepository.save(user);

            return new BadgeDateInfo(badgeRepository.getByBadgeIdx(user.getBadgeIdx()));
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    @Transactional
    public List<Integer> getAcquiredBadgeIdxList(int userIdx, Double totalDistance, Integer totalWalkCount) {
        // 현재 누적 거리, 횟수로 얻을 수 있는 뱃지 계산 - 획득한 뱃지가 없을 때 어떤 동작을 해야 하지?
        TotalDistanceStatus distanceStatus = TotalDistanceStatus.getDistanceBadge(totalDistance).orElseThrow();
        TotalRecordStatus recordStatus = TotalRecordStatus.getWalkCountBadge(totalWalkCount).orElseThrow();

        List<Integer> acquiredBadgeIdxList = new ArrayList<>();

        // 현재 갖고 있는 뱃지 조회
        List<Integer> beforeSavingWalkBadgeList = userBadgeRepository.findAllByUserIdxAndStatus(userIdx, "ACTIVE")
                .map(userBadgeList -> userBadgeList.stream()
                        .map(UserBadge::getBadgeIdx)
                        .sorted()
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());

        if (beforeSavingWalkBadgeList.isEmpty()) { // 발자국 스타터 부여
            acquiredBadgeIdxList.add(1);
        }

        int originMaxDistanceBadgeIdx = 1; // 원래 갖고 있던 뱃지(2~5)의 가장 큰 값
        int originMaxRecordBadgeIdx = 1; // 원래 갖고 있던 뱃지(6~8)의 가장 큰 값

        for (Integer originBadgeIdx : beforeSavingWalkBadgeList) {
            if (originBadgeIdx >= TotalDistanceStatus.minBadgeIdx() && originBadgeIdx <= TotalDistanceStatus.maxBadgeIdx()) {
                originMaxDistanceBadgeIdx = originBadgeIdx;
            }
            if (originBadgeIdx >= TotalRecordStatus.minBadgeIdx() && originBadgeIdx <= TotalRecordStatus.maxBadgeIdx()) {
                originMaxRecordBadgeIdx = originBadgeIdx;
            }
        }

        // 기록 관련 뱃지 저장 - 6~8
        if(distanceStatus.getBadgeIdx() > originMaxDistanceBadgeIdx) {
            for (int i = originMaxDistanceBadgeIdx + 1; i <= distanceStatus.getBadgeIdx(); i++) {
                acquiredBadgeIdxList.add(i);
            }
        }

        //거리 뱃지 확인 로직 - 2~5
        if(recordStatus.getBadgeIdx() > originMaxRecordBadgeIdx) {
            for (int i = originMaxRecordBadgeIdx + 1; i <= recordStatus.getBadgeIdx(); i++) {
                acquiredBadgeIdxList.add(i);
            }
        }

        return acquiredBadgeIdxList;
    }
}
