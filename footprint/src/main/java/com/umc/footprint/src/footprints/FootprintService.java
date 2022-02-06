package com.umc.footprint.src.footprints;

import com.umc.footprint.config.BaseException;
import static com.umc.footprint.config.BaseResponseStatus.*;

import com.umc.footprint.src.AwsS3Service;
import com.umc.footprint.src.footprints.model.PatchFootprintReq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
public class FootprintService {
    private final FootprintDao footprintDao;
    private final FootprintProvider footprintProvider;
    private final AwsS3Service awsS3Service;

    @Autowired
    public FootprintService(FootprintDao footprintDao, FootprintProvider footprintProvider, AwsS3Service awsS3Service) {
        this.footprintDao = footprintDao;
        this.footprintProvider = footprintProvider;
        this.awsS3Service = awsS3Service;
    }

    // 발자국 수정 (Patch)
    @Transactional(rollbackFor = Exception.class)
    public void modifyFootprint(PatchFootprintReq patchFootprintReq, int footprintIdx) throws BaseException {
        try {
            // validation - 존재하지 않는, 삭제된 발자국
            int activeFootprint = footprintDao.activeFootprint(footprintIdx);
            int footprintExist = footprintDao.footprintExist(footprintIdx);

            if (footprintExist == 0) { // 발자국이 존재하지 않을 때
                throw new BaseException(NO_EXIST_FOOTPRINT);
            }
            else if (activeFootprint == 0) { // 이미 삭제된 발자국
                throw new BaseException(DELETED_FOOTPRINT);
            }

            // 발자국 수정 과정
            int userIdx = footprintDao.findUserIdx(footprintIdx);

            // 1. 본문 수정
            footprintDao.modifyWrite(patchFootprintReq, footprintIdx);

            // 2. 사진 수정
            // DB에 저장되어 있는 파일 리스트
            List<String> dbPhotoList = footprintDao.getPhotoList(footprintIdx);
            // 전달되어온 파일 리스트
            List<MultipartFile> photos = patchFootprintReq.getPhotos();

            if(dbPhotoList.isEmpty()) { // 발자국에 저장된 사진이 존재하지 않음
                if(!photos.isEmpty()) { // 전달된 파일이 하나라도 존재
                    uploadImg(photos, userIdx, footprintIdx); // 새로운 사진들 업로드
                }
            }
            else { // 발자국에 저장된 기존 사진들이 존재
                if(photos.isEmpty()) { // 전달된 파일이 없음 > 사진을 지우고 싶다는 의미 > 기존 사진 삭제만 진행
                    footprintDao.deletePhotos(footprintIdx); // 기존 사진들 테이블에서 삭제
                }
                else {
                    footprintDao.deletePhotos(footprintIdx);
                    uploadImg(photos, userIdx, footprintIdx); // 새로운 사진들 업로드
                }
            }

            // 3. 태그 수정
            // DB에 저장되어 있는 태그 리스트
            List<String> dbTagList = footprintDao.getTagList(footprintIdx);
            // 전달되어온 태그 리스트
            List<String> tags = patchFootprintReq.getTagList();

            if(dbTagList.isEmpty()) { // 발자국에 저장된 태그가 존재하지 않음
                if(!tags.isEmpty()) { // 전달된 태그가 하나라도 존재
                    footprintDao.addTag(tags, userIdx, footprintIdx);
                }
            }
            else { // 발자국에 저장된 기존 사진들이 존재
                if(tags.isEmpty()) { // 전달된 파일이 없음 > 사진을 지우고 싶다는 의미 > 기존 사진 삭제만 진행
                    footprintDao.deleteHashtags(footprintIdx); // 기존 해시태그들 테이블에서 삭제
                }
                else {
                    footprintDao.deleteHashtags(footprintIdx);
                    footprintDao.addTag(tags, userIdx, footprintIdx); // 새로운 사진들 업로드
                }
            }

            // updateAt 업데이트
            footprintDao.updateAt(patchFootprintReq, footprintIdx);
        } catch (Exception exception) { // DB에 이상이 있는 경우 에러 메시지를 보냅니다.
            throw new BaseException(DATABASE_ERROR);
        }
    }


    // 발자국 삭제 (PATCH)
    @Transactional(rollbackFor = Exception.class)
    public void deleteFootprint(int footprintIdx) throws BaseException {
        try {
            int activeFootprint = footprintDao.activeFootprint(footprintIdx);
            int footprintExist = footprintDao.footprintExist(footprintIdx);

            if (footprintExist == 0) { // 발자국이 존재하지 않을 때
                throw new BaseException(NO_EXIST_FOOTPRINT);
            }
            else if (activeFootprint == 0) { // 이미 삭제된 발자국
                throw new BaseException(DELETED_FOOTPRINT);
            }
            int result = footprintDao.deleteFootprint(footprintIdx); // 발자국 삭제 성공 - 1, 실패 - 0

            // 발자국 삭제 실패 (Footprint, Photo 테이블 중 업데이트 되지 않은 테이블 존재)
            if (result == 0) {
                throw new BaseException(DELETE_FOOTPRINT_FAIL);
            }
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    // 이미지 URL 생성 > S3 업로드 > Photo 테이블 삽입
    @Transactional(rollbackFor = Exception.class)
    public List<String> uploadImg(List<MultipartFile> photos, int userIdx, int footprintIdx) throws BaseException {
        List<String> photoList = new ArrayList<>(); // URL 저장할 리스트

        // 이미지 URL 생성 및 S3 업로드
        for(MultipartFile photo : photos) {
            String imgUrl = awsS3Service.uploadFile(photo);
            photoList.add(imgUrl);
        }

        // Photo 테이블에 insert
        footprintDao.addPhoto(photoList, userIdx, footprintIdx);

        return photoList;
    }
}
