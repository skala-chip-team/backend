package com.skala.chip.queue.controller;

import com.skala.chip.queue.dto.QueueResponse;
import com.skala.chip.queue.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/queues")
public class QueueController {

    private final QueueService queueService;

    @GetMapping
    public List<QueueResponse> getQueues() {
        return queueService.getCurrentQueues();
    }
}