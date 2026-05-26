package com.skala.chip.reschedule.service;

import com.skala.chip.queue.dto.QueueResponse;
import com.skala.chip.queue.repository.QueueRepository;
import com.skala.chip.reschedule.domain.RescheduleOptionTemp;
import com.skala.chip.reschedule.domain.RescheduleQueueItemTemp;
import com.skala.chip.reschedule.dto.RescheduleOptionResponse;
import com.skala.chip.reschedule.dto.RescheduledQueueItem;
import com.skala.chip.reschedule.repository.RescheduleOptionTempRepository;
import com.skala.chip.reschedule.repository.RescheduleQueueItemTempRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class RescheduleService {

    private final QueueRepository queueRepository;
    private final RescheduleOptionTempRepository optionRepository;
    private final RescheduleQueueItemTempRepository itemRepository;

    public RescheduleOptionResponse generateOption(String queueId) {
        List<QueueResponse> allQueues = queueRepository.findCurrentQueues();

        QueueResponse targetQueue = allQueues.stream()
                .filter(q -> q.queueId().equals(queueId))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 queueId입니다: " + queueId)
                );

        List<QueueResponse> sameQueueGroup = allQueues.stream()
                .filter(q -> q.stepId().equals(targetQueue.stepId()))
                .filter(q -> q.districtId().equals(targetQueue.districtId()))
                .sorted(Comparator.comparing(QueueResponse::queuePosition))
                .toList();

        List<RescheduledQueueItem> beforeQueue = sameQueueGroup.stream()
                .map(q -> new RescheduledQueueItem(
                        q.queueId(),
                        q.unitId(),
                        q.stepId(),
                        q.districtId(),
                        q.queuePosition(),
                        q.queuePosition(),
                        0.0,
                        q.enqueueTime(),
                        "현재 큐 순서"
                ))
                .toList();

        List<QueueResponse> reorderedQueue = sameQueueGroup.stream()
                .sorted((a, b) -> {
                    if (a.queueId().equals(queueId)) return -1;
                    if (b.queueId().equals(queueId)) return 1;
                    return Integer.compare(a.queuePosition(), b.queuePosition());
                })
                .toList();

        List<RescheduledQueueItem> afterQueue = IntStream.range(0, reorderedQueue.size())
                .mapToObj(i -> {
                    QueueResponse q = reorderedQueue.get(i);

                    return new RescheduledQueueItem(
                            q.queueId(),
                            q.unitId(),
                            q.stepId(),
                            q.districtId(),
                            q.queuePosition(),
                            i + 1,
                            q.queueId().equals(queueId) ? 100.0 : 0.0,
                            q.enqueueTime(),
                            q.queueId().equals(queueId)
                                    ? "위험 큐로 판단되어 동일 공정/구역 큐 내 최우선 순위로 조정"
                                    : "대상 큐 이동에 따른 순위 조정"
                    );
                })
                .toList();

        String optionId = "OPT-" + System.currentTimeMillis();
        LocalDateTime createdAt = LocalDateTime.now();

        return new RescheduleOptionResponse(
                optionId,
                queueId,
                "QUEUE_REORDER",
                "PROPOSED",
                createdAt,
                beforeQueue,
                afterQueue
        );
    }

    public RescheduleOptionResponse approveOption(RescheduleOptionResponse response) {
        optionRepository.save(
                RescheduleOptionTemp.builder()
                        .optionId(response.optionId())
                        .targetQueueId(response.targetQueueId())
                        .perspective(response.perspective())
                        .status("APPROVED")
                        .createdAt(response.createdAt())
                        .build()
        );

        itemRepository.saveAll(
                response.afterQueue().stream()
                        .map(item -> RescheduleQueueItemTemp.builder()
                                .optionId(response.optionId())
                                .queueId(item.queueId())
                                .unitId(item.unitId())
                                .stepId(item.stepId())
                                .districtId(item.districtId())
                                .beforePosition(item.beforePosition())
                                .afterPosition(item.afterPosition())
                                .score(item.score())
                                .reason(item.reason())
                                .build()
                        )
                        .toList()
        );

        return new RescheduleOptionResponse(
                response.optionId(),
                response.targetQueueId(),
                response.perspective(),
                "APPROVED",
                response.createdAt(),
                response.beforeQueue(),
                response.afterQueue()
        );
    }

    public RescheduleOptionResponse getOption(String optionId) {
        RescheduleOptionTemp option = optionRepository.findById(optionId)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 optionId입니다: " + optionId)
                );

        List<RescheduledQueueItem> afterQueue = itemRepository.findByOptionId(optionId)
                .stream()
                .map(item -> new RescheduledQueueItem(
                        item.getQueueId(),
                        item.getUnitId(),
                        item.getStepId(),
                        item.getDistrictId(),
                        item.getBeforePosition(),
                        item.getAfterPosition(),
                        item.getScore(),
                        null,
                        item.getReason()
                ))
                .toList();

        return new RescheduleOptionResponse(
                option.getOptionId(),
                option.getTargetQueueId(),
                option.getPerspective(),
                option.getStatus(),
                option.getCreatedAt(),
                List.of(),
                afterQueue
        );
    }
}