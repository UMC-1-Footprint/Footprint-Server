package com.umc.footprint.src.badge.model.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Comparator;

@Getter
@RequiredArgsConstructor
public enum TotalDistance {
    TEN(10, "누적 10km", 2),
    THIRTY(30, "누적 30km", 3),
    FIFTY(50, "누적 50km", 4),
    HUNDRED(100, "누적 100km", 5),
    ;

    private final int distance;
    private final String badgeName;
    private final Integer badgeIdx;

    public static TotalDistance getDistanceBadge(Double distance) {
        return Arrays.stream(TotalDistance.values())
                .sorted(Comparator.reverseOrder())
                .filter(totalDistance -> distance >= totalDistance.distance)
                .findFirst()
                .orElse(null);
    }
}
