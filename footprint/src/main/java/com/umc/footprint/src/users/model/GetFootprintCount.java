package com.umc.footprint.src.users.model;

import lombok.*;

@Getter
@NoArgsConstructor
public class GetFootprintCount {
    private int day;
    private int walkCount;

    @Builder
    public GetFootprintCount(int day, int walkCount) {
        this.day = day;
        this.walkCount = walkCount;
    }
}
