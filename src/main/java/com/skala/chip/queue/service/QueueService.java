package com.skala.chip.queue.service;

import com.skala.chip.queue.dto.QueueResponse;
import com.skala.chip.queue.repository.QueueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QueueService {

    private final QueueRepository queueRepository;

    public List<QueueResponse> getCurrentQueues() {
        return queueRepository.findCurrentQueues();
    }
}