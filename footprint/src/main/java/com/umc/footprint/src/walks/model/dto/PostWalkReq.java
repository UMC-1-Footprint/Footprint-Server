package com.umc.footprint.src.walks.model.dto;

import io.swagger.annotations.ApiModelProperty;
import com.umc.footprint.src.footprints.model.vo.FootprintInfo;
import com.umc.footprint.src.walks.model.vo.WalkInfo;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
public class PostWalkReq {

    @ApiModelProperty(value = "산책 정보")
    private WalkInfo walk;

    @ApiModelProperty(value = "발자국 정보들")
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
}