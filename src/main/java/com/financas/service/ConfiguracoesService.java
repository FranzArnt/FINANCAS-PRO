package com.financas.service;

import com.financas.model.Categoria;
import com.financas.model.TipoCategoria;
import com.financas.repository.CategoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ConfiguracoesService {

    private final CategoriaRepository categoriaRepo;

    public ConfiguracoesService(CategoriaRepository categoriaRepo) {
        this.categoriaRepo = categoriaRepo;
    }

    public List<Categoria> listarRaizesComFilhas() {
        return categoriaRepo.findRaizesComFilhas();
    }

    public List<Categoria> listarTodasAtivas() {
        return categoriaRepo.findByAtivaTrue();
    }

    public List<Categoria> listarPorTipo(TipoCategoria tipo) {
        return categoriaRepo.findByTipoAndAtivaTrue(tipo);
    }

    public Optional<Categoria> buscar(Long id) {
        return categoriaRepo.findById(id);
    }

    @Transactional
    public Categoria salvarCategoria(Categoria categoria) {
        // Validação: subcategoria não pode ter pai que já é subcategoria
        if (categoria.getPai() != null && categoria.getPai().getId() != null) {
            Categoria pai = categoriaRepo.findById(categoria.getPai().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Categoria pai não encontrada"));
            if (pai.isSubcategoria()) {
                throw new IllegalArgumentException("Não é permitido criar subcategoria de subcategoria");
            }
            categoria.setPai(pai);
        }
        return categoriaRepo.save(categoria);
    }

    @Transactional
    public void excluirCategoria(Long id) {
        categoriaRepo.findById(id).ifPresent(cat -> {
            cat.getFilhas().forEach(categoriaRepo::delete);
            categoriaRepo.delete(cat);
        });
    }

    @Transactional
    public void desativarCategoria(Long id) {
        categoriaRepo.findById(id).ifPresent(cat -> {
            cat.setAtiva(false);
            // Desativa filhas em cascata
            cat.getFilhas().forEach(filha -> {
                filha.setAtiva(false);
                categoriaRepo.save(filha);
            });
            categoriaRepo.save(cat);
        });
    }

    @Transactional
    public void ativarCategoria(Long id) {
        categoriaRepo.findById(id).ifPresent(cat -> {
            cat.setAtiva(true);
            categoriaRepo.save(cat);
        });
    }
}
