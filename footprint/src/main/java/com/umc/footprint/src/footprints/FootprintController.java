package com.umc.footprint.src.footprints;

import org.springframework.web.bind.annotation.*;

import com.umc.footprint.src.footprints.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.umc.footprint.config.BaseException;
import com.umc.footprint.config.BaseResponse;

import java.util.List;

@RestController
@RequestMapping("/footprints")
public class FootprintController {
    private final FootprintProvider footprintProvider;
    private final FootprintService footprintService;

    @Autowired
    public FootprintController(FootprintProvider footprintProvider, FootprintService footprintService) {
        this.footprintProvider = footprintProvider;
        this.footprintService = footprintService;
    }

    /**
     * 발자국 조회 API
     * [GET] /footprints/:walkidx
     */
    @ResponseBody
    @GetMapping("/{walkIdx}")
    public BaseResponse<List<GetFootprintRes>> getFootprints(@PathVariable("walkIdx") int walkIdx) {
        try {
            List<GetFootprintRes> getFootprintRes = footprintProvider.getFootprints(walkIdx);
            return new BaseResponse<>(getFootprintRes);
        } catch (BaseException exception) {
            return new BaseResponse<>((exception.getStatus()));
        }
    }

    /**
     * 발자국 수정 API
     * [PATCH] /footprints/:footprintIdx
     */
    @ResponseBody
    @PatchMapping("/{footprintIdx}")
    public BaseResponse<String> modifyFootprint(@PathVariable("footprintIdx") int footprintIdx, GetFootprint footprint) {
        try {
            PatchFootprintReq patchFootprintReq = new PatchFootprintReq(footprint.getWrite(), footprint.getPhotos(), footprint.getTagList());
            footprintService.modifyFootprint(patchFootprintReq, footprintIdx);

            String result = "발자국이 수정되었습니다.";
            return new BaseResponse<>(result);
        } catch (BaseException exception) {
            return new BaseResponse<>((exception.getStatus()));
        }
    }

    /**
     * 발자국 삭제 API
     * [PATCH] /footprints/:footprintIdx/status
     */
    @ResponseBody
    @PatchMapping("/{footprintIdx}/status")
    public BaseResponse<String> DeleteFootprint(@PathVariable("footprintIdx") int footprintIdx) {
        try {
            footprintService.deleteFootprint(footprintIdx);
            String result = "발자국을 삭제하였습니다.";

            return new BaseResponse<>(result);
        } catch (BaseException exception) {
            return new BaseResponse<>((exception.getStatus()));
        }
    }
}
