package com.example.talaria.backend.dto.manner;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class MannerRuleUpsertReq {
  @Min(0) @Max(1)
  private Integer active;                 // 0/1

  @NotBlank
  private String agentCode;               // direct/negative/address/tone...

  @NotBlank
  private String label;                   // direct/negative_eval/addressing/literal_tone

  @Min(1) @Max(3)
  private Integer severity;               // 1~3

  @DecimalMin("0.0")
  private Double weight;                  // default 1.0

  private Integer priority;               // default 100

  @NotBlank
  private String patternType;             // regex/contains/exact/glob

  @NotBlank
  private String patternText;

  @Min(0) @Max(1)
  private Integer caseSensitive;          // 0/1

  @Min(0) @Max(1)
  private Integer negate;                 // 0/1

  private String speakerLocale;           // ko, en-US
  private String listenerLocale;          // ko, en-US
  private String culturePair;             // ko->en

  @NotBlank
  private String explanationTpl;

  @NotBlank
  private String suggestionTpl;

  private Integer cooldownSec;            // default 0
  private String versionTag;              // default v1
}
