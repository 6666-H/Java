package com.study.english.dto;

import lombok.Data;

import java.util.List;

@Data
public class TenantBookAuthUpdateRequest {

    private String tenantId;
    private List<Long> bookIds;
}
