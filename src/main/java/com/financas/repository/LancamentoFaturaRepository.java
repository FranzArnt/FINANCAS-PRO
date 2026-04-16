package com.financas.repository;

import com.financas.model.FaturaCartao;
import com.financas.model.LancamentoFatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface LancamentoFaturaRepository extends JpaRepository<LancamentoFatura, Long> {

    List<LancamentoFatura> findByFaturaOrderByDataCompraDesc(FaturaCartao fatura);

    @Query("SELECT COALESCE(SUM(l.valor), 0) FROM LancamentoFatura l WHERE l.fatura = :fatura")
    BigDecimal sumByFatura(@Param("fatura") FaturaCartao fatura);
}
