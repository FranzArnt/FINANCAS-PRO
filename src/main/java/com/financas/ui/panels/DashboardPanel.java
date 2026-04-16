package com.financas.ui.panels;

import com.financas.model.ContaBancaria;
import com.financas.model.FaturaCartao;
import com.financas.service.DashboardService;
import com.financas.service.DashboardService.DashboardDTO;
import com.financas.service.MovimentacaoService;
import com.financas.ui.util.UIUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public class DashboardPanel extends JPanel {

    private final DashboardService dashSvc;
    private final MovimentacaoService movSvc;

    private JComboBox<Object> cmbConta;
    private JLabel lblSaldo, lblReceitas, lblDespesas, lblProjetado;
    private JPanel chartsRow, faturasGrid;
    private boolean comboUpdating = false;

    public DashboardPanel(DashboardService dashSvc, MovimentacaoService movSvc) {
        this.dashSvc = dashSvc;
        this.movSvc  = movSvc;
        setBackground(UIUtils.BG);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        // ── Header ──────────────────────────────────────────────────
        JPanel header = UIUtils.headerPanel("Dashboard");
        cmbConta = new JComboBox<>();
        cmbConta.setPreferredSize(new Dimension(200, 32));
        cmbConta.addActionListener(e -> { if (!comboUpdating) refresh(); });
        header.add(cmbConta, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // ── Scrollable content ───────────────────────────────────────
        JPanel body = new JPanel();
        body.setBackground(UIUtils.BG);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(0, 24, 24, 24));

        // Stat cards
        JPanel stats = new JPanel(new GridLayout(1, 4, 12, 0));
        stats.setBackground(UIUtils.BG);
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
        lblSaldo     = addStatCard(stats, "Saldo Atual",       UIUtils.SUCCESS);
        lblReceitas  = addStatCard(stats, "Receitas do Mês",   UIUtils.SUCCESS);
        lblDespesas  = addStatCard(stats, "Despesas do Mês",   UIUtils.DANGER);
        lblProjetado = addStatCard(stats, "Saldo Projetado",   UIUtils.ACCENT);
        body.add(stats);
        body.add(Box.createVerticalStrut(16));

        // Charts
        chartsRow = new JPanel(new GridLayout(1, 2, 12, 0));
        chartsRow.setBackground(UIUtils.BG);
        chartsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        body.add(chartsRow);
        body.add(Box.createVerticalStrut(16));

        // Open invoices
        JLabel fatTitle = new JLabel("Faturas em Aberto");
        fatTitle.setFont(fatTitle.getFont().deriveFont(Font.BOLD, 16f));
        fatTitle.setForeground(UIUtils.TEXT_PRIMARY);
        fatTitle.setAlignmentX(LEFT_ALIGNMENT);
        body.add(fatTitle);
        body.add(Box.createVerticalStrut(8));

        faturasGrid = new JPanel(new GridLayout(0, 3, 12, 8));
        faturasGrid.setBackground(UIUtils.BG);
        faturasGrid.setAlignmentX(LEFT_ALIGNMENT);
        body.add(faturasGrid);

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UIUtils.BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    private JLabel addStatCard(JPanel parent, String title, Color color) {
        JPanel card = UIUtils.card(new BorderLayout(0, 6));
        JLabel t = new JLabel(title);
        t.setForeground(UIUtils.TEXT_SECONDARY);
        t.setFont(t.getFont().deriveFont(12f));
        JLabel v = new JLabel("R$ 0,00");
        v.setFont(v.getFont().deriveFont(Font.BOLD, 20f));
        v.setForeground(color);
        card.add(t, BorderLayout.NORTH);
        card.add(v, BorderLayout.CENTER);
        parent.add(card);
        return v;
    }

    public void refresh() {
        new SwingWorker<DashboardDTO, Void>() {
            private List<ContaBancaria> contas;

            @Override
            protected DashboardDTO doInBackground() {
                contas = dashSvc.listarContas();
                YearMonth mes = YearMonth.now();
                Object sel = cmbConta.getSelectedItem();
                if (sel instanceof ContaBancaria c) {
                    return dashSvc.montarDashboard(c, mes);
                }
                return dashSvc.montarDashboardTodos(mes);
            }

            @Override
            protected void done() {
                try {
                    DashboardDTO dto = get();
                    updateCombo(contas);

                    lblSaldo.setText(UIUtils.formatCurrency(dto.saldoAtual()));
                    lblSaldo.setForeground(dto.saldoAtual().compareTo(BigDecimal.ZERO) >= 0
                            ? UIUtils.SUCCESS : UIUtils.DANGER);
                    lblReceitas.setText(UIUtils.formatCurrency(dto.receitasMes()));
                    lblDespesas.setText(UIUtils.formatCurrency(dto.despesasMes()));
                    lblProjetado.setText(UIUtils.formatCurrency(dto.saldoProjetado()));
                    lblProjetado.setForeground(dto.saldoProjetado().compareTo(BigDecimal.ZERO) >= 0
                            ? UIUtils.ACCENT : UIUtils.DANGER);

                    buildCharts(dto);
                    buildFaturas(dto.faturasAbertas());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    private void updateCombo(List<ContaBancaria> contas) {
        Object sel = cmbConta.getSelectedItem();
        comboUpdating = true;
        cmbConta.removeAllItems();
        cmbConta.addItem("Todas as contas");
        contas.forEach(cmbConta::addItem);
        if (sel instanceof ContaBancaria c) {
            for (int i = 0; i < cmbConta.getItemCount(); i++) {
                if (cmbConta.getItemAt(i) instanceof ContaBancaria cb && cb.getId().equals(c.getId())) {
                    cmbConta.setSelectedIndex(i);
                    break;
                }
            }
        }
        comboUpdating = false;
    }

    private void buildCharts(DashboardDTO dto) {
        chartsRow.removeAll();

        // Bar chart — histórico 6 meses
        DefaultCategoryDataset barDs = new DefaultCategoryDataset();
        List<String> meses = dto.mesesGrafico();
        List<BigDecimal> entr = dto.entradasGrafico();
        List<BigDecimal> desp = dto.despesasGrafico();
        for (int i = 0; i < meses.size(); i++) {
            barDs.addValue(entr.get(i).doubleValue(), "Receitas", meses.get(i));
            barDs.addValue(desp.get(i).doubleValue(), "Despesas", meses.get(i));
        }
        JFreeChart barChart = ChartFactory.createBarChart(
                "Histórico 6 Meses", "", "R$", barDs, PlotOrientation.VERTICAL, true, false, false);
        barChart.setBackgroundPaint(Color.WHITE);
        CategoryPlot plot = barChart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setRangeGridlinePaint(UIUtils.BORDER);
        BarRenderer br = (BarRenderer) plot.getRenderer();
        br.setSeriesPaint(0, UIUtils.SUCCESS);
        br.setSeriesPaint(1, UIUtils.DANGER);
        br.setShadowVisible(false);
        br.setMaximumBarWidth(0.1);

        JPanel barCard = UIUtils.card(new BorderLayout());
        barCard.setBorder(new EmptyBorder(8, 8, 8, 8));
        ChartPanel cp = new ChartPanel(barChart);
        cp.setMouseWheelEnabled(false);
        barCard.add(cp, BorderLayout.CENTER);
        chartsRow.add(barCard);

        // Pie chart — gastos por categoria
        DefaultPieDataset<String> pieDs = new DefaultPieDataset<>();
        dto.gastosPorCategoria().forEach((k, v) -> {
            if (v.compareTo(BigDecimal.ZERO) > 0) pieDs.setValue(k, v.doubleValue());
        });
        JFreeChart pieChart = ChartFactory.createPieChart(
                "Despesas por Categoria", pieDs, true, false, false);
        pieChart.setBackgroundPaint(Color.WHITE);
        pieChart.getPlot().setBackgroundPaint(Color.WHITE);

        JPanel pieCard = UIUtils.card(new BorderLayout());
        pieCard.setBorder(new EmptyBorder(8, 8, 8, 8));
        ChartPanel cp2 = new ChartPanel(pieChart);
        cp2.setMouseWheelEnabled(false);
        pieCard.add(cp2, BorderLayout.CENTER);
        chartsRow.add(pieCard);

        chartsRow.revalidate();
        chartsRow.repaint();
    }

    private void buildFaturas(List<FaturaCartao> faturas) {
        faturasGrid.removeAll();
        if (faturas == null || faturas.isEmpty()) {
            JLabel empty = new JLabel("Nenhuma fatura em aberto");
            empty.setForeground(UIUtils.TEXT_SECONDARY);
            faturasGrid.setLayout(new FlowLayout(FlowLayout.LEFT));
            faturasGrid.add(empty);
        } else {
            faturasGrid.setLayout(new GridLayout(0, 3, 12, 8));
            for (FaturaCartao f : faturas) {
                JPanel card = UIUtils.card(new BorderLayout(0, 4));
                JLabel nome = new JLabel(f.getCartao().getNome());
                nome.setFont(nome.getFont().deriveFont(Font.BOLD, 14f));
                nome.setForeground(UIUtils.TEXT_PRIMARY);
                JLabel venc = new JLabel("Vence: " + UIUtils.formatDate(f.getDataVencimento()));
                venc.setForeground(UIUtils.TEXT_SECONDARY);
                venc.setFont(venc.getFont().deriveFont(12f));
                JLabel valor = new JLabel(UIUtils.formatCurrency(f.getValor()));
                valor.setFont(valor.getFont().deriveFont(Font.BOLD, 18f));
                valor.setForeground(UIUtils.DANGER);
                card.add(nome, BorderLayout.NORTH);
                card.add(valor, BorderLayout.CENTER);
                card.add(venc, BorderLayout.SOUTH);
                faturasGrid.add(card);
            }
        }
        faturasGrid.revalidate();
        faturasGrid.repaint();
    }
}
