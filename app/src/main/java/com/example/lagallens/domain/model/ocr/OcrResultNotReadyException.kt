package com.example.lagallens.domain.model.ocr

/** The document has no finished text yet: recognition never ran, is still running, or failed. */
class OcrResultNotReadyException : IllegalStateException("OCR result not ready")
