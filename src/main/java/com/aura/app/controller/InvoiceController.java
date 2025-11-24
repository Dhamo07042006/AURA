package com.aura.app.controller;

import com.aura.app.dto.InvoiceRequestDto;
import com.aura.app.dto.InvoiceResponseDto;
import com.aura.app.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/invoices")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000","https://auragoldinvoice.netlify.app"})
@Tag(name = "Invoices", description = "Revenue invoice operations")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping("/upload")
    @Operation(summary = "Upload invoice file (PDF or CSV), extract fields, save and export to CSV")
    public ResponseEntity<InvoiceResponseDto> uploadInvoice(@RequestParam("file") MultipartFile file,
                                                            @RequestParam(value = "userId", required = false) Long userId) {
        return ResponseEntity.ok(invoiceService.uploadInvoice(file, userId));
    }

    @PostMapping("/manual")
    @Operation(summary = "Create manual revenue invoice entry")
    public ResponseEntity<InvoiceResponseDto> createManual(@RequestBody InvoiceRequestDto requestDto) {
        return ResponseEntity.ok(invoiceService.createManualInvoice(requestDto));
    }

    @GetMapping("/all")
    @Operation(summary = "Get all invoices")
    public ResponseEntity<List<InvoiceResponseDto>> getAllInvoices() {
        return ResponseEntity.ok(invoiceService.getAllInvoices());
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get invoices for a specific user")
    public ResponseEntity<List<InvoiceResponseDto>> getInvoicesForUser(@PathVariable Long userId) {
        return ResponseEntity.ok(invoiceService.getInvoicesForUser(userId));
    }

    @GetMapping("/revenue/summary")
    @Operation(summary = "Get revenue summary")
    public ResponseEntity<Map<String, Object>> getRevenueSummary() {
        return ResponseEntity.ok(invoiceService.getRevenueSummary());
    }

    @GetMapping("/revenue/by-date")
    @Operation(summary = "Get revenue by date range")
    public ResponseEntity<List<InvoiceResponseDto>> getRevenueByDate(@RequestParam("start") LocalDate start,
                                                                     @RequestParam("end") LocalDate end) {
        return ResponseEntity.ok(invoiceService.getRevenueByDate(start, end));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete invoice by ID")
    public ResponseEntity<Void> deleteInvoice(@PathVariable Long id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.noContent().build();
    }
}
