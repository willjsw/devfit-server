package com.amcamp.domain.feedback.dto.request;

import com.amcamp.domain.feedback.dto.Message;

public record ChatRequest(String model, Message[] messages) {
    public static ChatRequest of(String model, String userMessage) {
        return new ChatRequest(
                model,
                new Message[] {
                    new Message(
                            "system",
                            """
                    당신은 팀 프로젝트의 동료 평가를 위한 피드백 메시지를 순화하는 역할을 맡고 있습니다.
					사용자가 제공한 피드백은 그대로 전달하기엔 다소 직설적이거나 부정적인 뉘앙스를 가질 수 있습니다. 이 메시지를 보다 부드럽고 건설적인 표현으로 다듬어 주세요.
					출력은 사용자가 최종적으로 보낼 메시지입니다. 별도의 설명이나 추가 문장은 포함하지 마세요.

                    - 피드백의 핵심 의도는 유지해주세요.
				    - 상대방이 기분 나쁘지 않게 개선점을 전달해주세요.
				    - 너무 딱딱하거나 로봇처럼 느껴지지 않도록 자연스럽게 표현해주세요.
				    - 이미 긍정적인 메시지라면, 조금 더 따뜻하게 다듬어주시면 됩니다.

				    예시:
				    "항상 고마워요" → "항상 도움을 아끼지 않아줘서 고마워요. 덕분에 이번 스프린트가 더 원활했던 것 같아요."
				    "이번엔 역할이 부족했던 것 같아요." → "이번엔 조금 어려움이 있었던 것 같아요. 다음엔 더 잘 맞춰갈 수 있으면 좋겠습니다."
                """),
                    new Message("user", userMessage)
                });
    }
}
