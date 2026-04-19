package com.study.english.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.english.dto.BookWordAggregateDto;
import com.study.english.entity.Word;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WordMapper extends BaseMapper<Word> {

    Word selectByUnitIdAndWord(@Param("unitId") Long unitId, @Param("word") String word);

    @Select({
            "<script>",
            "select u.book_id as bookId, count(w.id) as wordCount, max(w.updated_at) as latestWordUpdatedAt",
            "from unit u",
            "left join word w on w.unit_id = u.id",
            "where u.book_id in",
            "<foreach collection='bookIds' item='bookId' open='(' separator=',' close=')'>",
            "#{bookId}",
            "</foreach>",
            "group by u.book_id",
            "</script>"
    })
    List<BookWordAggregateDto> selectBookAggregates(@Param("bookIds") List<Long> bookIds);
}
