package com.umc.footprint.src.common.model.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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
@Table(name = "Photo")
// ToString 사용 시 무한 재귀 주의, 양방향 연관관계 추가 필요
//@ToString(exclude = {"footprint"})
public class Photo {

    @Id
    @Column(name = "photoIdx")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer photoIdx;

    @Column(name = "imageUrl")
    private String imageUrl;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "userIdx")
    private Integer userIdx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "footprintIdx")
    private Footprint footprint;

    @Builder
    public Photo(String imageUrl, String status, Integer userIdx, Footprint footprint) {
        this.imageUrl = imageUrl;
        this.status = status;
        this.userIdx = userIdx;
        this.footprint = footprint;
    }

    @PrePersist
    public void prePersist() {
        this.status = this.status == null ? "ACTIVE" : this.status;
    }

    public void setFootprint(Footprint footprint) {
        this.footprint = footprint;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }

    public void changeStatus(String status) {
        this.status = status;
    }


}
