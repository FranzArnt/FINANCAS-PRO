package com.financas.ui.panels;

import com.financas.model.*;
import com.financas.service.CartaoService;
import com.financas.service.ConfiguracoesService;
import com.financas.service.FaturaService;
import com.financas.service.MovimentacaoService;
import com.financas.ui.util.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;

public class CartoesPanel extends JPanel {

    private final CartaoService     cartaoSvc;
    private final FaturaService     faturaSvc;
    private final ConfiguracoesService confSvc;
    private final MovimentacaoService  movSvc;

    private JPanel cardsGrid;

    public CartoesPanel(CartaoService cartaoSvc, FaturaService faturaSvc,
                        ConfiguracoesService confSvc, MovimentacaoService movSvc) {
        this.cartaoSvc = cartaoSvc;
        this.faturaSvc = faturaSvc;
        this.confSvc   = confSvc;
        this.movSvc    = movSvc;
        setBackground(UIUtils.BG);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel header = UIUtils.headerPanel("Cartões de Crédito");
        JButton btnNovo = UIUtils.primaryBtn("+ Novo Cartão");
        btnNovo.addActionListener(e -> openForm(null));
        header.add(btnNovo, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        cardsGrid = new JPanel(new GridLayout(0, 3, 16, 16));
        cardsGrid.setBackground(UIUtils.BG);
        cardsGrid.setBorder(new EmptyBorder(0, 24, 24, 24));

        JScrollPane scroll = new JScrollPane(cardsGrid);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UIUtils.BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    public void refresh() {
        new SwingWorker<List<CartaoCredito>, Void>() {
            @Override protected List<CartaoCredito> doInBackground() {
                return cartaoSvc.listarAtivos();
            }
            @Override protected void done() {
                try { buildCards(get()); } catch (Exception ex) { ex.printStackTrace(); }
            }
        }.execute();
    }

    private void buildCards(List<CartaoCredito> cartoes) {
        cardsGrid.removeAll();
        if (cartoes.isEmpty()) {
            JLabel empty = new JLabel("Nenhum cartão cadastrado. Clique em '+ Novo Cartão'.");
            empty.setForeground(UIUtils.TEXT_SECONDARY);
            cardsGrid.setLayout(new FlowLayout(FlowLayout.LEFT));
            cardsGrid.add(empty);
        } else {
            cardsGrid.setLayout(new GridLayout(0, 3, 16, 16));
            for (CartaoCredito c : cartoes) {
                cardsGrid.add(buildCartaoCard(c));
            }
        }
        cardsGrid.revalidate();
        cardsGrid.repaint();
    }

    private JPanel buildCartaoCard(CartaoCredito c) {
        JPanel card = UIUtils.card(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(16, 16, 16, 16));

        // Top row: name + bandeira
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        JLabel nome = new JLabel(c.getNome());
        nome.setFont(nome.getFont().deriveFont(Font.BOLD, 15f));
        nome.setForeground(UIUtils.TEXT_PRIMARY);
        JLabel band = UIUtils.badge(c.getBandeira().name(), UIUtils.ACCENT);
        top.add(nome, BorderLayout.WEST);
        top.add(band, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        // Middle: limit bar + info
        JPanel mid = new JPanel();
        mid.setBackground(Color.WHITE);
        mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));

        BigDecimal disponivel = cartaoSvc.calcularLimiteDisponivel(c);
        BigDecimal utilizado  = c.getLimiteTotal().subtract(disponivel);
        int pct = c.getLimiteTotal().compareTo(BigDecimal.ZERO) > 0
                ? utilizado.multiply(BigDecimal.valueOf(100))
                        .divide(c.getLimiteTotal(), 0, RoundingMode.HALF_UP).intValue()
                : 0;

        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(pct);
        bar.setString(pct + "% utilizado");
        bar.setStringPainted(true);
        bar.setForeground(pct > 80 ? UIUtils.DANGER : UIUtils.ACCENT);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        mid.add(bar);
        mid.add(Box.createVerticalStrut(6));

        JLabel limDisp = new JLabel("Disponível: " + UIUtils.formatCurrency(disponivel));
        limDisp.setForeground(UIUtils.TEXT_SECONDARY);
        limDisp.setFont(limDisp.getFont().deriveFont(12f));
        mid.add(limDisp);

        JLabel fechVenc = new JLabel("Fecha dia " + c.getDiaFechamento() + " · Vence dia " + c.getDiaVencimento());
        fechVenc.setForeground(UIUtils.TEXT_SECONDARY);
        fechVenc.setFont(fechVenc.getFont().deriveFont(11f));
        mid.add(fechVenc);

        card.add(mid, BorderLayout.CENTER);

        // Buttons
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        btns.setBackground(Color.WHITE);

        JButton btnFaturas = UIUtils.secondaryBtn("Faturas");
        btnFaturas.addActionListener(e -> openFaturas(c));

        JButton btnEditar = UIUtils.secondaryBtn("Editar");
        btnEditar.addActionListener(e -> openForm(c));

        JButton btnDesativar = UIUtils.dangerBtn("Desativar");
        btnDesativar.addActionListener(e -> {
            int r = JOptionPane.showConfirmDialog(this,
                    "Desativar o cartão " + c.getNome() + "?", "Confirmar",
                    JOptionPane.YES_NO_OPTION);
            if (r == JOptionPane.YES_OPTION) {
                cartaoSvc.desativar(c.getId());
                refresh();
            }
        });

        btns.add(btnFaturas);
        btns.add(btnEditar);
        btns.add(btnDesativar);
        card.add(btns, BorderLayout.SOUTH);

        return card;
    }

    private void openFaturas(CartaoCredito cartao) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Faturas — " + cartao.getNome(), Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(700, 500);
        dlg.setLocationRelativeTo(this);

        String[] cols = {"Mês", "Fechamento", "Vencimento", "Valor", "Status", "Ações"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        UIUtils.styleTable(table);

        List<FaturaCartao> faturas = faturaSvc.listarPorCartao(cartao);
        for (FaturaCartao f : faturas) {
            model.addRow(new Object[]{
                    f.getMesReferencia(),
                    UIUtils.formatDate(f.getDataFechamento()),
                    UIUtils.formatDate(f.getDataVencimento()),
                    UIUtils.formatCurrency(f.getValor()),
                    f.getStatus().name()
            });
        }

        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnVer    = UIUtils.primaryBtn("Ver Fatura");
        JButton btnPagar  = UIUtils.primaryBtn("Pagar");
        JButton btnFechar = UIUtils.secondaryBtn("Fechar Fatura");

        btnVer.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(dlg, "Selecione uma fatura."); return; }
            FaturaCartao f = faturas.get(row);
            dlg.dispose();
            openFaturaDetalhe(f);
        });

        btnPagar.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(dlg, "Selecione uma fatura."); return; }
            FaturaCartao f = faturas.get(row);
            try {
                faturaSvc.pagarFatura(f.getId());
                JOptionPane.showMessageDialog(dlg, "Fatura paga com sucesso!");
                dlg.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnFechar.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(dlg, "Selecione uma fatura."); return; }
            FaturaCartao f = faturas.get(row);
            try {
                faturaSvc.fecharFatura(f.getId());
                JOptionPane.showMessageDialog(dlg, "Fatura fechada!");
                dlg.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        actionBar.add(btnFechar);
        actionBar.add(btnPagar);
        actionBar.add(btnVer);

        dlg.setLayout(new BorderLayout());
        dlg.add(UIUtils.scrollPane(table), BorderLayout.CENTER);
        dlg.add(actionBar, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void openFaturaDetalhe(FaturaCartao fatura) {
        FaturaDetalheDialog dlg = new FaturaDetalheDialog(
                SwingUtilities.getWindowAncestor(this), fatura, faturaSvc, confSvc);
        dlg.setVisible(true);
    }

    private void openForm(CartaoCredito existing) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
                existing == null ? "Novo Cartão" : "Editar Cartão",
                Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(420, 400);
        dlg.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(16, 16, 16, 16));

        JTextField fNome   = new JTextField(existing != null ? existing.getNome() : "");
        JComboBox<Bandeira> cmbBand = new JComboBox<>(Bandeira.values());
        JTextField fLimite = new JTextField(existing != null ? existing.getLimiteTotal().toPlainString() : "");
        JTextField fVenc   = new JTextField(existing != null ? String.valueOf(existing.getDiaVencimento()) : "");
        JComboBox<ContaBancaria> cmbConta = new JComboBox<>();
        movSvc.listarContas().forEach(cmbConta::addItem);

        if (existing != null) {
            cmbBand.setSelectedItem(existing.getBandeira());
            if (existing.getConta() != null) {
                for (int i = 0; i < cmbConta.getItemCount(); i++) {
                    if (cmbConta.getItemAt(i).getId().equals(existing.getConta().getId())) {
                        cmbConta.setSelectedIndex(i);
                        break;
                    }
                }
            }
        }

        int row = 0;
        form.add(new JLabel("Nome:"),          UIUtils.gbc(0, row, 1));
        form.add(fNome,                        UIUtils.gbc(1, row++, 1));
        form.add(new JLabel("Bandeira:"),      UIUtils.gbc(0, row, 1));
        form.add(cmbBand,                      UIUtils.gbc(1, row++, 1));
        form.add(new JLabel("Limite (R$):"),   UIUtils.gbc(0, row, 1));
        form.add(fLimite,                      UIUtils.gbc(1, row++, 1));
        form.add(new JLabel("Dia Vencimento:"),UIUtils.gbc(0, row, 1));
        form.add(fVenc,                        UIUtils.gbc(1, row++, 1));
        form.add(new JLabel("Conta Vinculada:"),UIUtils.gbc(0, row, 1));
        form.add(cmbConta,                     UIUtils.gbc(1, row++, 1));

        JButton btnSalvar = UIUtils.primaryBtn("Salvar");
        btnSalvar.addActionListener(e -> {
            try {
                CartaoCredito c = existing != null ? existing : new CartaoCredito();
                c.setNome(fNome.getText().trim());
                c.setBandeira((Bandeira) cmbBand.getSelectedItem());
                c.setLimiteTotal(new BigDecimal(fLimite.getText().trim().replace(",", ".")));
                c.setDiaVencimento(Integer.parseInt(fVenc.getText().trim()));
                c.setDiaFechamento(0);
                c.setConta((ContaBancaria) cmbConta.getSelectedItem());
                cartaoSvc.salvar(c);
                dlg.dispose();
                refresh();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dlg, "Verifique os campos numéricos.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        GridBagConstraints gc = UIUtils.gbc(0, row, 2);
        gc.anchor = GridBagConstraints.EAST;
        form.add(btnSalvar, gc);

        dlg.add(form);
        dlg.setVisible(true);
    }
}
