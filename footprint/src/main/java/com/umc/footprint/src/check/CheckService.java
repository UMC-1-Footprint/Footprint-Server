package com.umc.footprint.src.check;

import static com.umc.footprint.config.BaseResponseStatus.*;

import java.util.List;

import org.springframework.stereotype.Service;

import com.umc.footprint.config.BaseException;
import com.umc.footprint.src.common.model.entity.Hashtag;
import com.umc.footprint.src.common.repository.HashtagRepository;
import com.umc.footprint.utils.AES128;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckService {

    private final HashtagRepository hashtagRepository;


    public String checkEncryptWalk(String encryptString) throws BaseException {
        try{
            String encryptResult = AES128.encrypt(encryptString);

            log.info("encryptResult = {}",encryptResult );

            return encryptResult;
        } catch(Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    public String checkDecryptWalk(String decryptString) throws BaseException{
        try{
            System.out.println("decryptString = " + decryptString);
            String decryptResult = AES128.decrypt(decryptString);

            System.out.println("decryptResult = " + decryptResult);

            return decryptResult;
        } catch(Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }

    public void decryptHashtag() throws Exception {
        List<Hashtag> all = hashtagRepository.findAll();
        try {
            for (Hashtag encryptedHashtag : all) {
                encryptedHashtag.decryptHashtag(AES128.decrypt(encryptedHashtag.getHashtag()));
            }
            hashtagRepository.saveAll(all);
        } catch (Exception exception) {
            throw new Exception("해시태그 복호화 실패");
        }
    }
}
