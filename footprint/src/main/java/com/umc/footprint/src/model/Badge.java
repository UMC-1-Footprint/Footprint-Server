package com.umc.footprint.src.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "Badge")
public class Badge {

    @Id
    @Column(name = "badgeIdx")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int badgeIdx;

    @Column(name = "badgeName", length = 20, nullable = false)
    private String badgeName;

    @Column(name = "badgeUrl")
    private String badgeUrl;

    @Column(name = "badgeDate", length = 30, nullable = false)
    private String badgeDate;

    @Builder
    public Badge(int badgeIdx, String badgeName, String badgeUrl, String badgeDate) {
        this.badgeIdx = badgeIdx;
        this.badgeName = badgeName;
        this.badgeUrl = badgeUrl;
        this.badgeDate = badgeDate;
    }

}
