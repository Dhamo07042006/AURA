package com.aura.app.service;

import com.aura.app.dto.InvoiceRequestDto;
import org.springframework.web.multipart.MultipartFile;

public interface PdfParserService {

    InvoiceRequestDto parseInvoice(MultipartFile file);
}
