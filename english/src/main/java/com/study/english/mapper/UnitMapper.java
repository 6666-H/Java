package com.study.english.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.english.dto.BookUnitAggregateDto;
import com.study.english.entity.Unit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UnitMapper extends BaseMapper<Unit> {

    @Select({
            "<script>",
            "select book_id as bookId, count(*) as unitCount, max(updated_at) as latestUnitUpdatedAt",
            "from unit",
            "where book_id in",
            "<foreach collection='bookIds' item='bookId' open='(' separator=',' close=')'>",
            "#{bookId}",
            "</foreach>",
            "group by book_id",
            "</script>"
    })
    List<BookUnitAggregateDto> selectBookAggregates(@Param("bookIds") List<Long> bookIds);
}
