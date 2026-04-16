package com.financas.service;

import com.financas.model.*;
import com.financas.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Serviço legado — mantém Dívidas e Investimentos.
 * A lógica de Transações/Dashboard migrou para os novos serviços
 * (MovimentacaoService, DashboardService).
 */
@Service
public class FinancasService {

    private final DividaRepository dividaRepo;
    private final InvestimentoRepository investimentoRepo;
    private final MovimentacaoRepository movimentacaoRepo;
    private final ContaBancariaRepository contaRepo;
    private final CategoriaRepository categoriaRepo;

    public FinancasService(DividaRepository dividaRepo,
                           InvestimentoRepository investimentoRepo,
                           MovimentacaoRepository movimentacaoRepo,
                           ContaBancariaRepository contaRepo,
                           CategoriaRepository categoriaRepo) {
        this.dividaRepo = dividaRepo;
        this.investimentoRepo = investimentoRepo;
        this.movimentacaoRepo = movimentacaoRepo;
        this.contaRepo = contaRepo;
        this.categoriaRepo = categoriaRepo;
    }

    // ===== DÍVIDAS =====

    public List<Divida> listarDividas() {
        return dividaRepo.findAll().stream()
            .filter(d -> !"CARTAO".equals(d.getTipo()) && !"IMPORTADO".equals(d.getTipo()))
            .toList();
    }

    public List<Divida> listarFatura() {
        return dividaRepo.findAll().stream()
            .filter(d -> "CARTAO".equals(d.getTipo()) || "IMPORTADO".equals(d.getTipo()))
            .toList();
    }

    public Divida salvarDivida(Divida divida) {
        return dividaRepo.save(divida);
    }

    public Optional<Divida> buscarDivida(Long id) {
        return dividaRepo.findById(id);
    }

    public void excluirDivida(Long id) {
        dividaRepo.deleteById(id);
    }

    // ===== INVESTIMENTOS =====

    public List<Investimento> listarInvestimentos() {
        return investimentoRepo.findAll();
    }

    @Transactional
    public Investimento salvarInvestimento(Investimento investimento) {
        boolean isNovo = investimento.getId() == null;

        // Resolve conta pelo id
        if (investimento.getConta() != null && investimento.getConta().getId() != null) {
            contaRepo.findById(investimento.getConta().getId())
                    .ifPresentOrElse(investimento::setConta, () -> investimento.setConta(null));
        } else {
            investimento.setConta(null);
        }

        Investimento salvo = investimentoRepo.save(investimento);

        // Só debita da conta ao criar novo investimento
        if (isNovo && salvo.getConta() != null) {
            Transacao t = new Transacao();
            t.setData(salvo.getDataAplicacao() != null ? salvo.getDataAplicacao() : LocalDate.now());
            t.setValor(salvo.getValorInvestido());
            t.setTipo(TipoTransacao.DEBITO);
            t.setDirecao(DirecaoTransacao.SAIDA);
            t.setDescricao("Investimento: " + salvo.getNome());
            t.setDestino("Investimentos");
            t.setConta(salvo.getConta());
            categoriaRepo.findAll().stream()
                    .filter(c -> "Investimento".equals(c.getNome()))
                    .findFirst()
                    .ifPresent(t::setCategoria);
            movimentacaoRepo.save(t);
        }

        return salvo;
    }

    public Optional<Investimento> buscarInvestimento(Long id) {
        return investimentoRepo.findById(id);
    }

    public void excluirInvestimento(Long id) {
        investimentoRepo.deleteById(id);
    }

    @Transactional
    public void resgatarInvestimento(Long id) {
        Investimento inv = investimentoRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Investimento não encontrado"));

        if ("RESGATADO".equals(inv.getStatus())) {
            throw new IllegalStateException("Investimento já foi resgatado.");
        }

        if (inv.getConta() != null) {
            Transacao t = new Transacao();
            t.setData(LocalDate.now());
            t.setValor(inv.getValorAtual());
            t.setTipo(TipoTransacao.DEPOSITO);
            t.setDirecao(DirecaoTransacao.ENTRADA);
            t.setDescricao("Resgate: " + inv.getNome());
            t.setDestino(inv.getConta().getNome());
            t.setConta(inv.getConta());
            categoriaRepo.findAll().stream()
                    .filter(c -> "Investimento".equals(c.getNome()))
                    .findFirst()
                    .ifPresent(t::setCategoria);
            movimentacaoRepo.save(t);
        }

        inv.setStatus("RESGATADO");
        investimentoRepo.save(inv);
    }
}
