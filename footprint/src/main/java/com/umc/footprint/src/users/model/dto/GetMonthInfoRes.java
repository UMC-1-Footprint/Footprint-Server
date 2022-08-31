package com.umc.footprint.src.users.model.dto;

import com.umc.footprint.src.users.model.vo.GetMonthTotal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class GetMonthInfoRes {
    private List<String> goalDayList;
    private List<GetDayRateRes> getDayRatesRes; //일별 달성률?
    private GetMonthTotal getMonthTotal; //누적 3종세트

}
