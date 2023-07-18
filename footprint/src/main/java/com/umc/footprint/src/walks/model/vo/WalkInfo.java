package com.umc.footprint.src.walks.model.vo;

import java.time.LocalDateTime;
import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.umc.footprint.src.walks.CoordinateConvertor;
import com.umc.footprint.src.walks.model.entity.Walk;
import com.umc.footprint.utils.AES128;

import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WalkInfo {

    @NotNull
    @Positive
    @ApiModelProperty(example = "산책 인덱스", hidden = true)
    private Integer walkIdx;

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    @ApiModelProperty(value = "산책 시작 시간", example = "2020-03-18 18:25:40")
    private LocalDateTime startAt;

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    @ApiModelProperty(value = "산책 종료 시간", example = "2020-03-18 18:45:40")
    private LocalDateTime endAt;

    @NotNull
    @ApiModelProperty(value = "이동 거리", example = "0.3")
    private Double distance;

    @ApiModelProperty(value = "산책 좌표", example = "[[1.5, 2.12, 3.31, 4.25],[1.64, 9.51]]")
    private List<List<Double>> coordinates;

    @NotNull
    @Positive
    @ApiModelProperty(value = "소비한 칼로리", example = "140")
    private Integer calorie;

    @NotBlank
    @ApiModelProperty(value = "썸네일 이미지", example = "https://mystepsbucket.s3.ap-northeast-2.amazonaws.com/672bcf90-45c1-37e7-9c69-ce954f674133.jpg")
    private String thumbnail;

    @Builder
    public WalkInfo(
        Integer walkIdx,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Double distance,
        List<List<Double>> coordinates,
        Integer calorie,
        String thumbnail
    ) {
        this.walkIdx = walkIdx;
        this.startAt = startAt;
        this.endAt = endAt;
        this.distance = distance;
        this.coordinates = coordinates;
        this.calorie = calorie;
        this.thumbnail = thumbnail;
    }

    public Walk toEntity(String defaultThumbnail, int userIdx, Double goalRate) {
        String thumbnail = this.thumbnail;
        if (thumbnail.isEmpty()) {
            thumbnail = defaultThumbnail;
        }
        return Walk.builder()
            .startAt(this.startAt)
            .endAt(this.endAt)
            .distance(this.distance)
            .coordinate(AES128.encrypt(CoordinateConvertor.fromCoordinates(this.coordinates)))
            .pathImageUrl(AES128.encrypt(thumbnail))
            .userIdx(userIdx)
            .goalRate(goalRate)
            .calorie(this.calorie)
            .status("ACTIVE")
            .build();
    }

}


