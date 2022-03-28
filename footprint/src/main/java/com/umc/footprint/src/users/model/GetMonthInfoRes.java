package com.umc.footprint.src.users.model;

import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor
public class GetMonthInfoRes {
    private List<String> goalDayList;
    private List<GetDayRateRes> getDayRatesRes; //일별 달성률?
    private GetMonthTotal getMonthTotal; //누적 3종세트

    @Builder
    public GetMonthInfoRes(List<String> goalDayList, List<GetDayRateRes> getDayRatesRes, GetMonthTotal getMonthTotal) {
        this.goalDayList = goalDayList;
        this.getDayRatesRes = getDayRatesRes;
        this.getMonthTotal = getMonthTotal;
    }
}
