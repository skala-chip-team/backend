package com.skala.chip.order.controller;

import com.skala.chip.common.ApiResponse;
import com.skala.chip.order.dto.OrderResponseDTO;
import com.skala.chip.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "주문 목록 조회",
            description = "납기 오름차순 주문 목록. status(대기/진행중/완료) 와 districtId 로 필터링할 수 있다.")
    @GetMapping
    public ApiResponse<OrderResponseDTO.OrderList> getOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String districtId
    ) {
        return ApiResponse.success(orderService.getOrders(status, districtId));
    }

    @Operation(summary = "주문 상세 조회",
            description = "유닛별 STEP A~D 타임라인을 포함한 주문 상세.")
    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponseDTO.OrderDetail> getOrderDetail(
            @PathVariable String orderId
    ) {
        return ApiResponse.success(orderService.getOrderDetail(orderId));
    }
}
