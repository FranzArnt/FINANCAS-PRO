package com.financas.repository;

import com.financas.model.ContaBancaria;
import com.financas.model.DirecaoTransacao;
import com.financas.model.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface TransacaoRepository extends JpaRepository<Transacao, Long> {

    List<Transacao> findAllByOrderByDataDesc();

    List<Transacao> findByDirecao(DirecaoTransacao direcao);

    // Transações de uma conta ordenadas por data
    @Query("SELECT t FROM Transacao t WHERE t.conta = :conta ORDER BY t.data DESC")
    List<Transacao> findByConta(@Param("conta") ContaBancaria conta);

    // Soma de entradas de uma conta até hoje (acumulado)
    @Query("SELECT COALESCE(SUM(t.valor), 0) FROM Transacao t " +
           "WHERE t.conta = :conta AND t.direcao = 'ENTRADA'")
    BigDecimal somarEntradasTotal(@Param("conta") ContaBancaria conta);

    // Soma de saídas de uma conta até hoje (acumulado)
    @Query("SELECT COALESCE(SUM(t.valor), 0) FROM Transacao t " +
           "WHERE t.conta = :conta AND t.direcao = 'SAIDA'")
    BigDecimal somarSaidasTotal(@Param("conta") ContaBancaria conta);

    // Entradas do mês
    @Query("SELECT COALESCE(SUM(t.valor), 0) FROM Transacao t " +
           "WHERE t.conta = :conta AND t.direcao = 'ENTRADA' " +
           "AND YEAR(t.data) = :ano AND MONTH(t.data) = :mes")
    BigDecimal somarEntradasMes(@Param("conta") ContaBancaria conta,
                                @Param("mes") int mes,
                                @Param("ano") int ano);

    // Saídas do mês
    @Query("SELECT COALESCE(SUM(t.valor), 0) FROM Transacao t " +
           "WHERE t.conta = :conta AND t.direcao = 'SAIDA' " +
           "AND YEAR(t.data) = :ano AND MONTH(t.data) = :mes")
    BigDecimal somarSaidasMes(@Param("conta") ContaBancaria conta,
                               @Param("mes") int mes,
                               @Param("ano") int ano);

    // Histórico mês a mês para gráfico
    @Query("SELECT YEAR(t.data), MONTH(t.data), t.direcao, COALESCE(SUM(t.valor), 0) " +
           "FROM Transacao t WHERE t.conta = :conta " +
           "GROUP BY YEAR(t.data), MONTH(t.data), t.direcao " +
           "ORDER BY YEAR(t.data), MONTH(t.data)")
    List<Object[]> historicoMensal(@Param("conta") ContaBancaria conta);
}
