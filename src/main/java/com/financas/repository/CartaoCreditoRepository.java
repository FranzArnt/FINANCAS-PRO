package com.financas.repository;

import com.financas.model.CartaoCredito;
import com.financas.model.ContaBancaria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CartaoCreditoRepository extends JpaRepository<CartaoCredito, Long> {

    List<CartaoCredito> findByAtivoTrue();

    List<CartaoCredito> findByContaAndAtivoTrue(ContaBancaria conta);
}
