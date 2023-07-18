package com.umc.footprint.utils;

import java.time.LocalDateTime;
import java.util.Objects;

import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Getter;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseEntity {
    @CreatedDate
    private LocalDateTime createAt;

    @LastModifiedDate
    private LocalDateTime updateAt;

    private static boolean isSameYear(LocalDateTime first, LocalDateTime second) {
        return first.getYear() == second.getYear();
    }

    private static boolean isSameMonth(LocalDateTime first, LocalDateTime second) {
        return Objects.equals(first.getMonth(), second.getMonth());
    }

    public boolean isSameMonthAndYear(LocalDateTime first, LocalDateTime second) {
        return isSameMonth(first, second) && isSameYear(first, second);
    }
}
