package com.umc.footprint.src.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "UserBadge")
public class UserBadge {

    @Id
    @Column(name = "collectionIdx")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int collectionIdx;

    @Column(name = "userIdx", nullable = false)
    private int userIdx;

    @Column(name = "badgeIdx", nullable = false)
    private int badgeIdx;

    @Column(name = "status", length = 20)
    private String status;

    @Builder
    public UserBadge(int collectionIdx, int userIdx, int badgeIdx, String status) {
        this.collectionIdx = collectionIdx;
        this.userIdx = userIdx;
        this.badgeIdx = badgeIdx;
        this.status = status;
    }

}
