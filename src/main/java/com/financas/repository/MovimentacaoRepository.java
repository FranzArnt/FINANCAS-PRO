package com.financas.repository;

import com.financas.model.ContaBancaria;
import com.financas.model.FaturaCartao;
import com.financas.model.Movimentacao;
import com.financas.model.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface MovimentacaoRepository extends JpaRepository<Movimentacao, Long> {

    // Todas as movimentações de uma conta em um mês
    @Query("SELECT m FROM Movimentacao m " +
           "WHERE m.conta = :conta " +
           "AND YEAR(m.data) = :ano AND MONTH(m.data) = :mes " +
           "ORDER BY m.data DESC")
    List<Movimentacao> findByContaAndMes(@Param("conta") ContaBancaria conta,
                                         @Param("ano") int ano,
                                         @Param("mes") int mes);

    // Soma de despesas do mês (faturas abertas/fechadas + transações SAIDA)
    // Faturas PAGAS são excluídas pois geram Transacao SAIDA que já é contada
    @Query("SELECT COALESCE(SUM(m.valor), 0) FROM Movimentacao m " +
           "WHERE m.conta = :conta " +
           "AND YEAR(m.data) = :ano AND MONTH(m.data) = :mes " +
           "AND ((TYPE(m) = FaturaCartao AND TREAT(m AS FaturaCartao).status <> 'PAGA') " +
           "  OR (TYPE(m) = Transacao AND TREAT(m AS Transacao).direcao = 'SAIDA'))")
    BigDecimal sumDespesasByContaAndMes(@Param("conta") ContaBancaria conta,
                                        @Param("ano") int ano,
                                        @Param("mes") int mes);

    // Soma de entradas do mês
    @Query("SELECT COALESCE(SUM(m.valor), 0) FROM Movimentacao m " +
           "WHERE TYPE(m) = Transacao " +
           "AND TREAT(m AS Transacao).direcao = 'ENTRADA' " +
           "AND m.conta = :conta " +
           "AND YEAR(m.data) = :ano AND MONTH(m.data) = :mes")
    BigDecimal sumEntradasByContaAndMes(@Param("conta") ContaBancaria conta,
                                        @Param("ano") int ano,
                                        @Param("mes") int mes);

    // Histórico dos últimos N meses para gráfico
    @Query("SELECT YEAR(m.data), MONTH(m.data), SUM(m.valor) " +
           "FROM Movimentacao m " +
           "WHERE m.conta = :conta AND m.data >= :dataInicio " +
           "AND ((TYPE(m) = FaturaCartao AND TREAT(m AS FaturaCartao).status <> 'PAGA') " +
           "  OR (TYPE(m) = Transacao AND TREAT(m AS Transacao).direcao = 'SAIDA')) " +
           "GROUP BY YEAR(m.data), MONTH(m.data) " +
           "ORDER BY YEAR(m.data), MONTH(m.data)")
    List<Object[]> sumDespesasByMes(@Param("conta") ContaBancaria conta,
                                    @Param("dataInicio") LocalDate dataInicio);

    @Query("SELECT YEAR(m.data), MONTH(m.data), SUM(m.valor) " +
           "FROM Movimentacao m " +
           "WHERE m.conta = :conta AND m.data >= :dataInicio " +
           "AND TYPE(m) = Transacao " +
           "AND TREAT(m AS Transacao).direcao = 'ENTRADA' " +
           "GROUP BY YEAR(m.data), MONTH(m.data) " +
           "ORDER BY YEAR(m.data), MONTH(m.data)")
    List<Object[]> sumEntradasByMes(@Param("conta") ContaBancaria conta,
                                    @Param("dataInicio") LocalDate dataInicio);

    // Saldo acumulado — total de entradas de uma conta (todos os tempos)
    @Query("SELECT COALESCE(SUM(m.valor), 0) FROM Movimentacao m " +
           "WHERE TYPE(m) = Transacao " +
           "AND TREAT(m AS Transacao).direcao = 'ENTRADA' " +
           "AND m.conta = :conta")
    BigDecimal sumEntradasTotal(@Param("conta") ContaBancaria conta);

    // Saldo acumulado — total de saídas de uma conta (todos os tempos)
    @Query("SELECT COALESCE(SUM(m.valor), 0) FROM Movimentacao m " +
           "WHERE TYPE(m) = Transacao " +
           "AND TREAT(m AS Transacao).direcao = 'SAIDA' " +
           "AND m.conta = :conta")
    BigDecimal sumSaidasTotal(@Param("conta") ContaBancaria conta);

    // Gastos por categoria no mês (faturas pelo total + transações SAIDA)
    @Query("SELECT m.categoria, SUM(m.valor) FROM Movimentacao m " +
           "WHERE m.conta = :conta " +
           "AND YEAR(m.data) = :ano AND MONTH(m.data) = :mes " +
           "AND (TYPE(m) = FaturaCartao " +
           "  OR (TYPE(m) = Transacao AND TREAT(m AS Transacao).direcao = 'SAIDA')) " +
           "AND m.categoria IS NOT NULL " +
           "GROUP BY m.categoria")
    List<Object[]> gastosPorCategoriaMes(@Param("conta") ContaBancaria conta,
                                          @Param("ano") int ano,
                                          @Param("mes") int mes);
}
