package com.umc.footprint.src.badge.model.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum TotalRecordStatus {
    TEN_COUNT(10, "누적 10회", 6),
    TWENTY_COUNT(20, "누적 20회", 7),
    THIRTY_COUNT(30, "누적 30회", 8),
    ;

    private final int recordCount;
    private final String badgeName;
    private final Integer badgeIdx;

    public static Optional<TotalRecordStatus> getWalkCountBadge(int count) {
        return Arrays.stream(TotalRecordStatus.values())
                .sorted(Comparator.reverseOrder())
                .filter(totalRecordStatus -> count >= totalRecordStatus.recordCount)
                .findFirst();
    }

    public static Integer minBadgeIdx() {
        return TEN_COUNT.badgeIdx;
    }

    public static Integer maxBadgeIdx() {
        return THIRTY_COUNT.badgeIdx;
    }

}
