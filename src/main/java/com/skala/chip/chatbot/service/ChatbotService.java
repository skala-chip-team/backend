package com.skala.chip.chatbot.service;

import com.skala.chip.chatbot.dto.ChatbotRequestDTO;
import com.skala.chip.chatbot.dto.ChatbotResponseDTO;

import java.util.List;

/**
 * 챗봇 서비스.
 *
 * 메시지 전송은 챗봇 에이전트(ai_agent /chat)로 중계하고, 대화 이력 조회는
 * 공유 DB(tt_chatbot_session / td_chatbot_message)를 직접 읽는다.
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

    /**
     * 현재 사용자의 챗봇 세션 목록(최신순)을 조회한다.
     *
     * @param email JWT 인증 주체(이메일)
     */
    List<ChatbotResponseDTO.SessionSummary> getSessions(String email);

    /**
     * 특정 세션의 대화 메시지를 시간순으로 조회한다.
     * 세션 소유자가 현재 사용자가 아니면 거부한다(403).
     *
     * @param email     JWT 인증 주체(이메일)
     * @param sessionId 조회할 세션 ID
     */
    List<ChatbotResponseDTO.MessageDetail> getMessages(String email, String sessionId);
}
