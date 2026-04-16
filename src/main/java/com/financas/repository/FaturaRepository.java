package com.financas.repository;

import com.financas.model.CartaoCredito;
import com.financas.model.FaturaCartao;
import com.financas.model.StatusFatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface FaturaRepository extends JpaRepository<FaturaCartao, Long> {

    Optional<FaturaCartao> findByCartaoAndMesReferencia(CartaoCredito cartao, YearMonth mes);

    List<FaturaCartao> findByCartaoOrderByMesReferenciaDesc(CartaoCredito cartao);

    List<FaturaCartao> findByCartaoAndStatusIn(CartaoCredito cartao, List<StatusFatura> statuses);

    @Query("SELECT f FROM FaturaCartao f " +
           "WHERE f.status IN ('ABERTA', 'FECHADA') " +
           "AND f.cartao.ativo = true " +
           "AND f.dataVencimento BETWEEN :hoje AND :limite " +
           "ORDER BY f.dataVencimento ASC")
    List<FaturaCartao> findFaturasAVencer(@Param("hoje") LocalDate hoje,
                                          @Param("limite") LocalDate limite);

    @Query("SELECT COALESCE(SUM(f.valor), 0) FROM FaturaCartao f " +
           "WHERE f.cartao = :cartao AND f.status IN ('ABERTA', 'FECHADA')")
    BigDecimal sumFaturasAbertas(@Param("cartao") CartaoCredito cartao);

    @Query("SELECT f FROM FaturaCartao f WHERE f.status IN ('ABERTA','FECHADA') AND f.cartao.ativo = true ORDER BY f.dataVencimento ASC")
    List<FaturaCartao> findAllAbertas();
}
