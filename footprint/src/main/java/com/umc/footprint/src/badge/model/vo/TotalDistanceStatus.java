package com.umc.footprint.src.badge.model.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum TotalDistanceStatus {
    TEN_KM(10, "누적 10km", 2),
    THIRTY_KM(30, "누적 30km", 3),
    FIFTY_KM(50, "누적 50km", 4),
    HUNDRED_KM(100, "누적 100km", 5),
    ;

    private final int distance;
    private final String badgeName;
    private final Integer badgeIdx;

    public static Optional<TotalDistanceStatus> getDistanceBadge(Double distance) {
        return Arrays.stream(TotalDistanceStatus.values())
                .sorted(Comparator.reverseOrder())
                .filter(totalDistanceStatus -> distance >= totalDistanceStatus.distance)
                .findFirst();
    }

    public static Integer minBadgeIdx() {
        return TEN_KM.badgeIdx;
    }

    public static Integer maxBadgeIdx() {
        return HUNDRED_KM.badgeIdx;
    }
}
