package com.aura.app.service;

import com.aura.app.dto.InvoiceRequestDto;
import com.aura.app.dto.InvoiceResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface InvoiceService {
    InvoiceResponseDto uploadInvoice(MultipartFile file, Long userId);

    InvoiceResponseDto createManualInvoice(InvoiceRequestDto requestDto);

    List<InvoiceResponseDto> getAllInvoices();

    List<InvoiceResponseDto> getInvoicesForUser(Long userId);

    Map<String, Object> getRevenueSummary();

    List<InvoiceResponseDto> getRevenueByDate(LocalDate start, LocalDate end);

    void deleteInvoice(Long id);
}
