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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 챗봇 API 컨트롤러.
 *
 * 세션 lifecycle·소유권·메시지 저장은 백엔드가 소유하고, AI 서버(/infer)는 무상태 추론만 한다.
 * SecurityConfig 에서 permitAll 목록에 없으므로 JWT 인증이 필요하며,
 * 인증 주체(email)에서 user_id 를 확정한다. (본문에는 user_id 를 받지 않는다)
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
     * 세션 확인/생성 → 이력 조회 → AI 추론(/infer) → user+assistant 한 트랜잭션 저장.
     * 답변(answer)·세션 ID·도구호출·출처(sources)를 반환한다.
     *
     * @param email   JWT 인증 주체(이메일). SecurityContext 의 principal 에서 주입된다.
     * @param request groupId?, sessionId?, message (group/session 조합 규칙은 서비스에서 검증)
     */
    @Operation(summary = "챗봇 메시지 전송",
            description = "세션을 백엔드가 관리하고 AI(/infer)로 추론한다. "
                    + "user_id 는 JWT 에서 추출하므로 요청 본문에 포함하지 않는다.")
    @PostMapping("/messages")
    public ApiResponse<ChatbotResponseDTO.MessageResult> sendMessage(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody ChatbotRequestDTO.SendMessage request) {
        return ApiResponse.success(chatbotService.sendMessage(email, request));
    }

    /**
     * 내 챗봇 세션 목록 조회 (최신순). 화면에서 지난 대화 목록을 그릴 때 사용한다.
     */
    @Operation(summary = "챗봇 세션 목록 조회",
            description = "현재 로그인 사용자의 챗봇 대화 세션 목록을 최신순으로 반환한다.")
    @GetMapping("/sessions")
    public ApiResponse<List<ChatbotResponseDTO.SessionSummary>> getSessions(
            @AuthenticationPrincipal String email) {
        return ApiResponse.success(chatbotService.getSessions(email));
    }

    /**
     * 특정 세션의 대화 내역 조회 (시간순). 화면에서 지난 대화를 복원할 때 사용한다.
     * 다른 사용자의 세션이면 403.
     */
    @Operation(summary = "챗봇 세션 대화 내역 조회",
            description = "지정한 세션의 메시지를 시간순으로 반환한다. 본인 세션이 아니면 403.")
    @GetMapping("/sessions/{sessionId}/messages")
    public ApiResponse<List<ChatbotResponseDTO.MessageDetail>> getMessages(
            @AuthenticationPrincipal String email,
            @PathVariable String sessionId) {
        return ApiResponse.success(chatbotService.getMessages(email, sessionId));
    }
}
