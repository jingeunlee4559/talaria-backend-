package com.example.talaria.backend.controller;

import com.example.talaria.backend.dto.manner.MannerRuleUpsertReq;
import com.example.talaria.backend.service.manner.MannerDictService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manner/dict")
public class MannerDictController {

  private final MannerDictService service;

  /** AI용 Export (active=1만) */
  @GetMapping("/export")
  public Map<String, Object> export() {
    return service.exportActive();
  }

  /** 관리용 목록 조회 */
  @GetMapping
  public Map<String, Object> list(
      @RequestParam(required = false) Integer active,
      @RequestParam(required = false) String agentCode,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    return service.list(active, agentCode, page, size);
  }

  /** 단건 조회 */
  @GetMapping("/{ruleId}")
  public Map<String, Object> getOne(@PathVariable long ruleId) {
    return service.getOne(ruleId);
  }

  /** 생성 */
  @PostMapping
  public Map<String, Object> create(@RequestBody @Valid MannerRuleUpsertReq req) {
    Map<String, Object> body = toParam(req);
    return service.create(body);
  }

  /** 수정 */
  @PatchMapping("/{ruleId}")
  public Map<String, Object> update(
      @PathVariable long ruleId,
      @RequestBody @Valid MannerRuleUpsertReq req
  ) {
    Map<String, Object> body = toParam(req);
    return service.update(ruleId, body);
  }

  /** 활성/비활성 토글 */
  @PatchMapping("/{ruleId}/active")
  public Map<String, Object> setActive(
      @PathVariable long ruleId,
      @RequestParam boolean value
  ) {
    return service.setActive(ruleId, value);
  }

  /** 삭제 */
  @DeleteMapping("/{ruleId}")
  public Map<String, Object> delete(@PathVariable long ruleId) {
    boolean ok = service.delete(ruleId);
    return Map.of("ok", ok);
  }

  private static Map<String, Object> toParam(MannerRuleUpsertReq r) {
    Map<String, Object> m = new HashMap<>();
    if (r.getActive() != null) m.put("active", r.getActive());
    m.put("agentCode", r.getAgentCode());
    m.put("label", r.getLabel());
    if (r.getSeverity() != null) m.put("severity", r.getSeverity());
    if (r.getWeight() != null) m.put("weight", r.getWeight());
    if (r.getPriority() != null) m.put("priority", r.getPriority());
    m.put("patternType", r.getPatternType());
    m.put("patternText", r.getPatternText());
    if (r.getCaseSensitive() != null) m.put("caseSensitive", r.getCaseSensitive());
    if (r.getNegate() != null) m.put("negate", r.getNegate());
    if (r.getSpeakerLocale() != null) m.put("speakerLocale", r.getSpeakerLocale());
    if (r.getListenerLocale() != null) m.put("listenerLocale", r.getListenerLocale());
    if (r.getCulturePair() != null) m.put("culturePair", r.getCulturePair());
    m.put("explanationTpl", r.getExplanationTpl());
    m.put("suggestionTpl", r.getSuggestionTpl());
    if (r.getCooldownSec() != null) m.put("cooldownSec", r.getCooldownSec());
    if (r.getVersionTag() != null) m.put("versionTag", r.getVersionTag());
    return m;
  }
}
