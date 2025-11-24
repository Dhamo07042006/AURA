package com.aura.app.service.impl;

import com.aura.app.dto.InvoiceRequestDto;
import com.aura.app.dto.InvoiceResponseDto;
import com.aura.app.model.Invoice;
import com.aura.app.repository.InvoiceRepository;
import com.aura.app.service.InvoiceService;
import com.aura.app.service.PdfParserService;
import com.aura.app.utils.CsvWriterUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final PdfParserService pdfParserService;

    public InvoiceServiceImpl(InvoiceRepository invoiceRepository, PdfParserService pdfParserService) {
        this.invoiceRepository = invoiceRepository;
        this.pdfParserService = pdfParserService;
    }

    @Override
    public InvoiceResponseDto uploadInvoice(MultipartFile file, Long userId) {
        // Parse uploaded file to a single invoice request (PDF or CSV)
        List<InvoiceRequestDto> requestDtos = parseFileToInvoices(file);
        if (requestDtos.isEmpty()) {
            throw new IllegalArgumentException("No invoice data found in uploaded file");
        }

        InvoiceRequestDto dto = requestDtos.get(0);
        if (userId != null) {
            dto.setUserId(userId);
        }
        // Ensure amountWithoutGst is populated when we have GST and total
        backfillAmounts(dto);
        Invoice invoice = toEntity(dto);
        Invoice saved = invoiceRepository.save(invoice);

        // Write to CSV file
        java.nio.file.Path csvPath = CsvWriterUtil.appendInvoiceToCsv(dto);

        InvoiceResponseDto responseDto = toResponseDto(saved);
        responseDto.setCsvPath("/output/invoices_parsed.csv");
        return responseDto;
    }

    @Override
    public InvoiceResponseDto createManualInvoice(InvoiceRequestDto requestDto) {
        // In case client omits total or amountWithoutGst, normalize on server
        backfillAmounts(requestDto);
        Invoice invoice = toEntity(requestDto);
        Invoice saved = invoiceRepository.save(invoice);
        return toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponseDto> getAllInvoices() {
        return invoiceRepository.findAll().stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getRevenueSummary() {
        List<Invoice> invoices = invoiceRepository.findAll();
        BigDecimal totalRevenue = invoices.stream()
                .map(Invoice::getTotalAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalGst = invoices.stream()
                .map(Invoice::getGstAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Integer, BigDecimal> monthlyRevenue = new HashMap<>();
        Map<Integer, BigDecimal> yearlyRevenue = new HashMap<>();

        for (Invoice invoice : invoices) {
            if (invoice.getInvoiceDate() == null || invoice.getTotalAmount() == null) {
                continue;
            }
            int month = invoice.getInvoiceDate().getMonthValue();
            int year = invoice.getInvoiceDate().getYear();

            monthlyRevenue.merge(month, invoice.getTotalAmount(), BigDecimal::add);
            yearlyRevenue.merge(year, invoice.getTotalAmount(), BigDecimal::add);
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalRevenue", totalRevenue);
        summary.put("totalGst", totalGst);
        summary.put("monthlyRevenue", monthlyRevenue);
        summary.put("yearlyRevenue", yearlyRevenue);
        return summary;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponseDto> getRevenueByDate(LocalDate start, LocalDate end) {
        return invoiceRepository.findByInvoiceDateBetween(start, end).stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponseDto> getInvoicesForUser(Long userId) {
        List<Invoice> invoices = invoiceRepository.findByUserId(userId);

        invoices.sort((a, b) -> {
            LocalDate da = a.getInvoiceDate();
            LocalDate db = b.getInvoiceDate();
            if (da == null && db == null) return 0;
            if (da == null) return 1; // nulls last
            if (db == null) return -1;
            return da.compareTo(db);
        });

        return invoices.stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteInvoice(Long id) {
        invoiceRepository.deleteById(id);
    }

    private List<InvoiceRequestDto> parseFileToInvoices(MultipartFile file) {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase() : "";

        // Treat as CSV if content type or extension suggests so
        if (contentType.contains("csv") || filename.endsWith(".csv")) {
            return parseCsv(file);
        }

        // Default: parse as PDF (existing behavior)
        InvoiceRequestDto parsed = pdfParserService.parseInvoice(file);
        List<InvoiceRequestDto> list = new ArrayList<>();
        list.add(parsed);
        return list;
    }

    private List<InvoiceRequestDto> parseCsv(MultipartFile file) {
        List<InvoiceRequestDto> result = new ArrayList<>();
        DateTimeFormatter[] formatters = new DateTimeFormatter[]{
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ISO_LOCAL_DATE
        };

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) {
                    // Skip header row
                    first = false;
                    continue;
                }
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] parts = line.split(",");
                if (parts.length < 5) {
                    // Expect at least: invoiceDate, metalType, amountWithoutGst, gstAmount, totalAmount
                    continue;
                }

                InvoiceRequestDto dto = new InvoiceRequestDto();

                // Optional userId column at index 0; if present shift others right
                int idx = 0;
                Long userId = null;
                try {
                    userId = Long.parseLong(parts[0].trim());
                    idx = 1;
                } catch (NumberFormatException ignored) {
                    // No user id column, treat first column as date
                    idx = 0;
                }

                if (userId != null) {
                    dto.setUserId(userId);
                }

                // Date
                if (idx < parts.length) {
                    String dateStr = parts[idx++].trim();
                    dto.setInvoiceDate(parseDateFlexible(dateStr, formatters));
                }

                // Metal type
                if (idx < parts.length) {
                    dto.setMetalType(parts[idx++].trim());
                }

                // Amounts
                if (idx < parts.length) {
                    dto.setAmountWithoutGst(parseBigDecimalSafe(parts[idx++]));
                }
                if (idx < parts.length) {
                    dto.setGstAmount(parseBigDecimalSafe(parts[idx++]));
                }
                if (idx < parts.length) {
                    dto.setTotalAmount(parseBigDecimalSafe(parts[idx]));
                }

                result.add(dto);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse CSV invoice file", e);
        }

        return result;
    }

    private LocalDate parseDateFlexible(String value, DateTimeFormatter[] formatters) {
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private BigDecimal parseBigDecimalSafe(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim().replaceAll(",", "");
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Invoice toEntity(InvoiceRequestDto dto) {
        Invoice invoice = new Invoice();
        invoice.setUserId(dto.getUserId());
        invoice.setInvoiceDate(dto.getInvoiceDate());
        invoice.setMetalType(dto.getMetalType());
        invoice.setAmountWithoutGst(dto.getAmountWithoutGst());
        invoice.setGstAmount(dto.getGstAmount());
        invoice.setTotalAmount(dto.getTotalAmount());
        return invoice;
    }

    private InvoiceResponseDto toResponseDto(Invoice invoice) {
        InvoiceResponseDto dto = new InvoiceResponseDto();
        dto.setId(invoice.getId());
        dto.setUserId(invoice.getUserId());
        dto.setInvoiceDate(invoice.getInvoiceDate());
        dto.setMetalType(invoice.getMetalType());
        dto.setAmountWithoutGst(invoice.getAmountWithoutGst());
        dto.setGstAmount(invoice.getGstAmount());
        dto.setTotalAmount(invoice.getTotalAmount());
        dto.setCreatedAt(invoice.getCreatedAt());
        dto.setUpdatedAt(invoice.getUpdatedAt());
        return dto;
    }

    /**
     * Backfill missing monetary fields when enough data is present.
     * If amountWithoutGst is null but total and GST are present, compute: amountWithoutGst = total - GST.
     */
    private void backfillAmounts(InvoiceRequestDto dto) {
        if (dto == null) {
            return;
        }

        BigDecimal amountWithoutGst = dto.getAmountWithoutGst();
        BigDecimal gstAmount = dto.getGstAmount();
        BigDecimal totalAmount = dto.getTotalAmount();

        if (amountWithoutGst == null && gstAmount != null && totalAmount != null) {
            try {
                BigDecimal computed = totalAmount.subtract(gstAmount);
                dto.setAmountWithoutGst(computed);
            } catch (ArithmeticException ignored) {
                // Leave as null if subtraction fails for some reason
            }
        }
    }
}
