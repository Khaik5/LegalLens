package com.example.lagallens.presentation.feature.upload.error.contract

data class UploadErrorUiState(
    val errorCode: String = DEFAULT_ERROR_CODE,
    val errorMessage: String = DEFAULT_ERROR_MESSAGE
) {
    companion object {
        const val DEFAULT_ERROR_CODE = "OCR_READ_FAILED"
        const val DEFAULT_ERROR_MESSAGE =
            "Tệp PDF quét (Scan) có chất lượng hình ảnh quá thấp, mờ hoặc bị mất góc chữ không thể chuyển đổi số."
    }
}
