package com.umc.footprint.src.walks.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@RequiredArgsConstructor
public class GetCourseInfoRes {
    private final Integer courseIdx;
    private final Integer walkIdx;
    private final LocalDateTime startAt;
    private final LocalDateTime endAt;
    private final Double distance;
    private final List<ArrayList<Double>> coordinates;
    private final List<HashtagVO> hashtags;
    private final List<String> photos;
}
