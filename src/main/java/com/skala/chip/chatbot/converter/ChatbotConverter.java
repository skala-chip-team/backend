package com.skala.chip.chatbot.converter;

import com.skala.chip.chatbot.client.InferRequest;
import com.skala.chip.chatbot.client.InferResponse;
import com.skala.chip.chatbot.domain.ChatbotMessage;
import com.skala.chip.chatbot.dto.ChatbotResponseDTO;
import com.skala.chip.chatbot.repository.ChatbotSessionSummaryProjection;

import java.util.List;

/**
 * 챗봇 DTO ↔ AI 통신 모델 ↔ 엔티티 변환기.
 *
 * 변환 로직은 서비스가 아닌 Converter 에 둔다(코드 컨벤션). 정적 메서드만 제공한다.
 */
public class ChatbotConverter {

    private ChatbotConverter() {
    }

    /** group_id + 현재 메시지 + 이력(시간순) → AI 추론 요청. */
    public static InferRequest toInferRequest(String groupId, String message, List<ChatbotMessage> history) {
        List<InferRequest.Turn> turns = history.stream()
                .map(m -> InferRequest.Turn.builder()
                        .role(m.getMessageType())
                        .content(m.getContent())
                        .build())
                .toList();
        return InferRequest.builder()
                .groupId(groupId)
                .message(message)
                .history(turns)
                .build();
    }

    /** AI 추론 응답 + 세션 ID → 프론트 응답. */
    public static ChatbotResponseDTO.MessageResult toResponse(String sessionId, InferResponse response) {
        List<ChatbotResponseDTO.Source> sources = response.getSources() == null
                ? List.of()
                : response.getSources().stream().map(ChatbotConverter::toSource).toList();
        return ChatbotResponseDTO.MessageResult.builder()
                .sessionId(sessionId)
                .answer(response.getAnswer())
                .toolCalls(response.getToolCalls())
                .sources(sources)
                .build();
    }

    /** AI 출처 → 프론트 출처 DTO. */
    public static ChatbotResponseDTO.Source toSource(InferResponse.Source s) {
        return ChatbotResponseDTO.Source.builder()
                .groupId(s.getGroupId())
                .optionId(s.getOptionId())
                .strategy(s.getStrategy())
                .similarity(s.getSimilarity())
                .approvalStatus(s.getApprovalStatus())
                .selectionId(s.getSelectionId())
                .selectedAt(s.getSelectedAt())
                .build();
    }

    /** 세션 목록 프로젝션 → 세션 요약 DTO. */
    public static ChatbotResponseDTO.SessionSummary toSessionSummary(ChatbotSessionSummaryProjection p) {
        return ChatbotResponseDTO.SessionSummary.builder()
                .sessionId(p.getSessionId())
                .startedAt(p.getStartedAt())
                .endedAt(p.getEndedAt())
                .messageCount(p.getMessageCount())
                .build();
    }

    /** 메시지 엔티티 → 메시지 DTO. */
    public static ChatbotResponseDTO.MessageDetail toMessageDetail(ChatbotMessage m) {
        return ChatbotResponseDTO.MessageDetail.builder()
                .messageId(m.getMessageId())
                .messageType(m.getMessageType())
                .content(m.getContent())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
