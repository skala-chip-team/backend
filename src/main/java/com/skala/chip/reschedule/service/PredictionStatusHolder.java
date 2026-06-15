package com.skala.chip.reschedule.service;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 마지막 지연 예측(/predict) 시도의 상태를 보관한다(인메모리).
 *
 * 대시보드가 "예측 시스템 상태"를 표시할 수 있도록, 예측 결과를 다음으로 구분해 기록한다.
 *  - SUCCESS               : 예측 성공(신규 위험 inserted > 0)
 *  - SKIPPED_INSUFFICIENT  : 입력 데이터 부족 → 예측 미수행(inserted == 0). 다음 주기에 재시도.
 *  - FAILED                : 모델 추론 실패(예외). 메시지를 함께 보관한다.
 *  - NONE                  : 아직 예측 시도 기록 없음.
 *
 * 값은 재시작 시 초기화되며, 다음 예측 시도 때 다시 채워진다.
 */
@Component
public class PredictionStatusHolder {

    public enum Status { NONE, SUCCESS, SKIPPED_INSUFFICIENT, FAILED }

    private volatile Status status = Status.NONE;
    private volatile String message;
    private volatile Integer insertedCount;
    private volatile LocalDateTime attemptedAt;

    public synchronized void recordSuccess(int inserted) {
        this.status = Status.SUCCESS;
        this.message = null;
        this.insertedCount = inserted;
        this.attemptedAt = LocalDateTime.now();
    }

    public synchronized void recordInsufficient() {
        this.status = Status.SKIPPED_INSUFFICIENT;
        this.message = "입력 데이터가 부족하여 예측을 수행하지 않았습니다. 다음 주기에 재시도합니다.";
        this.insertedCount = 0;
        this.attemptedAt = LocalDateTime.now();
    }

    public synchronized void recordFailure(String error) {
        this.status = Status.FAILED;
        this.message = "모델 추론에 실패했습니다: " + (error == null ? "알 수 없는 오류" : error);
        this.insertedCount = null;
        this.attemptedAt = LocalDateTime.now();
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Integer getInsertedCount() {
        return insertedCount;
    }

    public LocalDateTime getAttemptedAt() {
        return attemptedAt;
    }
}
