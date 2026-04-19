package com.study.english.dto;

import lombok.Data;

@Data
public class StudentProfileUpdateRequest {
    private String nickname;
    private String avatar;
    private String phone;
}
