package com.skala.chip.order.service;

import com.skala.chip.order.dto.OrderResponseDTO;

public interface OrderService {

    /**
     * 주문 목록 조회. status / districtId 는 옵션 필터(null 이면 미적용).
     */
    OrderResponseDTO.OrderList getOrders(String status, String districtId);

    /**
     * 주문 상세 조회. 존재하지 않으면 OrderNotFoundException.
     */
    OrderResponseDTO.OrderDetail getOrderDetail(String orderId);
}
