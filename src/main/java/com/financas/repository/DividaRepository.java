package com.financas.repository;

import com.financas.model.Divida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface DividaRepository extends JpaRepository<Divida, Long> {
    List<Divida> findByStatus(String status);

    List<Divida> findByTipoIn(List<String> tipos);

    @Query("SELECT SUM(d.valorRestante) FROM Divida d")
    BigDecimal somarTotalDividas();

    @Query("SELECT d.tipo, SUM(d.valorRestante) FROM Divida d GROUP BY d.tipo")
    List<Object[]> totalPorTipo();
}
