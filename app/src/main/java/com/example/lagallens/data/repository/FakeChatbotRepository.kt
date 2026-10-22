package com.example.lagallens.data.repository

import com.example.lagallens.domain.model.chatbot.ChatbotCitation
import com.example.lagallens.domain.model.chatbot.ChatbotConversation
import com.example.lagallens.domain.model.chatbot.ChatbotReference
import com.example.lagallens.domain.model.chatbot.ChatbotReply
import com.example.lagallens.domain.repository.ChatbotRepository

/**
 * Development-only conversation source. Replace this implementation with the
 * authenticated backend repository once the chat API contract is approved.
 */
class FakeChatbotRepository : ChatbotRepository {
    override suspend fun openConversation(
        contractName: String,
        contractType: String
    ): ChatbotConversation = ChatbotConversation(
        id = LOCAL_CONVERSATION_ID,
        greeting = "Xin chào! Tôi đã sẵn sàng hỗ trợ. Dưới đây là một số câu hỏi gợi ý phù hợp nhất với loại hợp đồng thuê văn phòng này.",
        initialSuggestions = listOf(
            "Thời hạn hợp đồng là bao lâu?",
            "Lương được quy định như thế nào?",
            "Có điều khoản nào cần lưu ý?",
            "Quyền và nghĩa vụ của tôi là gì?"
        ),
        newConversationSuggestions = listOf(
            "Có điều khoản rủi ro nào cần sửa đổi?",
            "Phân tích nghĩa vụ thanh toán của tôi",
            "Chính sách phạt vi phạm như thế nào?"
        )
    )

    override suspend fun sendMessage(conversationId: String, message: String): ChatbotReply = when (message) {
        CITATION_QUESTION -> ChatbotReply.CitationAnswer(
            content = "Theo Điều 8.2 của hợp đồng, mức phạt vi phạm được thỏa thuận là 15% tổng giá trị hợp đồng.\n\nTuy nhiên, lưu ý rằng điều khoản này đang có rủi ro cao vì vượt quá mức trần 8% quy định bởi Luật Thương mại.",
            citation = ChatbotCitation(
                title = "Điều 301 · Luật Thương mại 2005",
                description = "Mức phạt vi phạm nghĩa vụ hợp đồng thương mại không quá 8% giá trị phần nghĩa vụ bị vi phạm.",
                actionLabel = "Xem chi tiết văn bản luật"
            )
        )

        GROUNDED_QUESTION -> ChatbotReply.GroundedAnswer(
            content = "Hợp đồng có thể chấm dứt trong hai trường hợp chính:\n1. Hết thời hạn 36 tháng mà không gia hạn [1].\n2. Đơn phương chấm dứt do vi phạm nghĩa vụ thanh toán kéo dài trên 30 ngày [2].",
            referenceSectionTitle = "Tài liệu dẫn chiếu (2)",
            references = listOf(
                ChatbotReference(
                    title = "[1] HĐ Bitexco · Điều 3.2",
                    excerpt = "“Hợp đồng tự động chấm dứt khi hết thời hạn quy định tại Điều 3.1...”"
                ),
                ChatbotReference(
                    title = "[2] HĐ Bitexco · Điều 11.4",
                    excerpt = "“Bên cho thuê có quyền đơn phương chấm dứt nếu Bên thuê trễ hạn...”"
                )
            )
        )

        NO_EVIDENCE_QUESTION -> ChatbotReply.NoEvidenceAnswer(
            title = "Không đủ căn cứ trả lời",
            content = "Không đủ căn cứ từ tài liệu hiện có để trả lời chắc chắn câu hỏi này. Hợp đồng hiện tại chỉ đề cập đến phí dịch vụ chung mà không chi tiết hóa danh mục đỗ xe. Vui lòng tham khảo ý kiến luật sư."
        )

        ERROR_QUESTION -> ChatbotReply.RetryableError(
            title = "Không thể kết nối. Vui lòng thử lại.",
            retryLabel = "Thử lại"
        )

        else -> ChatbotReply.Answer(LOCAL_RESPONSE)
    }

    override suspend fun retryMessage(conversationId: String, message: String): ChatbotReply =
        ChatbotReply.Answer(LOCAL_RESPONSE)

    private companion object {
        const val LOCAL_CONVERSATION_ID = "local-chatbot-conversation"
        const val CITATION_QUESTION = "Mức phạt vi phạm hợp đồng được quy định thế nào?"
        const val GROUNDED_QUESTION = "Điều khoản chấm dứt hợp đồng gồm những gì?"
        const val NO_EVIDENCE_QUESTION = "Hợp đồng này có hỗ trợ chi phí đỗ xe ô tô không?"
        const val ERROR_QUESTION = "Tóm tắt nghĩa vụ của bên thuê"
        const val LOCAL_RESPONSE = "Tôi đã ghi nhận câu hỏi của bạn. Đây là phản hồi mẫu trong khi dữ liệu hội thoại theo hợp đồng chưa được kết nối với hệ thống."
    }
}
