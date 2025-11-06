package com.example.talaria.backend.mapper.manner;

import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface MannerDictMapper {

  @Select("""
    SELECT *
    FROM tl_manner_dict
    WHERE active = 1
    ORDER BY priority ASC, weight DESC, rule_id ASC
  """)
  List<Map<String, Object>> selectActiveDict();

  @Select("""
    <script>
    SELECT *
    FROM tl_manner_dict
    <where>
      <if test="active != null">
        active = #{active}
      </if>
      <if test="agentCode != null and agentCode != ''">
        <if test="active != null"> AND </if>
        agent_code = #{agentCode}
      </if>
    </where>
    ORDER BY updated_at DESC, rule_id DESC
    LIMIT #{limit} OFFSET #{offset}
    </script>
  """)
  List<Map<String, Object>> selectAll(
      @Param("active") Integer active,
      @Param("agentCode") String agentCode,
      @Param("offset") int offset,
      @Param("limit") int limit
  );

  @Select("""
    SELECT * FROM tl_manner_dict WHERE rule_id = #{ruleId} LIMIT 1
  """)
  Map<String, Object> selectOne(@Param("ruleId") long ruleId);

  @Insert("""
    INSERT INTO tl_manner_dict(
      active, agent_code, label, severity, weight, priority,
      pattern_type, pattern_text, case_sensitive, negate,
      speaker_locale, listener_locale, culture_pair,
      explanation_tpl, suggestion_tpl, cooldown_sec, version_tag
    ) VALUES (
      #{active}, #{agentCode}, #{label}, #{severity}, #{weight}, #{priority},
      #{patternType}, #{patternText}, #{caseSensitive}, #{negate},
      #{speakerLocale}, #{listenerLocale}, #{culturePair},
      #{explanationTpl}, #{suggestionTpl}, #{cooldownSec}, #{versionTag}
    )
  """)
  @Options(useGeneratedKeys = true, keyProperty = "ruleId", keyColumn = "rule_id")
  int insert(Map<String, Object> params);

  @Update("""
    <script>
    UPDATE tl_manner_dict
    <set>
      <if test="active != null">active = #{active},</if>
      <if test="agentCode != null and agentCode != ''">agent_code = #{agentCode},</if>
      <if test="label != null and label != ''">label = #{label},</if>
      <if test="severity != null">severity = #{severity},</if>
      <if test="weight != null">weight = #{weight},</if>
      <if test="priority != null">priority = #{priority},</if>
      <if test="patternType != null and patternType != ''">pattern_type = #{patternType},</if>
      <if test="patternText != null and patternText != ''">pattern_text = #{patternText},</if>
      <if test="caseSensitive != null">case_sensitive = #{caseSensitive},</if>
      <if test="negate != null">negate = #{negate},</if>
      <if test="speakerLocale != null">speaker_locale = #{speakerLocale},</if>
      <if test="listenerLocale != null">listener_locale = #{listenerLocale},</if>
      <if test="culturePair != null">culture_pair = #{culturePair},</if>
      <if test="explanationTpl != null and explanationTpl != ''">explanation_tpl = #{explanationTpl},</if>
      <if test="suggestionTpl != null and suggestionTpl != ''">suggestion_tpl = #{suggestionTpl},</if>
      <if test="cooldownSec != null">cooldown_sec = #{cooldownSec},</if>
      <if test="versionTag != null and versionTag != ''">version_tag = #{versionTag},</if>
    </set>
    WHERE rule_id = #{ruleId}
    </script>
  """)
  int update(Map<String, Object> params);

  @Update("""
    UPDATE tl_manner_dict SET active = #{active} WHERE rule_id = #{ruleId}
  """)
  int setActive(@Param("ruleId") long ruleId, @Param("active") int active);

  @Delete("""
    DELETE FROM tl_manner_dict WHERE rule_id = #{ruleId}
  """)
  int delete(@Param("ruleId") long ruleId);
}
