package com.umc.footprint.src.common.model.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;

import com.umc.footprint.src.footprints.model.entity.Footprint;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "Tag")
public class Tag {

    @Id
    @Column(name = "tagIdx")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer tagIdx;

    @Column(name = "userIdx", nullable = false)
    private Integer userIdx;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "walkIdx")
    private Integer walkIdx;

    @OneToOne(targetEntity = Hashtag.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "hashtagIdx")
    private Hashtag hashtag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "footprintIdx")
    private Footprint footprint;

    @Builder
    public Tag(Integer tagIdx, Integer userIdx, String status, Integer walkIdx, Hashtag hashtag, Footprint footprint) {
        this.tagIdx = tagIdx;
        this.userIdx = userIdx;
        this.status = status;
        this.walkIdx = walkIdx;
        this.hashtag = hashtag;
        this.footprint = footprint;
    }

    @PrePersist
    public void prePersist() {
        this.status = this.status == null ? "ACTIVE" : this.status;
    }

    public void setHashtag(Hashtag hashtag) {
        this.hashtag = hashtag;
    }

    public void setFootprint(Footprint footprint) {
        this.footprint = footprint;
        if (!footprint.getTagList().contains(this)) {
            footprint.addTag(this);
        }
    }

    public void setWalk(Integer walkIdx) {
        this.walkIdx = walkIdx;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void changeStatus(String status) {
        this.status = status;
    }
}