package com.skala.chip.queue.repository;

import com.skala.chip.monitoring.domain.ProcessQueue;
import com.skala.chip.queue.dto.QueueResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QueueRepository extends JpaRepository<ProcessQueue, String> {

    @Query("""
        SELECT new com.skala.chip.queue.dto.QueueResponse(
            q.queueId,
            q.stepId,
            q.stepId,
            q.district.districtId,
            q.unit.unitId,
            q.queuePosition,
            q.enqueueTime,
            null,
            null,
            null
        )
        FROM ProcessQueue q
        ORDER BY q.queuePosition
    """)
    List<QueueResponse> findCurrentQueues();
}