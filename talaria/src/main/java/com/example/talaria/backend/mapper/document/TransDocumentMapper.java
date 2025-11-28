package com.example.talaria.backend.mapper.document;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

@Mapper
public interface TransDocumentMapper {

    Map<String, Object> selectById(@Param("table") String table,
                                   @Param("pk")    String pk,
                                   @Param("id")    long id);
}
