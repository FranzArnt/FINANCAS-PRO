package com.financas.repository;

import com.financas.model.Categoria;
import com.financas.model.TipoCategoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByAtivaTrue();

    List<Categoria> findByTipoAndAtivaTrue(TipoCategoria tipo);

    // Apenas raízes (sem pai)
    @Query("SELECT c FROM Categoria c WHERE c.pai IS NULL AND c.ativa = true ORDER BY c.nome")
    List<Categoria> findRaizesAtivas();

    // Raízes com filhas carregadas
    @Query("SELECT DISTINCT c FROM Categoria c LEFT JOIN FETCH c.filhas WHERE c.pai IS NULL AND c.ativa = true ORDER BY c.nome")
    List<Categoria> findRaizesComFilhas();
}
