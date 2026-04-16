package com.financas.repository;

import com.financas.model.Investimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface InvestimentoRepository extends JpaRepository<Investimento, Long> {
    List<Investimento> findByStatus(String status);

    @Query("SELECT SUM(i.valorAtual) FROM Investimento i")
    BigDecimal somarTotalAtual();

    @Query("SELECT SUM(i.valorInvestido) FROM Investimento i")
    BigDecimal somarTotalInvestido();

    @Query("SELECT i.tipo, SUM(i.valorAtual) FROM Investimento i GROUP BY i.tipo")
    List<Object[]> totalPorTipo();
}
