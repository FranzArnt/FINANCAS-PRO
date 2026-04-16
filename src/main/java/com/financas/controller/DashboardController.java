package com.financas.controller;

import com.financas.model.*;
import com.financas.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final FinancasService financasService;
    private final MovimentacaoService movimentacaoService;

    public DashboardController(DashboardService dashboardService,
                               FinancasService financasService,
                               MovimentacaoService movimentacaoService) {
        this.dashboardService = dashboardService;
        this.financasService = financasService;
        this.movimentacaoService = movimentacaoService;
    }

    // ===== DASHBOARD =====
    @GetMapping({"/", "/dashboard"})
    public String dashboard(@RequestParam(defaultValue = "0") int mes,
                            @RequestParam(defaultValue = "0") int ano,
                            @RequestParam(required = false) Long conta,
                            Model model) {

        List<ContaBancaria> contas = dashboardService.listarContas();
        model.addAttribute("contas", contas);

        model.addAttribute("semContas", contas.isEmpty());
        if (contas.isEmpty()) {
            model.addAttribute("currentPage", "dashboard");
            return "dashboard";
        }

        // Mês/ano
        YearMonth mesSel = (mes == 0 || ano == 0)
                ? YearMonth.now()
                : YearMonth.of(ano, mes);

        // conta = -1 significa "Todas as contas"
        boolean todos = conta != null && conta == -1L;

        var dto = todos
                ? dashboardService.montarDashboardTodos(mesSel)
                : dashboardService.montarDashboard(
                        contas.stream()
                                .filter(c -> conta != null && c.getId().equals(conta))
                                .findFirst()
                                .orElse(contas.get(0)),
                        mesSel);

        ContaBancaria contaSel = dto.conta() != null ? dto.conta() : contas.get(0);
        model.addAttribute("contaSelecionada", contaSel);
        model.addAttribute("todosSelecionado", todos);
        model.addAttribute("mes", dto.mes());
        model.addAttribute("nomeMes", nomeMes(dto.mes().getMonthValue()) + " " + dto.mes().getYear());
        model.addAttribute("receitasMes",    dto.receitasMes());
        model.addAttribute("despesasMes",    dto.despesasMes());
        model.addAttribute("saldoAtual",     dto.saldoAtual());
        model.addAttribute("saldoProjetado", dto.saldoProjetado());
        model.addAttribute("mediaHistorica", dto.mediaHistorica());
        model.addAttribute("faturasAbertas", dto.faturasAbertas());
        model.addAttribute("faturasAVencer", dto.faturasAVencer7d());
        model.addAttribute("gastosPorCategoria", dto.gastosPorCategoria());
        model.addAttribute("mesesGrafico",    dto.mesesGrafico());
        model.addAttribute("despesasGrafico", dto.despesasGrafico());
        model.addAttribute("entradasGrafico", dto.entradasGrafico());
        model.addAttribute("mesesDisponiveis", gerarMeses());
        model.addAttribute("mesSelecionado", mesSel.getMonthValue());
        model.addAttribute("anoSelecionado",  mesSel.getYear());
        // Total investido (para card no dashboard)
        var investimentos = financasService.listarInvestimentos();
        model.addAttribute("totalInvestido", investimentos.stream()
                .map(Investimento::getValorAtual)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        model.addAttribute("currentPage", "dashboard");
        return "dashboard";
    }

    // ===== DÍVIDAS =====
    @GetMapping("/dividas")
    public String dividas(Model model) {
        var lista = financasService.listarDividas();
        model.addAttribute("dividas", lista);
        model.addAttribute("divida", new Divida());
        model.addAttribute("totalDividas", lista.stream()
                .map(d -> d.getValorRestante() != null ? d.getValorRestante() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        model.addAttribute("currentPage", "dividas");
        return "dividas";
    }

    @PostMapping("/dividas/salvar")
    public String salvarDivida(@ModelAttribute Divida divida) {
        financasService.salvarDivida(divida);
        return "redirect:/dividas";
    }


@GetMapping("/dividas/excluir/{id}")
    public String excluirDivida(@PathVariable Long id) {
        financasService.excluirDivida(id);
        return "redirect:/dividas";
    }

    // ===== FATURA LEGADO (/fatura) =====
    @GetMapping("/fatura")
    public String fatura(Model model) {
        model.addAttribute("faturasItens", financasService.listarFatura());
        model.addAttribute("faturaItem", new Divida());
        model.addAttribute("currentPage", "fatura");
        return "fatura";
    }

    @PostMapping("/fatura/salvar")
    public String salvarFaturaItem(@ModelAttribute Divida divida) {
        divida.setTipo("CARTAO");
        financasService.salvarDivida(divida);
        return "redirect:/fatura";
    }

    @GetMapping("/fatura/excluir/{id}")
    public String excluirFaturaItem(@PathVariable Long id) {
        financasService.excluirDivida(id);
        return "redirect:/fatura";
    }

    @GetMapping("/fatura/editar/{id}")
    public String editarFaturaItem(@PathVariable Long id, Model model) {
        financasService.buscarDivida(id).ifPresent(d -> model.addAttribute("faturaItem", d));
        model.addAttribute("faturasItens", financasService.listarFatura());
        model.addAttribute("currentPage", "fatura");
        return "fatura";
    }

    // ===== INVESTIMENTOS =====
    @GetMapping("/investimentos")
    public String investimentos(Model model) {
        var lista = financasService.listarInvestimentos();
        var contas = movimentacaoService.listarContas();
        java.math.BigDecimal saldoContas = contas.stream()
                .map(movimentacaoService::calcularSaldoAtual)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        model.addAttribute("investimentos", lista);
        model.addAttribute("investimento", new Investimento());
        model.addAttribute("contas", contas);
        model.addAttribute("totalInvestido", lista.stream()
                .map(Investimento::getValorInvestido)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        model.addAttribute("totalAtual", lista.stream()
                .map(Investimento::getValorAtual)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        model.addAttribute("saldoContas", saldoContas);
        model.addAttribute("currentPage", "investimentos");
        return "investimentos";
    }

    @PostMapping("/investimentos/salvar")
    public String salvarInvestimento(@ModelAttribute Investimento investimento) {
        financasService.salvarInvestimento(investimento);
        return "redirect:/investimentos";
    }

    @GetMapping("/investimentos/excluir/{id}")
    public String excluirInvestimento(@PathVariable Long id) {
        financasService.excluirInvestimento(id);
        return "redirect:/investimentos";
    }

    @GetMapping("/investimentos/{id}/resgatar")
    public String resgatarInvestimento(@PathVariable Long id,
                                       org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        try {
            financasService.resgatarInvestimento(id);
            ra.addFlashAttribute("sucesso", "Investimento resgatado! Valor creditado na conta.");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/investimentos";
    }

    // ── Helpers ───────────────────────────────────────────────────
    private java.util.List<java.util.Map<String, Object>> gerarMeses() {
        var lista = new java.util.ArrayList<java.util.Map<String, Object>>();
        for (int i = 5; i >= -3; i--) {
            YearMonth m = YearMonth.now().minusMonths(i);
            lista.add(java.util.Map.of(
                    "mes", m.getMonthValue(),
                    "ano", m.getYear(),
                    "label", nomeMes(m.getMonthValue()) + " " + m.getYear()
            ));
        }
        return lista;
    }

    private String nomeMes(int m) {
        return switch (m) {
            case 1 -> "Janeiro"; case 2 -> "Fevereiro"; case 3 -> "Março";
            case 4 -> "Abril";   case 5 -> "Maio";      case 6 -> "Junho";
            case 7 -> "Julho";   case 8 -> "Agosto";    case 9 -> "Setembro";
            case 10 -> "Outubro"; case 11 -> "Novembro"; case 12 -> "Dezembro";
            default -> "?";
        };
    }
}
