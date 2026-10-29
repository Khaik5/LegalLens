package com.example.lagallens.data.repository

import com.example.lagallens.domain.model.ocr.OcrResult
import com.example.lagallens.domain.repository.OcrRepository

/** Development-only OCR result source; replace with the backend OCR repository when approved. */
class FakeOcrRepository : OcrRepository {
    override suspend fun getResult(documentName: String, documentDetails: String): OcrResult = OcrResult(
        confidenceLabel = "DEMO · Kết quả OCR minh họa, chưa đọc nội dung tệp",
        confidencePercentage = "—",
        documentText = SAMPLE_DOCUMENT_TEXT
    )

    private companion object {
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
