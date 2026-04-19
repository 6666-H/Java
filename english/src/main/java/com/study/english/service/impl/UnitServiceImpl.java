package com.study.english.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.english.entity.Unit;
import com.study.english.mapper.UnitMapper;
import com.study.english.service.UnitService;
import org.springframework.stereotype.Service;

@Service
public class UnitServiceImpl extends ServiceImpl<UnitMapper, Unit> implements UnitService {

    @Override
    public Unit getOrCreateByBookIdAndName(Long bookId, String name, int sortOrder) {
        Unit one = getOne(new LambdaQueryWrapper<Unit>()
                .eq(Unit::getBookId, bookId)
                .eq(Unit::getName, name));
        if (one != null) {
            one.setSortOrder(sortOrder);
            updateById(one);
            return one;
        }
        Unit unit = new Unit();
        unit.setBookId(bookId);
        unit.setName(name);
        unit.setSortOrder(sortOrder);
        save(unit);
        return unit;
    }
}
