package com.umc.footprint.src.walks.model;

import lombok.*;

@Getter
@NoArgsConstructor
public class GetWalkTime {
    private String date; //날짜
    private String startAt; //산책 시작 시간
    private String endAt; //산책 끝 시간
    private String timeString; //산책 시간은 string으로!

    @Builder
    public GetWalkTime(String date, String startAt, String endAt, String timeString) {
        this.date = date;
        this.startAt = startAt;
        this.endAt = endAt;
        this.timeString = timeString;
    }

//    public void convTimeString() {
//        int time = Integer.parseInt(timeString);
//        int min = time / 60;
//        int sec = time % 60;
//
//        String str;
//        str = String.format("%02d:%02d", min, sec );
//        setTimeString(str);
//    }
}


