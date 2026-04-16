package com.financas.ui.panels;

import com.financas.model.*;
import com.financas.service.ConfiguracoesService;
import com.financas.service.FaturaService;
import com.financas.ui.util.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class FaturaDetalheDialog extends JDialog {

    private final FaturaCartao      fatura;
    private final FaturaService     faturaSvc;
    private final ConfiguracoesService confSvc;
    private DefaultTableModel tableModel;
    private JLabel lblTotal, lblStatus;

    public FaturaDetalheDialog(Window owner, FaturaCartao fatura,
                               FaturaService faturaSvc, ConfiguracoesService confSvc) {
        super(owner, "Fatura — " + fatura.getCartao().getNome()
                + " " + fatura.getMesReferencia(), ModalityType.APPLICATION_MODAL);
        this.fatura   = fatura;
        this.faturaSvc = faturaSvc;
        this.confSvc  = confSvc;
        setSize(720, 560);
        setLocationRelativeTo(owner);
        buildUI();
        loadData();
    }

    private void buildUI() {
        setLayout(new BorderLayout());

        // ── Info bar ────────────────────────────────────────────────
        JPanel info = new JPanel(new GridLayout(1, 4, 8, 0));
        info.setBorder(new EmptyBorder(12, 16, 12, 16));
        info.setBackground(UIUtils.BG);

        lblStatus = new JLabel("...");
        lblStatus.setFont(lblStatus.getFont().deriveFont(Font.BOLD, 13f));
        lblTotal  = new JLabel("R$ 0,00");
        lblTotal.setFont(lblTotal.getFont().deriveFont(Font.BOLD, 16f));
        lblTotal.setForeground(UIUtils.DANGER);

        info.add(infoCard("Fechamento", UIUtils.formatDate(fatura.getDataFechamento())));
        info.add(infoCard("Vencimento", UIUtils.formatDate(fatura.getDataVencimento())));
        info.add(infoCard("Total", lblTotal));
        info.add(infoCard("Status", lblStatus));
        add(info, BorderLayout.NORTH);

        // ── Table ───────────────────────────────────────────────────
        String[] cols = {"Descrição", "Categoria", "Data Compra", "Valor", "Obs"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(tableModel);
        UIUtils.styleTable(table);
        add(UIUtils.scrollPane(table), BorderLayout.CENTER);

        // ── Actions ─────────────────────────────────────────────────
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));

        JButton btnAdd = UIUtils.primaryBtn("+ Lançamento");
        btnAdd.addActionListener(e -> openAddLancamento());

        JButton btnRemover = UIUtils.dangerBtn("Remover Selecionado");
        btnRemover.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "Selecione um lançamento."); return; }
            List<LancamentoFatura> lancs = faturaSvc.listarLancamentos(fatura);
            try {
                faturaSvc.removerLancamento(lancs.get(row).getId());
                loadData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnPagar = UIUtils.primaryBtn("Pagar Fatura");
        btnPagar.addActionListener(e -> {
            try {
                faturaSvc.pagarFatura(fatura.getId());
                JOptionPane.showMessageDialog(this, "Fatura paga com sucesso!");
                loadData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        actions.add(btnAdd);
        actions.add(btnRemover);
        actions.add(btnPagar);
        add(actions, BorderLayout.SOUTH);
    }

    private JPanel infoCard(String title, String value) {
        JLabel v = new JLabel(value);
        return infoCard(title, v);
    }

    private JPanel infoCard(String title, JLabel valueLabel) {
        JPanel p = UIUtils.card(new BorderLayout(0, 4));
        JLabel t = new JLabel(title);
        t.setForeground(UIUtils.TEXT_SECONDARY);
        t.setFont(t.getFont().deriveFont(11f));
        p.add(t, BorderLayout.NORTH);
        p.add(valueLabel, BorderLayout.CENTER);
        return p;
    }

    private void loadData() {
        tableModel.setRowCount(0);
        List<LancamentoFatura> lancs = faturaSvc.listarLancamentos(fatura);
        BigDecimal total = BigDecimal.ZERO;
        for (LancamentoFatura l : lancs) {
            tableModel.addRow(new Object[]{
                    l.getDescricao(),
                    l.getCategoria() != null ? l.getCategoria().getNome() : "",
                    UIUtils.formatDate(l.getDataCompra()),
                    UIUtils.formatCurrency(l.getValor()),
                    l.getObservacao() != null ? l.getObservacao() : ""
            });
            total = total.add(l.getValor());
        }
        lblTotal.setText(UIUtils.formatCurrency(total));

        StatusFatura status = faturaSvc.resolverStatus(fatura);
        lblStatus.setText(status.name());
        lblStatus.setForeground(UIUtils.statusColor(status.name()));
    }

    private void openAddLancamento() {
        if (faturaSvc.resolverStatus(fatura) != StatusFatura.ABERTA) {
            JOptionPane.showMessageDialog(this, "Só é possível lançar em faturas ABERTAS.");
            return;
        }

        JDialog dlg = new JDialog(this, "Novo Lançamento", ModalityType.APPLICATION_MODAL);
        dlg.setSize(400, 340);
        dlg.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(16, 16, 16, 16));

        JTextField fDesc  = new JTextField();
        JTextField fValor = new JTextField();
        JTextField fData  = new JTextField(UIUtils.formatDate(LocalDate.now()));
        JTextField fObs   = new JTextField();
        JComboBox<Categoria> cmbCat = new JComboBox<>();
        confSvc.listarTodasAtivas().stream()
                .filter(c -> c.getTipo() == TipoCategoria.DESPESA)
                .forEach(cmbCat::addItem);

        int r = 0;
        form.add(new JLabel("Descrição:"),    UIUtils.gbc(0, r, 1));
        form.add(fDesc,                       UIUtils.gbc(1, r++, 1));
        form.add(new JLabel("Valor (R$):"),   UIUtils.gbc(0, r, 1));
        form.add(fValor,                      UIUtils.gbc(1, r++, 1));
        form.add(new JLabel("Data (dd/MM/yyyy):"), UIUtils.gbc(0, r, 1));
        form.add(fData,                       UIUtils.gbc(1, r++, 1));
        form.add(new JLabel("Categoria:"),    UIUtils.gbc(0, r, 1));
        form.add(cmbCat,                      UIUtils.gbc(1, r++, 1));
        form.add(new JLabel("Observação:"),   UIUtils.gbc(0, r, 1));
        form.add(fObs,                        UIUtils.gbc(1, r++, 1));

        JButton btnSalvar = UIUtils.primaryBtn("Salvar");
        btnSalvar.addActionListener(e -> {
            try {
                LancamentoFatura l = new LancamentoFatura();
                l.setDescricao(fDesc.getText().trim());
                l.setValor(new BigDecimal(fValor.getText().trim().replace(",", ".")));
                l.setDataCompra(LocalDate.parse(fData.getText().trim(),
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                l.setCategoria((Categoria) cmbCat.getSelectedItem());
                l.setObservacao(fObs.getText().trim());
                faturaSvc.adicionarLancamento(fatura.getId(), l);
                dlg.dispose();
                loadData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        GridBagConstraints gc = UIUtils.gbc(0, r, 2);
        gc.anchor = GridBagConstraints.EAST;
        form.add(btnSalvar, gc);

        dlg.add(form);
        dlg.setVisible(true);
    }
}
