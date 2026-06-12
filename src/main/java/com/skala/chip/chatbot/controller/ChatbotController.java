package com.skala.chip.chatbot.controller;

import com.skala.chip.chatbot.dto.ChatbotRequestDTO;
import com.skala.chip.chatbot.dto.ChatbotResponseDTO;
import com.skala.chip.chatbot.service.ChatbotService;
import com.skala.chip.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 챗봇 API 컨트롤러.
 *
 * 프론트엔드 ↔ 챗봇 에이전트(ai_agent /chat) 사이를 중계한다.
 * SecurityConfig 에서 permitAll 목록에 없으므로 JWT 인증이 필요하며,
 * 인증 주체(email)에서 user_id 를 확정해 에이전트로 주입한다. (본문에는 user_id 를 받지 않는다)
 */
@Tag(name = "Chatbot", description = "재조정 챗봇 API")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    /**
     * 챗봇 메시지 전송.
     *
     * 프론트의 질문을 에이전트로 중계하고 답변(answer)·세션 ID·호출 도구 목록을 반환한다.
     *
     * @param email   JWT 인증 주체(이메일). SecurityContext 의 principal 에서 주입된다.
     * @param request groupId, sessionId?, message, refTime?
     */
    @Operation(summary = "챗봇 메시지 전송",
            description = "프론트엔드의 질문을 챗봇 에이전트로 중계하고 답변을 반환한다. "
                    + "user_id 는 JWT 에서 추출하므로 요청 본문에 포함하지 않는다.")
    @PostMapping("/messages")
    public ApiResponse<ChatbotResponseDTO.MessageResult> sendMessage(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody ChatbotRequestDTO.SendMessage request) {
        return ApiResponse.success(chatbotService.sendMessage(email, request));
    }
}
