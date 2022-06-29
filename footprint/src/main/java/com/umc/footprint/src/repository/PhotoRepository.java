package com.umc.footprint.src.repository;

import com.umc.footprint.src.model.Footprint;
import com.umc.footprint.src.model.Photo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PhotoRepository extends JpaRepository<Photo, Integer> {

    List<Photo> findPhotoByFootprint(Footprint footprint);
}
