package com.umc.footprint.src.goal;

import static com.umc.footprint.config.BaseResponseStatus.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.umc.footprint.config.BaseException;
import com.umc.footprint.config.BaseResponseStatus;
import com.umc.footprint.src.goal.model.dto.GetUserGoalRes;
import com.umc.footprint.src.goal.model.dto.PatchUserGoalReq;
import com.umc.footprint.src.goal.model.entity.Goal;
import com.umc.footprint.src.goal.model.entity.GoalDay;
import com.umc.footprint.src.goal.model.entity.GoalDayNext;
import com.umc.footprint.src.goal.model.entity.GoalNext;
import com.umc.footprint.src.goal.model.vo.GetGoalDays;
import com.umc.footprint.src.goal.model.vo.UserGoalTime;
import com.umc.footprint.src.goal.repository.GoalDayNextRepository;
import com.umc.footprint.src.goal.repository.GoalDayRepository;
import com.umc.footprint.src.goal.repository.GoalNextRepository;
import com.umc.footprint.src.goal.repository.GoalRepository;
import com.umc.footprint.src.walks.model.vo.WalkInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@EnableScheduling
@Configuration
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final GoalNextRepository goalNextRepository;
    private final GoalDayRepository goalDayRepository;
    private final GoalDayNextRepository goalDayNextRepository;

    @Transactional(propagation = Propagation.NESTED, rollbackFor = Exception.class)
    public void modifyGoalJPA(int userIdx, PatchUserGoalReq patchUserGoalReq) throws BaseException {
        try{

            /** 1. UPDATE Goal  */
            Optional<GoalNext> userGoal = goalNextRepository.findByUserIdx(userIdx);

            userGoal.get().setWalkGoalTime(patchUserGoalReq.getWalkGoalTime());
            userGoal.get().setWalkTimeSlot(patchUserGoalReq.getWalkTimeSlot());
            goalNextRepository.save(userGoal.get());

            /** 2. UPDATE GoalDay  */
            Optional<GoalDayNext> userGoalDay = goalDayNextRepository.findByUserIdx(userIdx);

            List<Integer> newDayIdxList = new ArrayList<>(List.of(0,0,0,0,0,0,0));
            List<Integer> dayIdxList = patchUserGoalReq.getDayIdx();

            for(Integer dayIdx : dayIdxList) {
                newDayIdxList.set(dayIdx-1,1);
            }

            userGoalDay.get().setMon(newDayIdxList.get(0));
            userGoalDay.get().setTue(newDayIdxList.get(1));
            userGoalDay.get().setWed(newDayIdxList.get(2));
            userGoalDay.get().setThu(newDayIdxList.get(3));
            userGoalDay.get().setFri(newDayIdxList.get(4));
            userGoalDay.get().setSat(newDayIdxList.get(5));
            userGoalDay.get().setSun(newDayIdxList.get(6));
            
            goalDayNextRepository.save(userGoalDay.get());

        } catch(Exception exception){
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw new BaseException(BaseResponseStatus.DATABASE_ERROR);
        }
    }

    public GetUserGoalRes getUserGoal(int userIdx) throws BaseException{

        /** 1. 이번달 정보 구하기 */
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        String month = today.format(DateTimeFormatter.ofPattern("yyyy년 MM월"));

        /** 2. get UserGoalDay -> dayIdxList */
        List<GoalDay> userGoalDayList = goalDayRepository.findByUserIdx(userIdx);
        GoalDay userGoalDay = GoalDay.builder().build();
        for(GoalDay goalDay : userGoalDayList ){
            LocalDate goalDayCreateAt = goalDay.getCreateAt().toLocalDate();
            if(goalDayCreateAt.getMonth().equals(LocalDate.now().getMonth()) && goalDayCreateAt.getYear() == LocalDate.now().getYear()){
                userGoalDay = goalDay;
                break;
            }
        }

        List<Integer> dayIdxList = new ArrayList<>();
        if(userGoalDay.getMon().equals(1))
            dayIdxList.add(1);
        if(userGoalDay.getTue().equals(1))
            dayIdxList.add(2);
        if(userGoalDay.getWed().equals(1))
            dayIdxList.add(3);
        if(userGoalDay.getThu().equals(1))
            dayIdxList.add(4);
        if(userGoalDay.getFri().equals(1))
            dayIdxList.add(5);
        if(userGoalDay.getSat().equals(1))
            dayIdxList.add(6);
        if(userGoalDay.getSun().equals(1))
            dayIdxList.add(7);

        /** 3. get UserGoalTime */
        List<Goal> userGoalList = goalRepository.findAllByUserIdx(userIdx);

        Goal userGoal = Goal.builder().build();
        for(Goal goal : userGoalList ){
            LocalDate goalCreateAt = goal.getCreateAt().toLocalDate();
            if(goalCreateAt.getMonth().equals(LocalDate.now().getMonth()) && goalCreateAt.getYear() == LocalDate.now().getYear()){
                userGoal = goal;
                break;
            }
        }

        UserGoalTime userGoalTime = UserGoalTime.builder()
                .walkGoalTime(userGoal.getWalkGoalTime())
                .walkTimeSlot(userGoal.getWalkTimeSlot())
                .build();

        /** 4. Check GoalNext Modified */
        boolean goalNextModified;
        Optional<GoalNext> userGoalNext = goalNextRepository.findByUserIdx(userIdx);

        LocalDateTime goalNextCreateAt = userGoalNext.get().getCreateAt();
        LocalDateTime goalNextUpdateAt = userGoalNext.get().getUpdateAt();

        if(today.getYear() == goalNextCreateAt.getYear() && today.getMonth() == goalNextCreateAt.getMonth()){
            System.out.println("goalNextCreateAt.equals(goalNextUpdateAt) = " + goalNextCreateAt.equals(goalNextUpdateAt));
            if(goalNextCreateAt.equals(goalNextUpdateAt)){
                goalNextModified = false;
            } else {
                goalNextModified = true;
            }

        } else{
            if(today.getYear() == goalNextUpdateAt.getYear() && today.getMonth() == goalNextUpdateAt.getMonth()){
                goalNextModified = true;
            } else {
                goalNextModified = false;
            }

        }

        return GetUserGoalRes.builder()
                .month(month)
                .dayIdx(dayIdxList)
                .userGoalTime(userGoalTime)
                .goalNextModified(goalNextModified)
                .build();


    }

    public GetUserGoalRes getUserGoalNext(int userIdx) throws BaseException{

        /** 1. 이번달 정보 구하기 */
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        String month = today.plusMonths(1).format(DateTimeFormatter.ofPattern("yyyy년 MM월"));

        /** 2. get UserGoalDay -> dayIdxList */
        Optional<GoalDayNext> userGoalDayNext = goalDayNextRepository.findByUserIdx(userIdx);

        List<Integer> dayIdxList = new ArrayList<>();
        if(userGoalDayNext.get().getMon().equals(1))
            dayIdxList.add(1);
        if(userGoalDayNext.get().getTue().equals(1))
            dayIdxList.add(2);
        if(userGoalDayNext.get().getWed().equals(1))
            dayIdxList.add(3);
        if(userGoalDayNext.get().getThu().equals(1))
            dayIdxList.add(4);
        if(userGoalDayNext.get().getFri().equals(1))
            dayIdxList.add(5);
        if(userGoalDayNext.get().getSat().equals(1))
            dayIdxList.add(6);
        if(userGoalDayNext.get().getSun().equals(1))
            dayIdxList.add(7);

        /** 3. get UserGoalTime */
        Optional<GoalNext> userGoalNext = goalNextRepository.findByUserIdx(userIdx);
        UserGoalTime userGoalTime = UserGoalTime.builder()
                .walkGoalTime(userGoalNext.get().getWalkGoalTime())
                .walkTimeSlot(userGoalNext.get().getWalkTimeSlot())
                .build();

        /** 4. Check GoalNext Modified */
        boolean goalNextModified;

        LocalDateTime goalNextCreateAt = userGoalNext.get().getCreateAt();
        LocalDateTime goalNextUpdateAt = userGoalNext.get().getUpdateAt();

        if(today.getYear() == goalNextCreateAt.getYear() && today.getMonth() == goalNextCreateAt.getMonth()){
            System.out.println("goalNextCreateAt.equals(goalNextUpdateAt) = " + goalNextCreateAt.equals(goalNextUpdateAt));
            if(goalNextCreateAt.equals(goalNextUpdateAt)){
                goalNextModified = false;
            } else {
                goalNextModified = true;
            }

        } else{
            if(today.getYear() == goalNextUpdateAt.getYear() && today.getMonth() == goalNextUpdateAt.getMonth()){
                goalNextModified = true;
            } else {
                goalNextModified = false;
            }

        }

        return GetUserGoalRes.builder()
                .month(month)
                .dayIdx(dayIdxList)
                .userGoalTime(userGoalTime)
                .goalNextModified(goalNextModified)
                .build();
    }

    // UserSchedule
    // 유저 목표 최신화
    // GoalNext -> Goal
    public void updateGoal(){

        // 1. GoalNext 테이블 전체 리스트 추출
        List<GoalNext> entireGoalNext = goalNextRepository.findAll();

        // 2. Goal 테이블에 1번 추출 결과물 삽입
        for( GoalNext goalNext : entireGoalNext){
            goalRepository.save(Goal.builder()
                    .walkGoalTime(goalNext.getWalkGoalTime())
                    .walkTimeSlot(goalNext.getWalkTimeSlot())
                    .userIdx(goalNext.getUserIdx())
                    .build());
        }

    }

    // UserSchedule
    // 유저 목표 요일 최신화
    // GoalDayNext -> GoalDay
    public void updateGoalDay(){

        // 1. GoalDayNext 테이블 전체 리스트 추출
        List<GoalDayNext> entireGoalDayNext = goalDayNextRepository.findAll();

        // 2. GoalDay 테이블에 1번 추출 결과물 삽입
        for( GoalDayNext goalDayNext : entireGoalDayNext){
            goalDayRepository.save(GoalDay.builder()
                    .mon(goalDayNext.getMon())
                    .tue(goalDayNext.getTue())
                    .wed(goalDayNext.getWed())
                    .thu(goalDayNext.getThu())
                    .fri(goalDayNext.getFri())
                    .sat(goalDayNext.getSat())
                    .sun(goalDayNext.getSun())
                    .userIdx(goalDayNext.getUserIdx())
                    .build());
        }
    }

    public List<String> getUserGoalDays(int userIdx, int year, int month) throws BaseException {
        GetGoalDays goalDay = new GetGoalDays(goalDayRepository.selectOnlyGoalDayByQuery(userIdx, year, month)
                .orElseThrow(()->new BaseException(NOT_EXIST_USER_IN_GOAL)));

        return convertGoalDayBoolToString(goalDay);
    }

    public List<String> convertGoalDayBoolToString(GetGoalDays goalDay) {
        List<String> goalDayString = new ArrayList<>();

        if(goalDay.getSun()==1) {
            goalDayString.add("SUN");
        }
        if(goalDay.getMon()==1) {
            goalDayString.add("MON");
        }
        if(goalDay.getTue()==1) {
            goalDayString.add("TUE");
        }
        if(goalDay.getWed()==1) {
            goalDayString.add("WED");
        }
        if(goalDay.getThu()==1) {
            goalDayString.add("THU");
        }
        if(goalDay.getFri()==1) {
            goalDayString.add("FRI");
        }
        if(goalDay.getSat()==1) {
            goalDayString.add("SAT");
        }
        return goalDayString;
    }

    @Transactional
    @Scheduled(cron = "0 0 0 1 * ?")
    public void changeMonthGoal() {
        updateGoal();
        updateGoalDay();
    }

    @Transactional(readOnly = true)
    public Double getGoalRate(WalkInfo walk, int userIdx) {
        Long walkTime = Duration.between(walk.getStartAt(), walk.getEndAt()).getSeconds();
        log.debug("walkTime: {}", walkTime);

        List<Goal> userGoals = goalRepository.findAllByUserIdx(userIdx);

        log.debug("userIdx: {}", userIdx);
        Goal currentMonthGoal = userGoals.stream()
            .filter(goal -> goal.isSameMonthAndYear(goal.getCreateAt(), LocalDateTime.now()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("현재 달에 등록된 목표가 없습니다."));

        return currentMonthGoal.computeGoalRate(walkTime);
    }
}
