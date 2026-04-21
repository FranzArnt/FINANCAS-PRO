package com.financas.ui.panels;

import com.financas.model.Divida;
import com.financas.service.FinancasService;
import com.financas.ui.util.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class DividasPanel extends JPanel {

    private final FinancasService financasSvc;
    private DefaultTableModel tableModel;
    private List<Divida> dados;

    public DividasPanel(FinancasService financasSvc) {
        this.financasSvc = financasSvc;
        setBackground(UIUtils.BG);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel header = UIUtils.headerPanel("Dívidas");
        JButton btnNova = UIUtils.primaryBtn("+ Nova Dívida");
        btnNova.addActionListener(e -> openForm(null));
        header.add(btnNova, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        String[] cols = {"Nome", "Tipo", "Total", "Restante", "% Pago", "Vencimento", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(tableModel);
        UIUtils.styleTable(table);

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(UIUtils.BG);
        body.setBorder(new EmptyBorder(0, 24, 16, 24));
        body.add(UIUtils.scrollPane(table), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setBackground(UIUtils.BG);

        JButton btnEditar = UIUtils.secondaryBtn("Editar");
        btnEditar.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0 || dados == null || row >= dados.size()) {
                JOptionPane.showMessageDialog(this, "Selecione uma dívida."); return;
            }
            openForm(dados.get(row));
        });

        JButton btnExcluir = UIUtils.dangerBtn("Excluir");
        btnExcluir.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0 || dados == null || row >= dados.size()) {
                JOptionPane.showMessageDialog(this, "Selecione uma dívida."); return;
            }
            int conf = JOptionPane.showConfirmDialog(this, "Excluir esta dívida?",
                    "Confirmar", JOptionPane.YES_NO_OPTION);
            if (conf == JOptionPane.YES_OPTION) {
                financasSvc.excluirDivida(dados.get(row).getId());
                refresh();
            }
        });

        actions.add(btnEditar);
        actions.add(btnExcluir);
        body.add(actions, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);
    }

    public void refresh() {
        new SwingWorker<List<Divida>, Void>() {
            @Override protected List<Divida> doInBackground() {
                return financasSvc.listarDividas();
            }
            @Override protected void done() {
                try {
                    dados = get();
                    tableModel.setRowCount(0);
                    for (Divida d : dados) {
                        int pct = d.getValorTotal() != null && d.getValorTotal().compareTo(BigDecimal.ZERO) > 0
                                ? d.getValorTotal().subtract(
                                d.getValorRestante() != null ? d.getValorRestante() : BigDecimal.ZERO)
                                .multiply(BigDecimal.valueOf(100))
                                .divide(d.getValorTotal(), 0, java.math.RoundingMode.HALF_UP).intValue()
                                : 0;
                        tableModel.addRow(new Object[]{
                                d.getNome(),
                                d.getTipo(),
                                UIUtils.formatCurrency(d.getValorTotal()),
                                UIUtils.formatCurrency(d.getValorRestante()),
                                pct + "%",
                                UIUtils.formatDate(d.getVencimento()),
                                d.getStatus()
                        });
                    }
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        }.execute();
    }

    private void openForm(Divida existing) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
                existing == null ? "Nova Dívida" : "Editar Dívida",
                Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(420, 400);
        dlg.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(16, 16, 16, 16));

        String[] tipos  = {"FINANCIAMENTO", "EMPRESTIMO", "CONTA", "PESSOAL", "OUTRO"};
        String[] status = {"PENDENTE", "ATIVO", "VENCIDA", "PAGO"};

        JTextField fNome     = new JTextField(existing != null ? existing.getNome() : "");
        JTextField fTotal    = new JTextField(existing != null && existing.getValorTotal() != null
                ? existing.getValorTotal().toPlainString() : "");
        JTextField fRestante = new JTextField(existing != null && existing.getValorRestante() != null
                ? existing.getValorRestante().toPlainString() : "");
        JTextField fVenc     = new JTextField(existing != null
                ? UIUtils.formatDate(existing.getVencimento())
                : UIUtils.formatDate(LocalDate.now()));
        JComboBox<String> cmbTipo   = new JComboBox<>(tipos);
        JComboBox<String> cmbStatus = new JComboBox<>(status);

        if (existing != null) {
            cmbTipo.setSelectedItem(existing.getTipo());
            cmbStatus.setSelectedItem(existing.getStatus());
        }

        int r = 0;
        form.add(new JLabel("Nome:"),               UIUtils.gbc(0, r, 1));
        form.add(fNome,                             UIUtils.gbc(1, r++, 1));
        form.add(new JLabel("Tipo:"),               UIUtils.gbc(0, r, 1));
        form.add(cmbTipo,                           UIUtils.gbc(1, r++, 1));
        form.add(new JLabel("Valor Total (R$):"),   UIUtils.gbc(0, r, 1));
        form.add(fTotal,                            UIUtils.gbc(1, r++, 1));
        form.add(new JLabel("Valor Restante (R$):"),UIUtils.gbc(0, r, 1));
        form.add(fRestante,                         UIUtils.gbc(1, r++, 1));
        form.add(new JLabel("Vencimento (dd/MM/yyyy):"), UIUtils.gbc(0, r, 1));
        form.add(fVenc,                             UIUtils.gbc(1, r++, 1));
        form.add(new JLabel("Status:"),             UIUtils.gbc(0, r, 1));
        form.add(cmbStatus,                         UIUtils.gbc(1, r++, 1));

        JButton btnSalvar = UIUtils.primaryBtn("Salvar");
        btnSalvar.addActionListener(e -> {
            try {
                Divida d = existing != null ? existing : new Divida();
                d.setNome(fNome.getText().trim());
                d.setTipo((String) cmbTipo.getSelectedItem());
                d.setValorTotal(new BigDecimal(fTotal.getText().trim().replace(",", ".")));
                d.setValorRestante(new BigDecimal(fRestante.getText().trim().replace(",", ".")));
                d.setVencimento(LocalDate.parse(fVenc.getText().trim(),
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                d.setStatus((String) cmbStatus.getSelectedItem());
                financasSvc.salvarDivida(d);
                dlg.dispose();
                refresh();
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
