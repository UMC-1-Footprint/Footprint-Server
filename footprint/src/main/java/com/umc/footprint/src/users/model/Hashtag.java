package com.umc.footprint.src.users.model;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Hashtag {
    private int walkIdx;
    private String hashtag;
}
