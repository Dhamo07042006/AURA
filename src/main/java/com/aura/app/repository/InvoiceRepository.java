package com.aura.app.repository;

import com.aura.app.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByInvoiceDateBetween(LocalDate start, LocalDate end);

    List<Invoice> findByUserId(Long userId);
}
