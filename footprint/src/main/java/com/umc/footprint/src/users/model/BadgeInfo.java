package com.umc.footprint.src.users.model;

import lombok.*;

@Getter
@NoArgsConstructor
public class BadgeInfo {
    private int badgeIdx;
    private String badgeName;
    private String badgeUrl;
    private String badgeDate;

    @Builder
    public BadgeInfo(int badgeIdx, String badgeName, String badgeUrl, String badgeDate) {
        this.badgeIdx = badgeIdx;
        this.badgeName = badgeName;
        this.badgeUrl = badgeUrl;
        this.badgeDate = badgeDate;
    }
}
