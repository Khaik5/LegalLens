package com.example.lagallens.data.repository

import com.example.lagallens.domain.model.ocr.OcrResult
import com.example.lagallens.domain.model.processing.ProcessingState
import com.example.lagallens.domain.repository.OcrRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Development-only OCR result source; replace with the backend OCR repository when approved. */
class FakeOcrRepository : OcrRepository {
    /** DEMO only: the delays stand in for recognition stages. */
    override fun process(documentId: String): Flow<ProcessingState> = flow {
        delay(STAGE_DURATION_MILLIS)
        emit(ProcessingState.Running(stage = 3))
        delay(STAGE_DURATION_MILLIS)
        emit(ProcessingState.Running(stage = 4))
        delay(STAGE_DURATION_MILLIS)
        emit(ProcessingState.Completed)
    }

    override suspend fun getResult(documentId: String): OcrResult = OcrResult(
        confidenceLabel = "DEMO · Kết quả OCR minh họa, chưa đọc nội dung tệp",
        confidencePercentage = "—",
        documentText = SAMPLE_DOCUMENT_TEXT,
        isSampleContent = true
    )

    private companion object {
        const val STAGE_DURATION_MILLIS = 900L
        val SAMPLE_DOCUMENT_TEXT = """DEMO — BẢN XEM TRƯỚC MẪU HỢP ĐỒNG LAO ĐỘNG
Ứng dụng chưa trích xuất nội dung từ tệp bạn đã chọn. Không dùng phần minh họa này để ký kết.

NGƯỜI SỬ DỤNG LAO ĐỘNG: [TÊN, ĐỊA CHỈ, NGƯỜI ĐẠI DIỆN]
NGƯỜI LAO ĐỘNG: [HỌ TÊN, NGÀY SINH, ĐỊA CHỈ, GIẤY TỜ ĐỊNH DANH]

ĐIỀU 1. CÔNG VIỆC VÀ ĐỊA ĐIỂM LÀM VIỆC
[CHỨC DANH, CÔNG VIỆC, ĐỊA ĐIỂM LÀM VIỆC]

ĐIỀU 2. THỜI HẠN HỢP ĐỒNG
[LOẠI HỢP ĐỒNG, NGÀY BẮT ĐẦU, NGÀY KẾT THÚC NẾU CÓ]

ĐIỀU 3. TIỀN LƯƠNG VÀ THỜI GIỜ LÀM VIỆC
[MỨC LƯƠNG, HÌNH THỨC VÀ KỲ HẠN TRẢ LƯƠNG, PHỤ CẤP, THỜI GIỜ LÀM VIỆC VÀ NGHỈ NGƠI]"""
    }
}
