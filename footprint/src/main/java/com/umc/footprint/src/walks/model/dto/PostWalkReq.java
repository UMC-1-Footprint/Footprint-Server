package com.umc.footprint.src.walks.model.dto;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.umc.footprint.src.footprints.model.vo.FootprintInfo;
import com.umc.footprint.src.walks.model.entity.Walk;
import com.umc.footprint.src.walks.model.vo.WalkInfo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PostWalkReq {

    @ApiModelProperty(value = "산책 정보")
    @NotNull
    private WalkInfo walk;

    @ApiModelProperty(value = "발자국 정보들")
    @NotNull
    private List<FootprintInfo> footprintList;

    @Builder
    public PostWalkReq(WalkInfo walk, List<FootprintInfo> footprintList) {
        this.walk = walk;
        this.footprintList = footprintList;
    }

    @ApiModelProperty(hidden = true)
    public void setWalkStrCoordinate(WalkInfo newCoordinateWalk) {
        this.walk = newCoordinateWalk;
    }

    @ApiModelProperty(hidden = true)
    public void setConvertedFootprints(ArrayList<FootprintInfo> convertedFootprints) {
        this.footprintList = convertedFootprints;
    }

    public Walk toWalk(String defaultThumbnail, int userIdx, Double goalRate) {
        return walk.toEntity(defaultThumbnail, userIdx, goalRate);
    }

    public boolean isFootprintsEmpty() {
        return footprintList.isEmpty();
    }
}