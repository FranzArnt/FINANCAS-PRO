package com.financas.service;

import com.financas.model.*;
import com.financas.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
public class FaturaService {

    private final FaturaRepository faturaRepo;
    private final LancamentoFaturaRepository lancamentoRepo;
    private final CategoriaRepository categoriaRepo;
    private final MovimentacaoRepository movimentacaoRepo;

    public FaturaService(FaturaRepository faturaRepo,
                         LancamentoFaturaRepository lancamentoRepo,
                         CategoriaRepository categoriaRepo,
                         MovimentacaoRepository movimentacaoRepo) {
        this.faturaRepo = faturaRepo;
        this.lancamentoRepo = lancamentoRepo;
        this.categoriaRepo = categoriaRepo;
        this.movimentacaoRepo = movimentacaoRepo;
    }

    // ── Identificar fatura-alvo de uma compra ─────────────────────
    @Transactional
    public FaturaCartao identificarFaturaAlvo(CartaoCredito cartao, LocalDate dataCompra) {
        int diaFechamento = cartao.getDiaFechamento();
        YearMonth mesAlvo = dataCompra.getDayOfMonth() <= diaFechamento
                ? YearMonth.from(dataCompra)
                : YearMonth.from(dataCompra).plusMonths(1);
        return gerarFaturaSeNaoExistir(cartao, mesAlvo);
    }

    @Transactional
    public FaturaCartao gerarFaturaSeNaoExistir(CartaoCredito cartao, YearMonth mes) {
        return faturaRepo.findByCartaoAndMesReferencia(cartao, mes)
                .orElseGet(() -> criarNovaFatura(cartao, mes));
    }

    private FaturaCartao criarNovaFatura(CartaoCredito cartao, YearMonth mes) {
        int diaFechamento = cartao.getDiaFechamento();
        int diaVencimento = cartao.getDiaVencimento();

        LocalDate dataFechamento = mes.atDay(Math.min(diaFechamento, mes.lengthOfMonth()));

        LocalDate dataVencimento;
        if (diaFechamento >= diaVencimento) {
            dataVencimento = mes.plusMonths(1).atDay(
                    Math.min(diaVencimento, mes.plusMonths(1).lengthOfMonth()));
        } else {
            dataVencimento = mes.atDay(Math.min(diaVencimento, mes.lengthOfMonth()));
        }

        FaturaCartao fatura = new FaturaCartao();
        fatura.setCartao(cartao);
        fatura.setMesReferencia(mes);
        fatura.setDataFechamento(dataFechamento);
        fatura.setDataVencimento(dataVencimento);
        fatura.setData(dataVencimento);
        fatura.setValor(BigDecimal.ZERO);
        fatura.setConta(cartao.getConta());
        fatura.setStatus(StatusFatura.ABERTA);

        // Categoria "Pagamento de Fatura"
        categoriaRepo.findAll().stream()
                .filter(c -> "Pagamento de Fatura".equals(c.getNome()))
                .findFirst()
                .ifPresent(fatura::setCategoria);

        return faturaRepo.save(fatura);
    }

    // ── Adicionar lançamento ──────────────────────────────────────
    @Transactional
    public LancamentoFatura adicionarLancamento(Long faturaId, LancamentoFatura lancamento) {
        FaturaCartao fatura = faturaRepo.findById(faturaId)
                .orElseThrow(() -> new IllegalArgumentException("Fatura não encontrada"));

        StatusFatura statusEfetivo = resolverStatus(fatura);
        if (statusEfetivo != StatusFatura.ABERTA) {
            throw new IllegalStateException("Não é possível adicionar lançamentos a uma fatura " + statusEfetivo);
        }

        // Resolve categoria pelo id (evita TransientPropertyValueException)
        if (lancamento.getCategoria() != null && lancamento.getCategoria().getId() != null) {
            categoriaRepo.findById(lancamento.getCategoria().getId())
                    .ifPresentOrElse(lancamento::setCategoria, () -> lancamento.setCategoria(null));
        } else {
            lancamento.setCategoria(null);
        }

        lancamento.setFatura(fatura);
        LancamentoFatura salvo = lancamentoRepo.save(lancamento);

        recalcularValorFatura(fatura);
        return salvo;
    }

    // ── Remover lançamento ────────────────────────────────────────
    @Transactional
    public void removerLancamento(Long lancamentoId) {
        LancamentoFatura lanc = lancamentoRepo.findById(lancamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Lançamento não encontrado"));

        FaturaCartao fatura = lanc.getFatura();
        StatusFatura statusEfetivo = resolverStatus(fatura);
        if (statusEfetivo != StatusFatura.ABERTA) {
            throw new IllegalStateException("Não é possível remover lançamentos de uma fatura " + statusEfetivo);
        }

        lancamentoRepo.delete(lanc);
        recalcularValorFatura(fatura);
    }

    // ── Fechar fatura ─────────────────────────────────────────────
    @Transactional
    public FaturaCartao fecharFatura(Long faturaId) {
        FaturaCartao fatura = faturaRepo.findById(faturaId)
                .orElseThrow(() -> new IllegalArgumentException("Fatura não encontrada"));

        if (fatura.getStatus() != StatusFatura.ABERTA) {
            throw new IllegalStateException("Apenas faturas ABERTAS podem ser fechadas");
        }

        if (fatura.getDataFechamento() != null && LocalDate.now().isBefore(fatura.getDataFechamento())) {
            throw new IllegalStateException("A fatura só pode ser fechada a partir do dia "
                    + fatura.getDataFechamento().getDayOfMonth() + " (dia de fechamento).");
        }

        fatura.setStatus(StatusFatura.FECHADA);
        FaturaCartao fechada = faturaRepo.save(fatura);

        // Gera automaticamente a fatura do próximo mês como ABERTA
        gerarFaturaSeNaoExistir(fatura.getCartao(), fatura.getMesReferencia().plusMonths(1));

        return fechada;
    }

    // ── Pagar fatura ──────────────────────────────────────────────
    @Transactional
    public FaturaCartao pagarFatura(Long faturaId) {
        FaturaCartao fatura = faturaRepo.findById(faturaId)
                .orElseThrow(() -> new IllegalArgumentException("Fatura não encontrada"));

        // Se ainda estiver aberta, só pode pagar após o dia de fechamento
        if (fatura.getStatus() == StatusFatura.ABERTA) {
            if (fatura.getDataFechamento() != null && LocalDate.now().isBefore(fatura.getDataFechamento())) {
                throw new IllegalStateException("A fatura só pode ser paga a partir do dia "
                        + fatura.getDataFechamento().getDayOfMonth() + " (dia de fechamento).");
            }
            fatura.setStatus(StatusFatura.FECHADA);
            fatura = faturaRepo.save(fatura);
            gerarFaturaSeNaoExistir(fatura.getCartao(), fatura.getMesReferencia().plusMonths(1));
        }

        if (fatura.getStatus() != StatusFatura.FECHADA && fatura.getStatus() != StatusFatura.VENCIDA) {
            throw new IllegalStateException("Fatura já foi paga.");
        }

        fatura.setStatus(StatusFatura.PAGA);
        FaturaCartao paga = faturaRepo.save(fatura);

        // Cria transação de saída na conta do cartão
        Transacao pagamento = new Transacao();
        pagamento.setData(LocalDate.now());
        pagamento.setValor(fatura.getValor());
        pagamento.setTipo(TipoTransacao.DEBITO);
        pagamento.setDirecao(DirecaoTransacao.SAIDA);
        pagamento.setDescricao("Pagamento fatura " + fatura.getCartao().getNome()
                + " " + fatura.getMesReferencia());
        pagamento.setDestino(fatura.getCartao().getNome());
        pagamento.setConta(fatura.getCartao().getConta());
        pagamento.setObservacao("Pagamento fatura " + fatura.getCartao().getNome()
                + " " + fatura.getMesReferencia());

        categoriaRepo.findAll().stream()
                .filter(c -> "Pagamento de Fatura".equals(c.getNome()))
                .findFirst()
                .ifPresent(pagamento::setCategoria);

        movimentacaoRepo.save(pagamento);
        return paga;
    }

    // ── Resolver status dinamicamente ─────────────────────────────
    public StatusFatura resolverStatus(FaturaCartao fatura) {
        if (fatura.getStatus() == StatusFatura.PAGA) return StatusFatura.PAGA;
        if (fatura.getDataVencimento() != null
                && fatura.getDataVencimento().isBefore(LocalDate.now())
                && fatura.getStatus() != StatusFatura.PAGA) {
            return StatusFatura.VENCIDA;
        }
        return fatura.getStatus();
    }

    // ── Consultas ─────────────────────────────────────────────────
    public Optional<FaturaCartao> buscar(Long id) {
        return faturaRepo.findById(id).map(f -> {
            f.setStatus(resolverStatus(f));
            return f;
        });
    }

    public List<FaturaCartao> listarPorCartao(CartaoCredito cartao) {
        return faturaRepo.findByCartaoOrderByMesReferenciaDesc(cartao).stream()
                .peek(f -> f.setStatus(resolverStatus(f)))
                .toList();
    }

    public List<FaturaCartao> faturasAVencer(int dias) {
        LocalDate hoje = LocalDate.now();
        return faturaRepo.findFaturasAVencer(hoje, hoje.plusDays(dias)).stream()
                .peek(f -> f.setStatus(resolverStatus(f)))
                .toList();
    }

    public List<FaturaCartao> listarAbertas() {
        return faturaRepo.findAllAbertas().stream()
                .peek(f -> f.setStatus(resolverStatus(f)))
                .toList();
    }

    public List<LancamentoFatura> listarLancamentos(FaturaCartao fatura) {
        return lancamentoRepo.findByFaturaOrderByDataCompraDesc(fatura);
    }

    // ── Helpers ───────────────────────────────────────────────────
    private void recalcularValorFatura(FaturaCartao fatura) {
        BigDecimal total = lancamentoRepo.sumByFatura(fatura);
        fatura.setValor(total);
        faturaRepo.save(fatura);
    }
}
