package com.example.lagallens.domain.usecase

import com.example.lagallens.domain.model.chatbot.LegalArticleText
import com.example.lagallens.domain.repository.ChatbotRepository

class GetLegalArticleTextUseCase(private val repository: ChatbotRepository) {
    suspend operator fun invoke(articleId: Long): LegalArticleText? = repository.legalArticleText(articleId)
}
