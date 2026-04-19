package com.study.english.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.english.entity.Unit;

public interface UnitService extends IService<Unit> {

    Unit getOrCreateByBookIdAndName(Long bookId, String name, int sortOrder);
}
