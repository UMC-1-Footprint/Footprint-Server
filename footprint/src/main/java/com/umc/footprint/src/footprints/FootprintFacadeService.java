package com.umc.footprint.src.footprints;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.footprint.src.common.model.entity.Hashtag;
import com.umc.footprint.src.common.model.entity.Tag;
import com.umc.footprint.src.common.repository.HashtagRepository;
import com.umc.footprint.src.common.repository.PhotoRepository;
import com.umc.footprint.src.common.repository.TagRepository;
import com.umc.footprint.src.footprints.model.entity.Footprint;
import com.umc.footprint.src.footprints.model.vo.FootprintInfo;
import com.umc.footprint.src.footprints.repository.FootprintRepository;
import com.umc.footprint.src.walks.model.dto.PostWalkReq;
import com.umc.footprint.src.walks.model.entity.Walk;

import lombok.RequiredArgsConstructor;

@Transactional
@Service
@RequiredArgsConstructor
public class FootprintFacadeService {

	private final FootprintRepository footprintRepository;
	private final HashtagRepository hashtagRepository;
	private final TagRepository tagRepository;
	private final PhotoRepository photoRepository;

	public void addFootprints(PostWalkReq request, int userIdx, Walk walk) {
		if (request.isFootprintsEmpty())
			return;

		request.getFootprintList()
			.forEach(footprintInfo -> addFootprint(userIdx, walk, footprintInfo));
	}

	private void addFootprint(int userIdx, Walk walk, FootprintInfo footprintInfo) {
		Footprint footprint = footprintRepository.save(footprintInfo.toEntity(walk));
		walk.addFootprint(footprint);

		addHashTags(userIdx, footprintInfo, footprint);

		photoRepository.saveAll(footprintInfo.toPhotos(userIdx, footprint));
	}

	private void addHashTags(int userIdx, FootprintInfo footprintInfo, Footprint footprint) {
		List<Hashtag> hashtags = hashtagRepository.saveAll(footprintInfo.toHashtag());
		List<Tag> tags = tagRepository.saveAll(footprintInfo.toTags(footprint, hashtags, userIdx));
		tags.forEach(footprint::addTag);
	}
}
