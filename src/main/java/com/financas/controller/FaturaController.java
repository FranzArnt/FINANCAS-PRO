package com.financas.controller;

import com.financas.model.*;
import com.financas.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.YearMonth;

@Controller
@RequestMapping("/faturas")
public class FaturaController {

    private final FaturaService faturaService;
    private final ConfiguracoesService configService;
    private final CartaoService cartaoService;
    private final MovimentacaoService movService;

    public FaturaController(FaturaService faturaService,
                            ConfiguracoesService configService,
                            CartaoService cartaoService,
                            MovimentacaoService movService) {
        this.faturaService = faturaService;
        this.configService = configService;
        this.cartaoService = cartaoService;
        this.movService = movService;
    }

    // Atalho: vai para a fatura aberta correta considerando o dia de fechamento
    @GetMapping("/cartao/{cartaoId}/atual")
    public String faturaAtual(@PathVariable Long cartaoId) {
        CartaoCredito cartao = cartaoService.buscar(cartaoId)
                .orElseThrow(() -> new IllegalArgumentException("Cartão não encontrado"));
        FaturaCartao fatura = faturaService.identificarFaturaAlvo(cartao, LocalDate.now());
        // Só avança para o próximo mês se a fatura está PAGA E o dia de fechamento já passou
        if (fatura.getStatus() == com.financas.model.StatusFatura.PAGA) {
            LocalDate dataFechamento = fatura.getDataFechamento();
            boolean fechamentoPassou = dataFechamento == null || !LocalDate.now().isBefore(dataFechamento);
            if (fechamentoPassou) {
                fatura = faturaService.gerarFaturaSeNaoExistir(cartao, fatura.getMesReferencia().plusMonths(1));
            }
        }
        return "redirect:/faturas/" + fatura.getId();
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Model model) {
        FaturaCartao fatura = faturaService.buscar(id)
                .orElseThrow(() -> new IllegalArgumentException("Fatura não encontrada"));

        var lancamentos = faturaService.listarLancamentos(fatura);
        var categorias  = configService.listarPorTipo(TipoCategoria.DESPESA);

        // Saldo da conta vinculada ao cartão
        java.math.BigDecimal saldoConta = java.math.BigDecimal.ZERO;
        if (fatura.getCartao().getConta() != null) {
            saldoConta = movService.calcularSaldoAtual(fatura.getCartao().getConta());
        }

        // Dias restantes até o fechamento
        long diasParaFechar = 0;
        boolean podeFecharOuPagar = true;
        if (fatura.getDataFechamento() != null && fatura.getStatus() == StatusFatura.ABERTA) {
            diasParaFechar = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), fatura.getDataFechamento());
            podeFecharOuPagar = diasParaFechar <= 0;
        }

        model.addAttribute("fatura", fatura);
        model.addAttribute("lancamentos", lancamentos);
        model.addAttribute("categorias", categorias);
        model.addAttribute("novoLancamento", new LancamentoFatura());
        model.addAttribute("saldoConta", saldoConta);
        model.addAttribute("saldoSuficiente", saldoConta.compareTo(fatura.getValor()) >= 0);
        model.addAttribute("diasParaFechar", diasParaFechar);
        model.addAttribute("podeFecharOuPagar", podeFecharOuPagar);
        model.addAttribute("currentPage", "cartoes");
        return "faturas/detalhe";
    }

    @PostMapping("/{id}/lancamentos")
    public String adicionarLancamento(@PathVariable Long id,
                                      @ModelAttribute LancamentoFatura lancamento,
                                      RedirectAttributes ra) {
        try {
            faturaService.adicionarLancamento(id, lancamento);
            ra.addFlashAttribute("sucesso", "Lançamento adicionado!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/faturas/" + id;
    }

    @GetMapping("/{id}/lancamentos/{lid}/remover")
    public String removerLancamento(@PathVariable Long id, @PathVariable Long lid,
                                    RedirectAttributes ra) {
        try {
            faturaService.removerLancamento(lid);
            ra.addFlashAttribute("sucesso", "Lançamento removido.");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/faturas/" + id;
    }

    @PostMapping("/{id}/fechar")
    public String fecharFatura(@PathVariable Long id, RedirectAttributes ra) {
        try {
            faturaService.fecharFatura(id);
            ra.addFlashAttribute("sucesso", "Fatura fechada com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/faturas/" + id;
    }

    @PostMapping("/{id}/pagar")
    public String pagarFatura(@PathVariable Long id, RedirectAttributes ra) {
        try {
            faturaService.pagarFatura(id);
            ra.addFlashAttribute("sucesso", "Fatura paga! Transação registrada na conta.");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/faturas/" + id;
    }
}
