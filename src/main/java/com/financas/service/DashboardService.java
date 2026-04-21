package com.financas.service;

import com.financas.model.*;
import com.financas.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
public class DashboardService {

    private final MovimentacaoRepository movRepo;
    private final FaturaRepository faturaRepo;
    private final ContaBancariaRepository contaRepo;
    private final InvestimentoRepository investimentoRepo;
    private final MovimentacaoService movService;
    private final FaturaService faturaService;

    public DashboardService(MovimentacaoRepository movRepo,
                            FaturaRepository faturaRepo,
                            ContaBancariaRepository contaRepo,
                            InvestimentoRepository investimentoRepo,
                            MovimentacaoService movService,
                            FaturaService faturaService) {
        this.movRepo = movRepo;
        this.faturaRepo = faturaRepo;
        this.contaRepo = contaRepo;
        this.investimentoRepo = investimentoRepo;
        this.movService = movService;
        this.faturaService = faturaService;
    }

    public record DashboardDTO(
            ContaBancaria conta,
            YearMonth mes,
            BigDecimal receitasMes,
            BigDecimal despesasMes,
            BigDecimal saldoAtual,
            BigDecimal saldoProjetado,
            BigDecimal mediaHistorica,
            BigDecimal totalInvestido,
            List<FaturaCartao> faturasAbertas,
            List<FaturaCartao> faturasAVencer7d,
            Map<String, BigDecimal> gastosPorCategoria,
            List<String> mesesGrafico,
            List<BigDecimal> despesasGrafico,
            List<BigDecimal> entradasGrafico,
            List<String> semanasGrafico,
            List<BigDecimal> despesasSemanais,
            List<BigDecimal> entradasSemanais
    ) {}

    public DashboardDTO montarDashboard(ContaBancaria conta, YearMonth mes) {
        BigDecimal receitasMes  = movRepo.sumEntradasByContaAndMes(conta, mes.getYear(), mes.getMonthValue());
        BigDecimal despesasMes  = movRepo.sumDespesasByContaAndMes(conta, mes.getYear(), mes.getMonthValue());
        BigDecimal saldoAtual   = movService.calcularSaldoAtual(conta);
        BigDecimal saldoProjeto = movService.calcularSaldoProjetado(conta, mes);
        BigDecimal media        = movService.calcularMediaMensalDespesas(conta, 3);

        List<FaturaCartao> abertas    = faturaService.listarAbertas().stream()
                .filter(f -> f.getConta() != null && f.getConta().getId().equals(conta.getId()))
                .toList();
        List<FaturaCartao> aVencer7d  = faturaService.faturasAVencer(7).stream()
                .filter(f -> f.getConta() != null && f.getConta().getId().equals(conta.getId()))
                .toList();

        // Gastos por categoria
        Map<String, BigDecimal> gastos = new LinkedHashMap<>();
        for (Object[] row : movRepo.gastosPorCategoriaMes(conta, mes.getYear(), mes.getMonthValue())) {
            Categoria cat = (Categoria) row[0];
            BigDecimal val = (BigDecimal) row[1];
            gastos.put(cat.getNome(), val);
        }

        // Histórico 6 meses para gráfico
        List<String> mesesGrafico    = new ArrayList<>();
        List<BigDecimal> despGrafico = new ArrayList<>();
        List<BigDecimal> entGrafico  = new ArrayList<>();

        Map<String, BigDecimal> mapDesp = new LinkedHashMap<>();
        Map<String, BigDecimal> mapEnt  = new LinkedHashMap<>();

        for (Object[] row : movService.historicoDespesasMensal(conta, 6)) {
            String chave = nomeMes(((Number) row[1]).intValue()) + "/" + String.valueOf(((Number) row[0]).intValue()).substring(2);
            mapDesp.put(chave, (BigDecimal) row[2]);
        }
        for (Object[] row : movService.historicoEntradasMensal(conta, 6)) {
            String chave = nomeMes(((Number) row[1]).intValue()) + "/" + String.valueOf(((Number) row[0]).intValue()).substring(2);
            mapEnt.put(chave, (BigDecimal) row[2]);
        }

        Set<String> chaves = new LinkedHashSet<>(mapDesp.keySet());
        chaves.addAll(mapEnt.keySet());
        for (String c : chaves) {
            mesesGrafico.add(c);
            despGrafico.add(mapDesp.getOrDefault(c, BigDecimal.ZERO));
            entGrafico.add(mapEnt.getOrDefault(c, BigDecimal.ZERO));
        }

        BigDecimal totalInvestido = calcularTotalInvestido();
        DadosSemanais semanais = calcularSemanais(List.of(conta), mes);

        return new DashboardDTO(conta, mes, receitasMes, despesasMes, saldoAtual, saldoProjeto,
                media, totalInvestido, abertas, aVencer7d, gastos,
                mesesGrafico, despGrafico, entGrafico,
                semanais.semanas(), semanais.despesas(), semanais.entradas());
    }

    public DashboardDTO montarDashboardTodos(YearMonth mes) {
        List<ContaBancaria> contas = listarContas();

        BigDecimal receitasMes = contas.stream()
                .map(c -> movRepo.sumEntradasByContaAndMes(c, mes.getYear(), mes.getMonthValue()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal despesasMes = contas.stream()
                .map(c -> movRepo.sumDespesasByContaAndMes(c, mes.getYear(), mes.getMonthValue()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal saldoAtual = contas.stream()
                .map(movService::calcularSaldoAtual)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal saldoProjetado = contas.stream()
                .map(c -> movService.calcularSaldoProjetado(c, mes))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal media = contas.stream()
                .map(c -> movService.calcularMediaMensalDespesas(c, 3))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<FaturaCartao> abertas   = faturaService.listarAbertas();
        List<FaturaCartao> aVencer7d = faturaService.faturasAVencer(7);

        // Gastos por categoria — agrega todas as contas
        Map<String, BigDecimal> gastos = new LinkedHashMap<>();
        for (ContaBancaria c : contas) {
            for (Object[] row : movRepo.gastosPorCategoriaMes(c, mes.getYear(), mes.getMonthValue())) {
                String nome = ((Categoria) row[0]).getNome();
                BigDecimal val = (BigDecimal) row[1];
                gastos.merge(nome, val, BigDecimal::add);
            }
        }

        // Histórico 6 meses — agrega todas as contas
        Map<String, BigDecimal> mapDesp = new LinkedHashMap<>();
        Map<String, BigDecimal> mapEnt  = new LinkedHashMap<>();
        for (ContaBancaria c : contas) {
            for (Object[] row : movService.historicoDespesasMensal(c, 6)) {
                String chave = nomeMes(((Number) row[1]).intValue()) + "/" + String.valueOf(((Number) row[0]).intValue()).substring(2);
                mapDesp.merge(chave, (BigDecimal) row[2], BigDecimal::add);
            }
            for (Object[] row : movService.historicoEntradasMensal(c, 6)) {
                String chave = nomeMes(((Number) row[1]).intValue()) + "/" + String.valueOf(((Number) row[0]).intValue()).substring(2);
                mapEnt.merge(chave, (BigDecimal) row[2], BigDecimal::add);
            }
        }

        List<String> mesesGrafico    = new ArrayList<>();
        List<BigDecimal> despGrafico = new ArrayList<>();
        List<BigDecimal> entGrafico  = new ArrayList<>();
        Set<String> chaves = new LinkedHashSet<>(mapDesp.keySet());
        chaves.addAll(mapEnt.keySet());
        for (String c : chaves) {
            mesesGrafico.add(c);
            despGrafico.add(mapDesp.getOrDefault(c, BigDecimal.ZERO));
            entGrafico.add(mapEnt.getOrDefault(c, BigDecimal.ZERO));
        }

        BigDecimal totalInvestido = calcularTotalInvestido();
        DadosSemanais semanais = calcularSemanais(contas, mes);

        return new DashboardDTO(null, mes, receitasMes, despesasMes, saldoAtual, saldoProjetado,
                media, totalInvestido, abertas, aVencer7d, gastos,
                mesesGrafico, despGrafico, entGrafico,
                semanais.semanas(), semanais.despesas(), semanais.entradas());
    }

    public List<ContaBancaria> listarContas() {
        return contaRepo.findByAtivaTrue();
    }

    public BigDecimal calcularTotalInvestido() {
        BigDecimal total = investimentoRepo.somarTotalAtual();
        return total != null ? total : BigDecimal.ZERO;
    }

    public record DadosSemanais(List<String> semanas, List<BigDecimal> despesas, List<BigDecimal> entradas) {}

    private DadosSemanais calcularSemanais(List<ContaBancaria> contas, YearMonth mes) {
        int[][] faixas = {{1,7},{8,14},{15,21},{22,mes.lengthOfMonth()}};
        List<String> semanas = List.of("Sem 1","Sem 2","Sem 3","Sem 4");
        List<BigDecimal> despSem = new ArrayList<>(List.of(BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO));
        List<BigDecimal> entSem  = new ArrayList<>(List.of(BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO));

        for (ContaBancaria conta : contas) {
            List<Movimentacao> movs = movRepo.findByContaAndMes(conta, mes.getYear(), mes.getMonthValue());
            for (Movimentacao m : movs) {
                int dia = m.getData().getDayOfMonth();
                for (int s = 0; s < 4; s++) {
                    if (dia >= faixas[s][0] && dia <= faixas[s][1]) {
                        if (m instanceof Transacao t) {
                            if (t.getDirecao() == DirecaoTransacao.SAIDA)
                                despSem.set(s, despSem.get(s).add(m.getValor()));
                            else if (t.getDirecao() == DirecaoTransacao.ENTRADA)
                                entSem.set(s, entSem.get(s).add(m.getValor()));
                        } else if (m instanceof FaturaCartao f && f.getStatus() != StatusFatura.PAGA) {
                            despSem.set(s, despSem.get(s).add(m.getValor()));
                        }
                        break;
                    }
                }
            }
        }
        return new DadosSemanais(semanas, despSem, entSem);
    }

    private String nomeMes(int m) {
        return switch (m) {
            case 1 -> "Jan"; case 2 -> "Fev"; case 3 -> "Mar";
            case 4 -> "Abr"; case 5 -> "Mai"; case 6 -> "Jun";
            case 7 -> "Jul"; case 8 -> "Ago"; case 9 -> "Set";
            case 10 -> "Out"; case 11 -> "Nov"; case 12 -> "Dez";
            default -> "?";
        };
    }
}
