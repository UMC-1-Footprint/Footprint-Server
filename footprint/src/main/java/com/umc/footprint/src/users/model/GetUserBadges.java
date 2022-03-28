package com.umc.footprint.src.users.model;

import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor

public class GetUserBadges {
    private BadgeInfo repBadgeInfo;
    private List<BadgeOrder> badgeList;


    @Builder
    public GetUserBadges(BadgeInfo repBadgeInfo, List<BadgeOrder> badgeList) {
        this.repBadgeInfo = repBadgeInfo;
        this.badgeList = badgeList;
    }
}