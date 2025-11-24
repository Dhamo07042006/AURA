package com.aura.app.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class InvoiceRequestDto {

    private Long userId;
    private LocalDate invoiceDate;
    private String metalType;
    private BigDecimal amountWithoutGst;
    private BigDecimal gstAmount;
    private BigDecimal totalAmount;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(LocalDate invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public String getMetalType() {
        return metalType;
    }

    public void setMetalType(String metalType) {
        this.metalType = metalType;
    }

    public BigDecimal getAmountWithoutGst() {
        return amountWithoutGst;
    }

    public void setAmountWithoutGst(BigDecimal amountWithoutGst) {
        this.amountWithoutGst = amountWithoutGst;
    }

    public BigDecimal getGstAmount() {
        return gstAmount;
    }

    public void setGstAmount(BigDecimal gstAmount) {
        this.gstAmount = gstAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}
