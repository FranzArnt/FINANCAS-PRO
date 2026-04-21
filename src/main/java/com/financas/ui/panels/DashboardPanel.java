package com.financas.ui.panels;

import com.financas.model.*;
import com.financas.service.*;
import com.financas.service.DashboardService.DashboardDTO;
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
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardPanel extends JPanel {

    private final DashboardService      dashSvc;
    private final MovimentacaoService   movSvc;
    private final ConfiguracoesService  confSvc;

    // Filtros
    private YearMonth              selectedMonth = YearMonth.now();
    private JLabel                 lblMes;
    private JComboBox<Object>      cmbConta;
    private JComboBox<Object>      cmbCategoria;
    private boolean                updating = false;

    // Conteúdo
    private JLabel     lblSaldo, lblReceitas, lblDespesas, lblProjetado, lblInvestido;
    private JPanel     barChartPanel, barMensalPanel, pieChartPanel, faturasGrid;
    private DashboardDTO lastDto;

    public DashboardPanel(DashboardService dashSvc, MovimentacaoService movSvc,
                          ConfiguracoesService confSvc, CartaoService cartaoSvc) {
        this.dashSvc  = dashSvc;
        this.movSvc   = movSvc;
        this.confSvc  = confSvc;
        setBackground(UIUtils.BG);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(UIUtils.BG);
        top.add(UIUtils.headerPanel("Dashboard"), BorderLayout.NORTH);
        top.add(buildFilterBar(), BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setBackground(UIUtils.BG);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(0, 24, 24, 24));

        JPanel stats = new JPanel(new GridLayout(1, 5, 12, 0));
        stats.setBackground(UIUtils.BG);
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
        lblSaldo     = addStatCard(stats, "Saldo Atual",      UIUtils.SUCCESS);
        lblReceitas  = addStatCard(stats, "Receitas do Mês",  UIUtils.SUCCESS);
        lblDespesas  = addStatCard(stats, "Despesas do Mês",  UIUtils.DANGER);
        lblProjetado = addStatCard(stats, "Saldo Projetado",  UIUtils.ACCENT);
        lblInvestido = addStatCard(stats, "Total Investido",  new Color(0x8B5CF6));
        body.add(stats);
        body.add(Box.createVerticalStrut(16));

        // Gráfico de barras — histórico 6 meses
        // Gráfico semanal do mês selecionado (primeiro)
        JLabel mensalTitle = new JLabel("Receitas × Despesas — Semanas do Mês");
        mensalTitle.setFont(mensalTitle.getFont().deriveFont(Font.BOLD, 16f));
        mensalTitle.setForeground(UIUtils.TEXT_PRIMARY);
        mensalTitle.setAlignmentX(LEFT_ALIGNMENT);
        body.add(mensalTitle);
        body.add(Box.createVerticalStrut(8));
        barMensalPanel = UIUtils.card(new BorderLayout());
        barMensalPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        barMensalPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));
        body.add(barMensalPanel);
        body.add(Box.createVerticalStrut(16));

        // Gráfico histórico 6 meses (depois)
        JLabel histTitle = new JLabel("Histórico 6 Meses");
        histTitle.setFont(histTitle.getFont().deriveFont(Font.BOLD, 16f));
        histTitle.setForeground(UIUtils.TEXT_PRIMARY);
        histTitle.setAlignmentX(LEFT_ALIGNMENT);
        body.add(histTitle);
        body.add(Box.createVerticalStrut(8));
        barChartPanel = UIUtils.card(new BorderLayout());
        barChartPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        barChartPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        body.add(barChartPanel);
        body.add(Box.createVerticalStrut(16));

        // Faturas em aberto
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
        body.add(Box.createVerticalStrut(16));

        // Gráfico de pizza — abaixo das faturas
        JLabel catTitle = new JLabel("Despesas por Categoria");
        catTitle.setFont(catTitle.getFont().deriveFont(Font.BOLD, 16f));
        catTitle.setForeground(UIUtils.TEXT_PRIMARY);
        catTitle.setAlignmentX(LEFT_ALIGNMENT);
        body.add(catTitle);
        body.add(Box.createVerticalStrut(8));

        pieChartPanel = UIUtils.card(new BorderLayout());
        pieChartPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        pieChartPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        body.add(pieChartPanel);

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UIUtils.BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel buildFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        bar.setBackground(UIUtils.BG);
        bar.setBorder(new EmptyBorder(0, 24, 0, 24));

        // Navegação de mês
        JButton btnAnterior = new JButton("◀");
        styleFilterBtn(btnAnterior);
        btnAnterior.addActionListener(e -> { selectedMonth = selectedMonth.minusMonths(1); updateMesLabel(); refresh(); });

        lblMes = new JLabel(formatMes(selectedMonth));
        lblMes.setFont(lblMes.getFont().deriveFont(Font.BOLD, 13f));
        lblMes.setForeground(UIUtils.ACCENT);
        lblMes.setPreferredSize(new Dimension(120, 28));
        lblMes.setHorizontalAlignment(SwingConstants.CENTER);
        lblMes.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblMes.setToolTipText("Clique para escolher mês");
        lblMes.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) { openMonthPicker(); }
        });

        JButton btnProximo = new JButton("▶");
        styleFilterBtn(btnProximo);
        btnProximo.addActionListener(e -> { selectedMonth = selectedMonth.plusMonths(1); updateMesLabel(); refresh(); });

        JButton btnHoje = new JButton("Hoje");
        styleFilterBtn(btnHoje);
        btnHoje.addActionListener(e -> { selectedMonth = YearMonth.now(); updateMesLabel(); refresh(); });

        // Conta
        cmbConta = new JComboBox<>();
        cmbConta.setPreferredSize(new Dimension(160, 28));
        cmbConta.addActionListener(e -> { if (!updating) applyFilters(); });

        // Categoria
        cmbCategoria = new JComboBox<>();
        cmbCategoria.setPreferredSize(new Dimension(150, 28));
        cmbCategoria.addActionListener(e -> { if (!updating) applyFilters(); });

        bar.add(btnAnterior);
        bar.add(lblMes);
        bar.add(btnProximo);
        bar.add(btnHoje);
        bar.add(new JLabel("Conta:"));
        bar.add(cmbConta);
        bar.add(new JLabel("Categoria:"));
        bar.add(cmbCategoria);

        return bar;
    }

    private void styleFilterBtn(JButton btn) {
        btn.setPreferredSize(new Dimension(32, 28));
        btn.setFocusPainted(false);
        btn.setFont(btn.getFont().deriveFont(11f));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void updateMesLabel() {
        lblMes.setText(formatMes(selectedMonth));
    }

    private String formatMes(YearMonth ym) {
        String mes = ym.getMonth().getDisplayName(TextStyle.FULL, new Locale("pt", "BR"));
        return mes.substring(0, 1).toUpperCase() + mes.substring(1) + " " + ym.getYear();
    }

    private void openMonthPicker() {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "Selecionar Mês", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(320, 260);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(8, 8));

        String[] meses = {"Janeiro","Fevereiro","Março","Abril","Maio","Junho",
                          "Julho","Agosto","Setembro","Outubro","Novembro","Dezembro"};

        // Navegação do ano
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        topBar.setBackground(UIUtils.BG);
        JButton btnAnoAntes = new JButton("◀");
        JLabel  lblAno      = new JLabel(String.valueOf(selectedMonth.getYear()));
        lblAno.setFont(lblAno.getFont().deriveFont(Font.BOLD, 14f));
        lblAno.setPreferredSize(new Dimension(60, 24));
        lblAno.setHorizontalAlignment(SwingConstants.CENTER);
        JButton btnAnoDepois = new JButton("▶");
        btnAnoAntes.addActionListener(e  -> lblAno.setText(String.valueOf(Integer.parseInt(lblAno.getText()) - 1)));
        btnAnoDepois.addActionListener(e -> lblAno.setText(String.valueOf(Integer.parseInt(lblAno.getText()) + 1)));
        topBar.add(btnAnoAntes); topBar.add(lblAno); topBar.add(btnAnoDepois);
        dlg.add(topBar, BorderLayout.NORTH);

        // Grade de meses
        JPanel grid = new JPanel(new GridLayout(3, 4, 6, 6));
        grid.setBackground(UIUtils.BG);
        grid.setBorder(new EmptyBorder(8, 12, 8, 12));
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < 12; i++) {
            final int idx = i;
            JToggleButton btn = new JToggleButton(meses[i].substring(0, 3));
            btn.setFocusPainted(false);
            if (i + 1 == selectedMonth.getMonthValue()) btn.setSelected(true);
            group.add(btn);
            btn.addActionListener(e -> {
                int ano = Integer.parseInt(lblAno.getText());
                selectedMonth = YearMonth.of(ano, idx + 1);
                updateMesLabel();
                dlg.dispose();
                refresh();
            });
            grid.add(btn);
        }
        dlg.add(grid, BorderLayout.CENTER);
        dlg.setVisible(true);
    }

    public void refresh() {
        new SwingWorker<DashboardDTO, Void>() {
            private List<ContaBancaria> contas;
            private List<Categoria> categorias;

            @Override
            protected DashboardDTO doInBackground() {
                contas     = dashSvc.listarContas();
                categorias = confSvc.listarTodasAtivas();

                Object sel = cmbConta.getSelectedItem();
                if (sel instanceof ContaBancaria c) {
                    return dashSvc.montarDashboard(c, selectedMonth);
                }
                return dashSvc.montarDashboardTodos(selectedMonth);
            }

            @Override
            protected void done() {
                try {
                    lastDto = get();
                    populateCombos(contas, categorias);
                    applyFilters();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    private void populateCombos(List<ContaBancaria> contas, List<Categoria> categorias) {
        updating = true;

        Object selConta = cmbConta.getSelectedItem();
        cmbConta.removeAllItems();
        cmbConta.addItem("Todas as contas");
        contas.forEach(cmbConta::addItem);
        restoreSelection(cmbConta, selConta);

        Object selCat = cmbCategoria.getSelectedItem();
        cmbCategoria.removeAllItems();
        cmbCategoria.addItem("Todas as categorias");
        categorias.forEach(cmbCategoria::addItem);
        restoreSelection(cmbCategoria, selCat);

        updating = false;
    }

    private void restoreSelection(JComboBox<Object> cmb, Object prev) {
        if (prev == null) return;
        for (int i = 0; i < cmb.getItemCount(); i++) {
            Object item = cmb.getItemAt(i);
            if (item != null && item.toString().equals(prev.toString())) {
                cmb.setSelectedIndex(i);
                return;
            }
        }
    }

    private void applyFilters() {
        if (lastDto == null) return;

        // Filtro de categoria no gráfico de pizza e stat de despesas
        Object selCat = cmbCategoria.getSelectedItem();
        Map<String, BigDecimal> gastos = lastDto.gastosPorCategoria();
        BigDecimal despesasExibidas = lastDto.despesasMes();

        if (selCat instanceof Categoria cat) {
            despesasExibidas = gastos.getOrDefault(cat.getNome(), BigDecimal.ZERO);
            gastos = Map.of(cat.getNome(), despesasExibidas);
        }

        List<FaturaCartao> faturas = lastDto.faturasAbertas();

        // Atualiza stats
        lblSaldo.setText(UIUtils.formatCurrency(lastDto.saldoAtual()));
        lblSaldo.setForeground(lastDto.saldoAtual().compareTo(BigDecimal.ZERO) >= 0 ? UIUtils.SUCCESS : UIUtils.DANGER);
        lblReceitas.setText(UIUtils.formatCurrency(lastDto.receitasMes()));
        lblDespesas.setText(UIUtils.formatCurrency(despesasExibidas));
        lblProjetado.setText(UIUtils.formatCurrency(lastDto.saldoProjetado()));
        lblProjetado.setForeground(lastDto.saldoProjetado().compareTo(BigDecimal.ZERO) >= 0 ? UIUtils.ACCENT : UIUtils.DANGER);
        lblInvestido.setText(UIUtils.formatCurrency(lastDto.totalInvestido()));

        buildCharts(lastDto, gastos);
        buildBarMensal(lastDto);
        buildFaturas(faturas);
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

    private void buildCharts(DashboardDTO dto, Map<String, BigDecimal> gastos) {
        // Gráfico de barras
        barChartPanel.removeAll();
        DefaultCategoryDataset barDs = new DefaultCategoryDataset();
        List<String> meses    = dto.mesesGrafico();
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
        br.setMaximumBarWidth(0.08);
        ChartPanel cp = new ChartPanel(barChart);
        cp.setMouseWheelEnabled(false);
        barChartPanel.add(cp, BorderLayout.CENTER);
        barChartPanel.revalidate();
        barChartPanel.repaint();

        // Gráfico de pizza
        pieChartPanel.removeAll();
        DefaultPieDataset<String> pieDs = new DefaultPieDataset<>();
        gastos.forEach((k, v) -> { if (v.compareTo(BigDecimal.ZERO) > 0) pieDs.setValue(k, v.doubleValue()); });
        JFreeChart pieChart = ChartFactory.createPieChart(
                "", pieDs, true, false, false);
        pieChart.setBackgroundPaint(Color.WHITE);
        pieChart.getPlot().setBackgroundPaint(Color.WHITE);
        ChartPanel cp2 = new ChartPanel(pieChart);
        cp2.setMouseWheelEnabled(false);
        pieChartPanel.add(cp2, BorderLayout.CENTER);
        pieChartPanel.revalidate();
        pieChartPanel.repaint();
    }

    private void buildBarMensal(DashboardDTO dto) {
        barMensalPanel.removeAll();
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        List<String> semanas     = dto.semanasGrafico();
        List<BigDecimal> entSem  = dto.entradasSemanais();
        List<BigDecimal> despSem = dto.despesasSemanais();
        for (int i = 0; i < semanas.size(); i++) {
            ds.addValue(entSem.get(i).doubleValue(),  "Receitas", semanas.get(i));
            ds.addValue(despSem.get(i).doubleValue(), "Despesas", semanas.get(i));
        }
        String titulo = formatMes(dto.mes());
        JFreeChart chart = ChartFactory.createBarChart(
                titulo, "", "R$", ds, PlotOrientation.VERTICAL, true, false, false);
        chart.setBackgroundPaint(Color.WHITE);
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setRangeGridlinePaint(UIUtils.BORDER);
        BarRenderer br = (BarRenderer) plot.getRenderer();
        br.setSeriesPaint(0, UIUtils.SUCCESS);
        br.setSeriesPaint(1, UIUtils.DANGER);
        br.setShadowVisible(false);
        br.setMaximumBarWidth(0.15);
        ChartPanel cp = new ChartPanel(chart);
        cp.setMouseWheelEnabled(false);
        barMensalPanel.add(cp, BorderLayout.CENTER);
        barMensalPanel.revalidate();
        barMensalPanel.repaint();
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
