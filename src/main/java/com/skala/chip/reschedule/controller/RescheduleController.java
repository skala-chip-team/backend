package com.skala.chip.reschedule.controller;

import com.skala.chip.reschedule.dto.RescheduleOptionResponse;
import com.skala.chip.reschedule.service.RescheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reschedule/options")
public class RescheduleController {

    private final RescheduleService rescheduleService;

    @PostMapping("/{queueId}")
    public RescheduleOptionResponse generateOption(
            @PathVariable String queueId
    ) {
        return rescheduleService.generateOption(queueId);
    }

    @PostMapping("/approve")
    public RescheduleOptionResponse approveOption(
            @RequestBody RescheduleOptionResponse response
    ) {
        return rescheduleService.approveOption(response);
    }

    @GetMapping("/{optionId}")
    public RescheduleOptionResponse getOption(
            @PathVariable String optionId
    ) {
        return rescheduleService.getOption(optionId);
    }
}