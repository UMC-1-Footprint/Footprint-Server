package com.umc.footprint.src.walks.model;

import lombok.*;

import java.text.SimpleDateFormat;

@Getter
@NoArgsConstructor
public class GetWalkInfo {
    private int walkIdx;
    GetWalkTime getWalkTime;
    private int calorie;
    private double distance;
    private int footCount;
    private String pathImageUrl;

    @Builder
    public GetWalkInfo(int walkIdx, GetWalkTime getWalkTime, int calorie, double distance, int footCount, String pathImageUrl) {
        this.walkIdx = walkIdx;
        this.getWalkTime = getWalkTime;
        this.calorie = calorie;
        this.distance = distance;
        this.footCount = footCount;
        this.pathImageUrl = pathImageUrl;
    }

    public void changePathImageUrl(GetWalkInfo getWalkInfo, String pathImageUrl) {
        this.walkIdx = getWalkInfo.getWalkIdx();
        this.getWalkTime = getWalkInfo.getGetWalkTime();
        this.calorie = getWalkInfo.getCalorie();
        this.distance = getWalkInfo.getDistance();
        this.footCount = getWalkInfo.getFootCount();
        this.pathImageUrl = pathImageUrl;
    }
}
