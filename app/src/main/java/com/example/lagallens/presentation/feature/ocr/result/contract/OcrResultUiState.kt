package com.example.lagallens.presentation.feature.ocr.result.contract

data class OcrResultUiState(
    val confidenceLabel: String = "Độ tin cậy OCR rất cao",
    val confidencePercentage: String = "94.5%",
    val documentText: String = OcrResultSample.documentText,
    val documentName: String = "",
    val documentDetails: String = "",
    val isReviewConfirmed: Boolean = false
)

object OcrResultSample {
    val documentText = """Hôm nay, ngày 24 tháng 10 năm 2026, tại tòa nhà Bitexco Financial Tower, chúng tôi gồm có:

BÊN CHO THUÊ (BÊN A):
- Công ty Cổ phần Đầu tư Phát triển Bitexco
- Đại diện bởi: Ông Nguyễn Phi Long
- Chức vụ: Giám đốc Điều hành

BÊN THUÊ (BÊN B):
- Công ty TNHH Công nghệ LegalLens Việt Nam
- Đại diện bởi: Bà Trần Thị Mai
- Chức vụ: Giám đốc Kỹ thuật

ĐIỀU 1: TÀI SẢN CHO THUÊ & GIÁ CẢ
1. Bên A đồng ý cho Bên B thuê văn phòng tại Tầng 24, tòa nhà Bitexco Financial Tower, Quận 1, TP. Hồ Chí Minh.
2. Diện tích sử dụng thực tế: 150 m2.
3. Đơn giá thuê cố định là 45 USD / m2 / tháng (Chưa bao gồm VAT và phí quản lý dịch vụ). Tổng giá trị hợp đồng thanh toán định kỳ mỗi 03 tháng."""
}
