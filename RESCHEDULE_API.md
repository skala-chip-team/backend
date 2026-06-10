# 재조정 제안 페이지 API 명세

모든 응답은 공통 래퍼로 감싸집니다:

```json
{ "success": true, "code": 200, "message": "요청 성공", "data": <본문> }
```

> 프론트는 항상 `res.data.data` 에서 실제 본문을 꺼내 쓰세요. 에러는 `success:false` + `code`(HTTP 상태) + `message`.

Base path: `/api/reschedule/groups`

---

## 1) 목록 — `GET /api/reschedule/groups`

재조정안 관리/목록 페이지. 위험 탐지 알림(폴링) 도 이 엔드포인트를 사용.

**Query (선택)**
| param | 설명 |
|---|---|
| `districtId` | 구역 필터 (예: `DST-01`). 없으면 전체 |
| `status` | `pending` / `approved` / `expired` / `active`(=pending+approved) / 없으면 전체 |

**응답 `data`: 배열** (생성 최신순)
```json
[
  {
    "groupId": "GRP-8ebe764e-...",
    "districtId": "DST-01",
    "stepId": "STEP-D693D364B",
    "processStep": "STEP_D",
    "maxRiskScore": 0.82,
    "riskLevel": "High",            // 알림 배지용: Low/Medium/High/Critical
    "groupStatus": "pending",       // pending / approved / expired
    "createdAt": "2025-05-05T15:35:00",
    "affectedUnits": [
      { "unitId": "UNIT-5C0FA62A", "estimatedDelayHr": 4.2 }
    ]
  }
]
```

**폴링 알림 팁:** `?status=pending` 으로 주기 조회 → `groupId` 가 직전 `seen` 셋에 없고 `riskLevel` 이 High/Critical 이면 토스트.

---

## 2) 상세(제안) — `GET /api/reschedule/groups/{groupId}`

그룹 정보 + 관련 delay_risks + **에이전트 원인분석/전략별 재조정안**.

**응답 `data`**
```json
{
  "groupId": "GRP-8ebe764e-...",
  "districtId": "DST-01",
  "stepId": "STEP-D693D364B",
  "processStep": "STEP_D",
  "stepOrder": 4,
  "maxDelayProbability": 0.82,
  "groupStatus": "pending",
  "actedAt": "2025-05-05T15:35:00",

  "delayRisks": [
    {
      "riskId": "RISK-87FBA642", "unitId": "UNIT-5C0FA62A",
      "riskLevel": "High", "riskFactor": "Machine_Capacity",
      "riskScore": 0.71, "delayProbability": 0.82,
      "estimatedDelayHr": 4.2, "detectionTime": "2025-05-05T11:10:00"
    }
  ],

  "riskAnalysis": {                 // 에이전트 원인분석. 미호출 시 null
    "root_cause": { "category": "Machine_Capacity", "evidence": [ ... ] },
    "causal_chain": "Machine_Capacity → queue_depth_now, avg_wait_min_now → 미래 지연 ↑",
    "signal_agreement": "strong",
    "analysis_status": "success"
  },

  "options": [                      // 전략별 카드. 미호출/실패 시 []
    {
      "strategy": "due_date_first",
      "analysisStatus": "success",          // success / fallback
      "fallbackReason": null,
      "recommended": true,                  // 추천 배지
      "summary": "납기 긴급 유닛을 우선 처리하여 누적 지연을 13.71시간 감소시킵니다.",
      "selected": false,                    // 확정된 전략 여부
      "estimatedDelayHrAfter": 3.55,
      "avgWaitTimeMinAfter": 212.9,
      "avgUtilizationRateAfter": 0.56,      // 0~1 (×100 → %)
      "maxWaitTimeMinAfter": 273.3,
      "deadlineViolationCount": 0,
      "afterSchedule": {                    // 스케줄 미리보기 (적용 시 schedule_master 반영)
        "units": [
          { "unit_id": "UNIT-E963B4CA", "steps": [
            { "step_id": "STEP-D693D364B", "start": "2025-05-05T12:34:01",
              "finish": "2025-05-05T14:04:01", "machine_id": "MACHINE-31" } ] }
        ]
      },
      "queueReorder": [                     // 대기열 순서 변경
        { "unit_id": "UNIT-E963B4CA", "queue_id": "Q-EE618C71",
          "original_queue_position": 3, "new_queue_position": 1, "priority_score": 0.47 }
      ]
    }
    // bottleneck_minimization, utilization_balance ...
  ]
}
```

> **fallback 처리:** `analysisStatus == "fallback"` 이면 `afterSchedule`/metrics 가 null 일 수 있음 → 선택 버튼 비활성화 + "재생성" 유도 권장.

---

## 3) 재생성 — `POST /api/reschedule/groups/{groupId}/generate`

에이전트(/run) 를 다시 호출해 재조정안을 채운다. (fallback/실패 시 재시도용)
바디 없음. 응답 `data` 는 **2) 상세와 동일 구조**.

> LLM 호출이라 수 초~2분 소요. 실패 시 `502 RESCHEDULE_GENERATE_FAILED`.

---

## 4) 선택·확정(승인) — `POST /api/reschedule/groups/{groupId}/select`

선택한 전략을 확정하고 **process_queue(순서) + schedule_master(시각/머신)** 에 실제 반영.
그룹 상태가 `approved` 로 바뀐다.

**요청 바디**
```json
{ "strategy": "due_date_first" }
```

**응답 `data`**
```json
{
  "selectionId": "SEL-...",
  "groupId": "GRP-8ebe764e-...",
  "strategy": "due_date_first",
  "status": "applied",
  "selectedAt": "2026-06-10T09:50:00",
  "groupStatus": "approved"
}
```

---

## 에러 코드 (공통 래퍼 `code` / `message`)

| HTTP | 상황 |
|---|---|
| `404` | 존재하지 않는 재조정 그룹 (`RESCHEDULE_GROUP_NOT_FOUND`) |
| `404` | 선택한 strategy 의 재조정안 없음 (`RESCHEDULE_STRATEGY_NOT_FOUND`) |
| `400` | strategy 누락 / 재조정안 미생성(`RESCHEDULE_DETAIL_NOT_READY`) |
| `409` | 만료된 제안 선택 시도 (`RESCHEDULE_EXPIRED`) |
| `502` | 재생성 시 에이전트 호출 실패 (`RESCHEDULE_GENERATE_FAILED`) |

---

## 화면 흐름 요약

1. 목록(`GET /groups?status=active`) → 카드/표 + High/Critical 알림 배지
2. 카드 클릭 → 상세(`GET /groups/{id}`) → `riskAnalysis`(원인) + `options`(전략 3종 비교)
3. 전략 선택 → `POST /groups/{id}/select` → 승인, 스케줄 반영
4. (fallback/빈 options) → `POST /groups/{id}/generate` 로 재생성
