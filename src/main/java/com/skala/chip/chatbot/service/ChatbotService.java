package com.skala.chip.chatbot.service;

import com.skala.chip.chatbot.dto.ChatbotRequestDTO;
import com.skala.chip.chatbot.dto.ChatbotResponseDTO;

/**
 * 챗봇 메시지 중계 서비스.
 *
 * 프론트엔드의 질문을 챗봇 에이전트(ai_agent /chat)로 중계하고 답변을 반환한다.
 */
public interface ChatbotService {

    /**
     * 챗봇 메시지를 에이전트로 전달하고 답변을 받는다.
     *
     * @param email   JWT 인증 주체(이메일). 이 값으로 user_id 를 조회해 에이전트에 주입한다.
     * @param request 프론트 요청(groupId, sessionId?, message, refTime?)
     * @return 세션 ID + 답변 + 호출된 도구 목록
     */
    ChatbotResponseDTO.MessageResult sendMessage(String email, ChatbotRequestDTO.SendMessage request);
}
