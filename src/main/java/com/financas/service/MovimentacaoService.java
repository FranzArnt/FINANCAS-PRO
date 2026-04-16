package com.financas.service;

import com.financas.model.*;
import com.financas.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
public class MovimentacaoService {

    private final MovimentacaoRepository movRepo;
    private final TransacaoRepository transacaoRepo;
    private final ContaBancariaRepository contaRepo;
    private final CategoriaRepository categoriaRepo;
    private final FaturaRepository faturaRepo;

    public MovimentacaoService(MovimentacaoRepository movRepo,
                               TransacaoRepository transacaoRepo,
                               ContaBancariaRepository contaRepo,
                               CategoriaRepository categoriaRepo,
                               FaturaRepository faturaRepo) {
        this.movRepo = movRepo;
        this.transacaoRepo = transacaoRepo;
        this.contaRepo = contaRepo;
        this.categoriaRepo = categoriaRepo;
        this.faturaRepo = faturaRepo;
    }

    // ── Saldo real da conta ───────────────────────────────────────
    public BigDecimal calcularSaldoAtual(ContaBancaria conta) {
        BigDecimal entradas = movRepo.sumEntradasTotal(conta);
        BigDecimal saidas   = movRepo.sumSaidasTotal(conta);
        return conta.getSaldoInicial().add(entradas).subtract(saidas);
    }

    // ── Saldo projetado: saldo atual - faturas abertas/fechadas ───
    public BigDecimal calcularSaldoProjetado(ContaBancaria conta, YearMonth mes) {
        BigDecimal saldo = calcularSaldoAtual(conta);
        BigDecimal faturasPendentes = faturaRepo.findAllAbertas().stream()
                .filter(f -> f.getConta() != null && f.getConta().getId().equals(conta.getId()))
                .filter(f -> f.getDataVencimento() != null
                        && YearMonth.from(f.getDataVencimento()).equals(mes))
                .map(FaturaCartao::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return saldo.subtract(faturasPendentes);
    }

    // ── Média mensal de despesas ──────────────────────────────────
    public BigDecimal calcularMediaMensalDespesas(ContaBancaria conta, int ultimosMeses) {
        LocalDate inicio = LocalDate.now().withDayOfMonth(1).minusMonths(ultimosMeses);
        BigDecimal total = BigDecimal.ZERO;
        int mesesComDados = 0;

        for (int i = 1; i <= ultimosMeses; i++) {
            LocalDate mes = LocalDate.now().minusMonths(i);
            BigDecimal desp = movRepo.sumDespesasByContaAndMes(conta, mes.getYear(), mes.getMonthValue());
            if (desp.compareTo(BigDecimal.ZERO) > 0) {
                total = total.add(desp);
                mesesComDados++;
            }
        }
        if (mesesComDados == 0) return BigDecimal.ZERO;
        return total.divide(new BigDecimal(mesesComDados), 2, RoundingMode.HALF_UP);
    }

    // ── Transações ────────────────────────────────────────────────
    public List<Transacao> listarTransacoes(ContaBancaria conta) {
        return transacaoRepo.findByConta(conta);
    }

    public List<Transacao> listarTodasTransacoes() {
        return transacaoRepo.findAllByOrderByDataDesc();
    }

    @Transactional
    public Transacao salvarTransacao(Transacao transacao) {
        // Resolve categoria pelo id (evita TransientPropertyValueException)
        if (transacao.getCategoria() != null && transacao.getCategoria().getId() != null) {
            categoriaRepo.findById(transacao.getCategoria().getId())
                    .ifPresentOrElse(transacao::setCategoria, () -> transacao.setCategoria(null));
        } else {
            transacao.setCategoria(null);
        }
        // Resolve conta pelo id (evita TransientPropertyValueException)
        if (transacao.getConta() != null && transacao.getConta().getId() != null) {
            contaRepo.findById(transacao.getConta().getId())
                    .ifPresentOrElse(transacao::setConta, () -> transacao.setConta(null));
        } else {
            transacao.setConta(null);
        }
        return transacaoRepo.save(transacao);
    }

    @Transactional
    public void excluirTransacao(Long id) {
        transacaoRepo.deleteById(id);
    }

    public Optional<Transacao> buscarTransacao(Long id) {
        return transacaoRepo.findById(id);
    }

    // ── Contas bancárias ──────────────────────────────────────────
    public List<ContaBancaria> listarContas() {
        return contaRepo.findByAtivaTrue();
    }

    public Optional<ContaBancaria> buscarConta(Long id) {
        return contaRepo.findById(id);
    }

    @Transactional
    public ContaBancaria salvarConta(ContaBancaria conta) {
        return contaRepo.save(conta);
    }

    // ── Histírico mensal para gráfico ─────────────────────────────
    public List<Object[]> historicoDespesasMensal(ContaBancaria conta, int meses) {
        LocalDate inicio = LocalDate.now().minusMonths(meses).withDayOfMonth(1);
        return movRepo.sumDespesasByMes(conta, inicio);
    }

    public List<Object[]> historicoEntradasMensal(ContaBancaria conta, int meses) {
        LocalDate inicio = LocalDate.now().minusMonths(meses).withDayOfMonth(1);
        return movRepo.sumEntradasByMes(conta, inicio);
    }
}
