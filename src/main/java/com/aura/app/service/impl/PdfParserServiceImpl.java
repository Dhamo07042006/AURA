package com.aura.app.service.impl;

import com.aura.app.dto.InvoiceRequestDto;
import com.aura.app.service.PdfParserService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PdfParserServiceImpl implements PdfParserService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH);

    @Override
    public InvoiceRequestDto parseInvoice(MultipartFile file) {
        try (InputStream is = file.getInputStream(); PDDocument document = PDDocument.load(is)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            // Temporary debug logging so we can see what the PDF actually contains
            System.out.println("=== RAW PDF TEXT START ===");
            System.out.println(text);
            System.out.println("=== RAW PDF TEXT END ===");
            return parseText(text);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse PDF", e);
        }
    }

    private InvoiceRequestDto parseText(String text) {
        InvoiceRequestDto dto = new InvoiceRequestDto();

        dto.setInvoiceDate(extractDate(text));
        dto.setMetalType(extractMetalType(text));
        dto.setAmountWithoutGst(extractLineAmount(text));
        dto.setGstAmount(extractGstAmount(text));
        dto.setTotalAmount(extractTotalInvoiceValue(text));

        // If parsing failed for some fields, log it so we can refine the patterns
        if (dto.getInvoiceDate() == null
                || dto.getMetalType() == null
                || dto.getAmountWithoutGst() == null
                || dto.getGstAmount() == null
                || dto.getTotalAmount() == null) {
            System.out.println("[PdfParser] Parsed invoice with missing fields: date=" + dto.getInvoiceDate()
                    + ", metalType=" + dto.getMetalType()
                    + ", amountWithoutGst=" + dto.getAmountWithoutGst()
                    + ", gstAmount=" + dto.getGstAmount()
                    + ", totalAmount=" + dto.getTotalAmount());
        }

        return dto;
    }

    private LocalDate extractDate(String text) {
        // Try specific label first: "Date: 2025-10-28" or "Date 2025-10-28"
        Pattern labeled = Pattern.compile("(?i)Date\\s*[:]?\\s*(\\d{4}-\\d{2}-\\d{2}|\\d{2}/\\d{2}/\\d{4})");
        Matcher matcher = labeled.matcher(text);
        if (matcher.find()) {
            String value = matcher.group(1).trim();
            try {
                if (value.contains("-")) {
                    return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
                }
                return LocalDate.parse(value, DATE_FORMATTER);
            } catch (DateTimeParseException ignored) {
            }
        }

        // Fallback to any dd/MM/yyyy in the text
        Pattern any = Pattern.compile("(\\d{2}/\\d{2}/\\d{4})");
        Matcher anyMatcher = any.matcher(text);
        if (anyMatcher.find()) {
            String dateStr = anyMatcher.group(1);
            try {
                return LocalDate.parse(dateStr, DATE_FORMATTER);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private String extractMetalType(String text) {
        // Prefer explicit Product label: "Product: SILVER24"
        Pattern productLabel = Pattern.compile("(?i)Product\\s*:?\\s*([A-Z0-9]+)");
        Matcher productMatcher = productLabel.matcher(text);
        if (productMatcher.find()) {
            return productMatcher.group(1).trim();
        }

        // Fallback: first word in a line starting with a metal name like GOLD/SILVER
        Pattern metalLine = Pattern.compile("(?im)^(GOLD|SILVER|PLATINUM|PALLADIUM)[A-Z0-9 ]*.*$");
        Matcher metalLineMatcher = metalLine.matcher(text);
        if (metalLineMatcher.find()) {
            return metalLineMatcher.group(1).toUpperCase(Locale.ENGLISH);
        }

        return null;
    }

    private BigDecimal extractLineAmount(String text) {
        // Match product line with columns ending in amount, e.g. "SILVER24 6.4604 150.28 970.87"
        Pattern lineAmount = Pattern.compile("(?im)^(?:Product\\s*:?\\s*)?(SILVER24|GOLD[0-9A-Z ]*|SILVER[0-9A-Z ]*).*$");
        Matcher matcher = lineAmount.matcher(text);
        if (matcher.find()) {
            // Take the last numeric token on that line as the amount
            String line = matcher.group(0);
            Pattern amountPattern = Pattern.compile("([0-9]+[.,][0-9]+)\\s*$");
            Matcher amountMatcher = amountPattern.matcher(line);
            if (amountMatcher.find()) {
                String number = amountMatcher.group(1).replace(",", "");
                number = number.replaceAll("[^0-9.]+", "");
                try {
                    return new BigDecimal(number);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    private BigDecimal extractGstAmount(String text) {
        // Example: "GST(3%) 29.13"
        Pattern gstPattern = Pattern.compile("(?i)GST\\s*\\([^)]*\\)\\s*([₹$€£]?\\s*[0-9]+[.,][0-9]+)");
        Matcher matcher = gstPattern.matcher(text);
        if (matcher.find()) {
            String number = matcher.group(1).replace(",", "");
            number = number.replaceAll("[^0-9.]+", "");
            try {
                return new BigDecimal(number);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private BigDecimal extractTotalInvoiceValue(String text) {
        // Example: "TOTAL INVOICE VALUE 1000"
        Pattern totalPattern = Pattern.compile("(?i)TOTAL\\s+INVOICE\\s+VALUE\\s*([₹$€£]?\\s*[0-9]+[.,]?[0-9]*)");
        Matcher matcher = totalPattern.matcher(text);
        if (matcher.find()) {
            String number = matcher.group(1).replace(",", "");
            number = number.replaceAll("[^0-9.]+", "");
            try {
                return new BigDecimal(number);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}
