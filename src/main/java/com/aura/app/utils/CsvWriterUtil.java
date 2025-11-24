package com.aura.app.utils;

import com.aura.app.dto.InvoiceRequestDto;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class CsvWriterUtil {

    private static final String OUTPUT_DIR = "output";
    private static final String OUTPUT_FILE = "invoices_parsed.csv";
    private static final String HEADER = "invoiceDate,metalType,amountWithoutGst,gstAmount,totalAmount";

    public static Path appendInvoiceToCsv(InvoiceRequestDto dto) {
        try {
            Path dir = Paths.get(OUTPUT_DIR);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            Path filePath = dir.resolve(OUTPUT_FILE);
            boolean fileExists = Files.exists(filePath);

            try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                if (!fileExists) {
                    writer.write(HEADER);
                    writer.newLine();
                }

                String line = String.format("%s,%s,%s,%s,%s",
                        dto.getInvoiceDate() != null ? dto.getInvoiceDate() : "",
                        dto.getMetalType() != null ? dto.getMetalType() : "",
                        dto.getAmountWithoutGst() != null ? dto.getAmountWithoutGst() : "",
                        dto.getGstAmount() != null ? dto.getGstAmount() : "",
                        dto.getTotalAmount() != null ? dto.getTotalAmount() : "");
                writer.write(line);
                writer.newLine();
            }

            return filePath.toAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write invoice CSV", e);
        }
    }
}
