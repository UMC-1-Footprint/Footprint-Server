package com.umc.footprint.src.footprints;

import com.umc.footprint.config.BaseException;
import static com.umc.footprint.config.BaseResponseStatus.*;

import com.umc.footprint.config.EncryptProperties;
import com.umc.footprint.src.AwsS3Service;
import com.umc.footprint.src.footprints.model.GetFootprintRes;
import com.umc.footprint.src.footprints.model.PatchFootprintReq;

import com.umc.footprint.src.model.*;
import com.umc.footprint.src.repository.*;
import com.umc.footprint.src.walks.WalkService;

import com.umc.footprint.utils.AES128;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@Slf4j
public class FootprintService {
    private final FootprintDao footprintDao;
    private final FootprintProvider footprintProvider;
    private final WalkRepository walkRepository;
    private final FootprintRepository footprintRepository;
    private final PhotoRepository photoRepository;
    private final TagRepository tagRepository;
    private final HashtagRepository hashtagRepository;
    private final WalkService walkService;
    private final UserRepository userRepository;
    private final AwsS3Service awsS3Service;
    private final EncryptProperties encryptProperties;

    @Autowired
    public FootprintService(FootprintDao footprintDao, FootprintProvider footprintProvider, WalkRepository walkRepository, FootprintRepository footprintRepository, PhotoRepository photoRepository, TagRepository tagRepository, HashtagRepository hashtagRepository, WalkService walkService, UserRepository userRepository, AwsS3Service awsS3Service, EncryptProperties encryptProperties) {
        this.footprintDao = footprintDao;
        this.footprintProvider = footprintProvider;
        this.walkRepository = walkRepository;
        this.footprintRepository = footprintRepository;
        this.photoRepository = photoRepository;
        this.tagRepository = tagRepository;
        this.hashtagRepository = hashtagRepository;
        this.walkService = walkService;
        this.userRepository = userRepository;
        this.awsS3Service = awsS3Service;
        this.encryptProperties = encryptProperties;
    }


    // ????????? ?????? (Patch)
    @Transactional(propagation = Propagation.NESTED, rollbackFor = Exception.class)
    public void modifyFootprint(PatchFootprintReq patchFootprintReq, int walkIdx, int footprintIdx, String userId) throws BaseException {
        try {
            Integer userIdx = userRepository.findByUserId(userId).getUserIdx();
            Footprint footprintByNumber = getFootprintByNumber(walkIdx, footprintIdx, userIdx);

            // ????????? ?????? ??????
            // 1. ?????? ??????
            if(patchFootprintReq.getWrite() != null) {
                footprintByNumber.recordDecrypt(new AES128(encryptProperties.getKey()).encrypt(patchFootprintReq.getWrite()));

                footprintRepository.save(footprintByNumber);
            }

            // 2. ?????? ??????
            // DB??? ???????????? ?????? ?????? ?????????
            List<Photo> photoList = photoRepository.findAllByFootprintAndStatus(footprintByNumber, "ACTIVE");
            // ??????????????? ?????? ?????????
            List<MultipartFile> photos = patchFootprintReq.getPhotos();

            if (photos != null) { // ????????? ??????(?????? ?????? X)?????? ?????? photos ????????? null??? ???

                if (photoList.isEmpty()) { // ???????????? ????????? ????????? ???????????? ??????
                    if (!("".equals(photos.get(0).getOriginalFilename()))) { // ????????? ????????? ???????????? ??????
                        uploadImg(photos, userIdx, footprintByNumber); // ????????? ????????? ?????????
                    }
                } else { // ???????????? ????????? ?????? ???????????? ??????
                    for (Photo photo : photoList) {
                        photo.changeStatus("INACTIVE");
                    }
                    photoRepository.saveAll(photoList);

                    if (photos.size() != 1 || !"".equals(photos.get(0).getOriginalFilename())) { // ????????? ????????? ?????? > ????????? ????????? ????????? ?????? > ?????? ?????? ????????? ??????
                        uploadImg(photos, userIdx, footprintByNumber); // ????????? ????????? ?????????
                    }
                }
            }

            // 3. ?????? ??????
            List<Hashtag> hashtagList = new ArrayList<>();
            List<Tag> tagList = new ArrayList<>();

            if(patchFootprintReq.getTagList() != null) {
                for(String tag : patchFootprintReq.getTagList()){
                    hashtagList.add(
                            Hashtag.builder()
                            .hashtag(new AES128(encryptProperties.getKey()).encrypt(tag))
                            .build()
                    );
                }
                List<Hashtag> savedHashtagList = hashtagRepository.saveAll(hashtagList);

                if (!footprintByNumber.getTagList().isEmpty()) {
                    for (Tag tag : footprintByNumber.getTagList()) {
                        tag.changeStatus("INACTIVE");
                    }
                    tagRepository.saveAll(footprintByNumber.getTagList());
                }

                for (Hashtag hashtag : savedHashtagList) {
                    Tag tag = Tag.builder()
                            .userIdx(userIdx)
                            .status("ACTIVE")
                            .build();
                    tag.setFootprint(footprintByNumber);
                    tag.setHashtag(hashtag);
                    tagList.add(tag);
                    footprintByNumber.addTagList(tag);
                }
                tagRepository.saveAll(tagList);

            }
        } catch (Exception exception) { // DB??? ????????? ?????? ?????? ?????? ???????????? ????????????.
            exception.printStackTrace();
            throw new BaseException(DATABASE_ERROR);
        }
    }


    // ????????? ?????? (PATCH) - ?????? ??????, ?????? ??????, ????????? ?????? ??????
    @Transactional(propagation = Propagation.NESTED, rollbackFor = Exception.class)
    public void deleteFootprint(int footprintIdx) throws BaseException {
        try {
            int activeFootprint = footprintDao.activeFootprint(footprintIdx);
            int footprintExist = footprintDao.footprintExist(footprintIdx);

            if (footprintExist == 0) { // ???????????? ???????????? ?????? ???
                throw new BaseException(NO_EXIST_FOOTPRINT);
            }
            else if (activeFootprint == 0) { // ?????? ????????? ?????????
                throw new BaseException(DELETED_FOOTPRINT);
            }

            footprintDao.deletePhotos(footprintIdx);
            footprintDao.deleteHashtags(footprintIdx);
            footprintDao.deleteFootprint(footprintIdx); // ????????? ?????? ?????? - 1, ?????? - 0
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new BaseException(DATABASE_ERROR);
        }
    }

    // ????????? ?????? (PATCH) - ?????? ??????, ?????? ??????, ????????? ?????? ??????
    @Transactional(propagation = Propagation.NESTED, rollbackFor = Exception.class)
    public void deleteFootprintJPA(int footprintIdx) throws BaseException {
        try {
            Optional<Footprint> targetFootprint = footprintRepository.findByFootprintIdx(footprintIdx);

            if(targetFootprint.isEmpty()) { // ???????????? ???????????? ?????? ???
                throw new BaseException(NO_EXIST_FOOTPRINT);
            }

            // Photo status ??????
            List<Photo> photoByFootprintIdx = photoRepository.findPhotoByFootprint(targetFootprint.get());

            for(Photo photo : photoByFootprintIdx){
                photo.setStatus("INACTIVE");
                photoRepository.save(photo);
            }

            // Hashtag status ??????
            List<Tag> TagByFootprintIdx = tagRepository.findByFootprint(targetFootprint.get());

            for(Tag tag : TagByFootprintIdx){
                tag.setStatus("INACTIVE");
                tagRepository.save(tag);
            }

            // Footprint status ??????
            targetFootprint.get().setStatus("INACTIVE");
            footprintRepository.save(targetFootprint.get());


        } catch (Exception exception) {
            exception.printStackTrace();
            throw new BaseException(DATABASE_ERROR);
    }
    }

    // ????????? URL ?????? > S3 ????????? > Photo ????????? ??????
    @Transactional(rollbackFor = Exception.class)
    public List<String> uploadImg(List<MultipartFile> photos, int userIdx, Footprint footprint) throws BaseException {
        List<String> urlList = new ArrayList<>(); // URL ????????? ?????????
        List<Photo> photoList = new ArrayList<>();

        // ????????? URL ?????? ??? S3 ?????????
        try {
            for (MultipartFile file : photos) {
                String imgUrl = awsS3Service.uploadFile(file);
                urlList.add(new AES128(encryptProperties.getKey()).encrypt(imgUrl));
                Photo photo = Photo.builder()
                        .imageUrl(new AES128(encryptProperties.getKey()).encrypt(imgUrl))
                        .status("ACTIVE")
                        .userIdx(userIdx)
                        .build();
                photo.setFootprint(footprint);
                photoList.add(photo);
            }
        } catch (Exception exception){
            throw new BaseException(DATABASE_ERROR);
        }

        // Photo ???????????? insert
        photoRepository.saveAll(photoList);

        return urlList;
    }

    // ????????? ??????
    public List<GetFootprintRes> getFootprints(int walkIdx, String userId) throws BaseException {
        try {
            Integer userIdx = userRepository.findByUserId(userId).getUserIdx();

            Walk walkByNumber = walkService.getWalkByNumber(walkIdx, userIdx);
            log.debug("walkByNumber: " + walkByNumber.toString());

            List<GetFootprintRes> getFootprintRes = new ArrayList<>();

            List<Footprint> footprintList = footprintRepository.findAllByWalkAndStatus(walkByNumber, "ACTIVE");

            /* ????????? ????????? ???????????? ?????? ?????? : write, photo, tag ????????? ?????? */

            log.debug("Footprint Handling");
            for (Footprint footprint : footprintList) {
                List<String> decryptPhotoList = new ArrayList<>();
                List<String> decryptTagList = new ArrayList<>();

                log.debug("?????? ?????????");
                List<Photo> photoList = photoRepository.findAllByFootprintAndStatus(footprint, "ACTIVE");
                for (Photo photo : photoList) {
                    if (photo.getStatus().equals("ACTIVE")) {
                        decryptPhotoList.add(new AES128(encryptProperties.getKey()).decrypt(photo.getImageUrl()));
                    }
                }
                log.debug("?????? ?????????");
                for (Tag tag : footprint.getTagList()) {
                    if (tag.getStatus().equals("ACTIVE")) {
                        decryptTagList.add(new AES128(encryptProperties.getKey()).decrypt(tag.getHashtag().getHashtag()));
                    }
                }
                log.debug("response ????????? ??????");
                getFootprintRes.add(GetFootprintRes.builder()
                        .footprintIdx(footprint.getFootprintIdx())
                        .recordAt(footprint.getRecordAt())
                        .write(new AES128(encryptProperties.getKey()).decrypt(footprint.getRecord()))
                        .photoList(decryptPhotoList)
                        .tagList(decryptTagList)
                        .onWalk(footprint.getOnWalk())
                        .build());
            }

            if (getFootprintRes.isEmpty()){
                throw new BaseException(NO_FOOTPRINT_IN_WALK); // ?????? ????????? ???????????? ?????? ???
            }
            return getFootprintRes;
        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    public Footprint getFootprintByNumber(int walkNumber, int footprintNumber, Integer userIdx) throws BaseException {
        Walk walkByNumber = walkService.getWalkByNumber(walkNumber, userIdx);
        PageRequest pageRequest = PageRequest.of(footprintNumber - 1, 1);
        try {
            Page<Footprint> footprintPage = footprintRepository.findByWalkAndStatusOrderByRecordAtAsc(walkByNumber, "ACTIVE", pageRequest);
            if (footprintPage.getTotalElements() == 0) {
                throw new BaseException(DELETED_FOOTPRINT);
            }
            Footprint footprint = footprintPage.get().collect(Collectors.toList()).get(0);
            return footprint;
        } catch (Exception exception) {
            throw new BaseException(INVALID_FOOTPRINTIDX);
        }
    }

    public int getWalkWholeIdx(int walkIdx, int userIdx) throws BaseException {
        try {
            List<Walk> walkList = walkRepository.findAllByStatusAndUserIdx("ACTIVE", userIdx);
            
//            for(Walk walk : walkList){
//                System.out.println("walk.getWalkIdx() = " + walk.getWalkIdx());
//            }

            return walkList.get(walkIdx-1).getWalkIdx();

        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    // ?????? ??? n?????? ????????? -> ??????????????? ????????? ?????????
    public int getFootprintWholeIdx(int walkIdx, int footprintIdx) throws BaseException {
        try {
            List<Footprint> footprintList = footprintRepository.findAllByWalkWalkIdx(walkIdx);

            return footprintList.get(footprintIdx-1).getFootprintIdx();

        } catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }
}
