package com.example.lagallens.presentation.feature.ocr.contract

data class OcrReviewUiState(
    val text: String = OcrReviewSample.editedText,
    val isOriginalText: Boolean = false
)

object OcrReviewSample {
    val originalText = """BÊN SỬ DỤNG LAO ĐỘNG (BÊN A):
- Công ty [Tên doanh nghiệp]
- Đại diện bởi: [Người đại diện]
- Chức vụ: [Chức danh]

NGƯỜI LAO ĐỘNG (BÊN B):
- Họ và tên: [Họ tên người lao động]
- Vị trí: Nhân viên hành chính

ĐIỀU 1: CÔNG VIỆC VÀ THỜI HẠN HỢP ĐỒNG
1. Bên B làm việc tại [Địa điểm làm việc].
2. Thời hạn hợp đồng: 12 tháng."""

    val editedText = """BÊN SỬ DỤNG LAO ĐỘNG (BÊN A):
- Công ty [Tên doanh nghiệp]
- Đại diện bởi: [Người đại diện]
- Chức vụ: [Chức danh]

NGƯỜI LAO ĐỘNG (BÊN B):
- Họ và tên: [Họ tên người lao động] (Độ tin cậy 65%)
- Vị trí: Nhân viên hành chính

ĐIỀU 1: CÔNG VIỆC VÀ THỜI HẠN HỢP ĐỒNG
1. Bên B làm việc tại [Địa điểm làm việc] (Độ tin cậy 58%).
2. Thời hạn hợp đồng: 12 tháng."""
}
