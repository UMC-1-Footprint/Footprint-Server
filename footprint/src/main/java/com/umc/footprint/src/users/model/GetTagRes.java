package com.umc.footprint.src.users.model;

import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor
public class GetTagRes {
    private String walkAt;
    private List<SearchWalk> walks;

    @Builder
    public GetTagRes(String walkAt, List<SearchWalk> walks) {
        this.walkAt = walkAt;
        this.walks = walks;
    }
}
