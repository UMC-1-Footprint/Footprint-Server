package com.umc.footprint.src.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "Footprint")
public class Footprint {

    @Id
    @Column(name = "footprintIdx")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer footprintIdx;

    @Column(name = "coordinate", length = 100, nullable = false)
    private String coordinate;

    @Column(name = "record", columnDefinition = "LONGTEXT")
    private String record;

    @Column(name = "recordAt", nullable = false)
    private LocalDateTime recordAt;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "updateAt")
    private LocalDateTime updateAt;

    @Column(name = "onWalk", nullable = false)
    private Integer onWalk;

    @JsonIgnore
    @OneToMany(targetEntity = Tag.class, fetch = FetchType.LAZY, mappedBy = "footprint")
    private List<Tag> tagList = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "walkIdx")
    private Walk walk;

    @Builder
    public Footprint(Integer footprintIdx, String coordinate, String record, LocalDateTime recordAt, String status, LocalDateTime updateAt, Integer onWalk) {
        this.footprintIdx = footprintIdx;
        this.coordinate = coordinate;
        this.record = record;
        this.recordAt = recordAt;
        this.status = status;
        this.updateAt = updateAt;
        this.onWalk = onWalk;
    }

    @PrePersist
    public void prePersist() {
        this.status = this.status == null ? "ACTIVE" : this.status;
    }

    public void addTagList(Tag tag) {
        this.getTagList().add(tag);
        // 무한 루프에 빠지지 않게 체크
        if (tag.getFootprint() != this) {
            tag.setFootprint(this);
        }
    }

    public void setWalk(Walk walk) {
        this.walk = walk;
        if (!walk.getFootprintList().contains(this)) {
            walk.getFootprintList().add(this);
        }
    }


    public void setStatus(String status) {
        this.status = status;
    }

    public void changeStatus(String status) {
        this.status = status;
    }

    public void recordDecrypt(String decrypt) {
        this.record = decrypt;
        this.updateAt = LocalDateTime.now();
    }

}
