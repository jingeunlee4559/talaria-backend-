package com.example.talaria.backend.service.manner;

import com.example.talaria.backend.mapper.manner.MannerDictMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MannerDictService {

  private final MannerDictMapper mapper;

  /** Export for AI (active=1 only) */
  @Transactional(readOnly = true)
  public Map<String, Object> exportActive() {
    List<Map<String, Object>> items = mapper.selectActiveDict();
    return Map.of(
        "version", System.currentTimeMillis(),
        "items", items
    );
  }

  /** List with optional filters */
  @Transactional(readOnly = true)
  public Map<String, Object> list(Integer active, String agentCode, int page, int size) {
    if (size <= 0) size = 20;
    if (size > 100) size = 100;
    if (page < 0) page = 0;
    int offset = page * size;

    List<Map<String, Object>> items = mapper.selectAll(active, agentCode, offset, size);
    boolean hasMore = items.size() == size;
    return Map.of(
        "page", page,
        "size", size,
        "hasMore", hasMore,
        "items", items
    );
  }

  @Transactional(readOnly = true)
  public Map<String, Object> getOne(long ruleId) {
    return mapper.selectOne(ruleId);
  }

  @Transactional
  public Map<String, Object> create(Map<String, Object> body) {
    normalize(body);
    int ins = mapper.insert(body);
    if (ins <= 0) throw new IllegalStateException("insert failed");
    Number rid = (Number) body.get("ruleId");
    return mapper.selectOne(rid.longValue());
  }

  @Transactional
  public Map<String, Object> update(long ruleId, Map<String, Object> body) {
    body.put("ruleId", ruleId);
    normalize(body);
    int up = mapper.update(body);
    if (up <= 0) throw new IllegalStateException("update affected 0 rows");
    return mapper.selectOne(ruleId);
  }

  @Transactional
  public Map<String, Object> setActive(long ruleId, boolean active) {
    int v = active ? 1 : 0;
    int up = mapper.setActive(ruleId, v);
    if (up <= 0) throw new IllegalStateException("setActive affected 0 rows");
    return mapper.selectOne(ruleId);
  }

  @Transactional
  public boolean delete(long ruleId) {
    return mapper.delete(ruleId) > 0;
  }

  /** 기본값/정규화 */
  private void normalize(Map<String, Object> body) {
    if (!body.containsKey("active")) body.put("active", 1);
    if (!body.containsKey("weight")) body.put("weight", 1.00);
    if (!body.containsKey("priority")) body.put("priority", 100);
    if (!body.containsKey("cooldownSec")) body.put("cooldownSec", 0);
    if (!body.containsKey("versionTag")) body.put("versionTag", "v1");
  }
}
