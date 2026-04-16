package com.financas.ui.panels;

import com.financas.model.*;
import com.financas.service.FinancasService;
import com.financas.service.MovimentacaoService;
import com.financas.ui.util.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class InvestimentosPanel extends JPanel {

    private final FinancasService    financasSvc;
    private final MovimentacaoService movSvc;

    private DefaultTableModel tableModel;
    private List<Investimento> dados;

    public InvestimentosPanel(FinancasService financasSvc, MovimentacaoService movSvc) {
        this.financasSvc = financasSvc;
        this.movSvc      = movSvc;
        setBackground(UIUtils.BG);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel header = UIUtils.headerPanel("Investimentos");
        JButton btnNovo = UIUtils.primaryBtn("+ Novo Investimento");
        btnNovo.addActionListener(e -> openForm(null));
        header.add(btnNovo, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        String[] cols = {"Nome", "Tipo", "Conta", "Investido", "Atual", "Lucro", "Data", "Status"};
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
            if (row < 0) { JOptionPane.showMessageDialog(this, "Selecione um investimento."); return; }
            openForm(dados.get(row));
        });

        JButton btnResgatar = UIUtils.primaryBtn("Resgatar");
        btnResgatar.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "Selecione um investimento."); return; }
            Investimento inv = dados.get(row);
            int conf = JOptionPane.showConfirmDialog(this,
                    "Resgatar \"" + inv.getNome() + "\" (R$ " + inv.getValorAtual() + ")?",
                    "Confirmar", JOptionPane.YES_NO_OPTION);
            if (conf == JOptionPane.YES_OPTION) {
                try {
                    financasSvc.resgatarInvestimento(inv.getId());
                    JOptionPane.showMessageDialog(this, "Resgate realizado com sucesso!");
                    refresh();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JButton btnExcluir = UIUtils.dangerBtn("Excluir");
        btnExcluir.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "Selecione um investimento."); return; }
            int conf = JOptionPane.showConfirmDialog(this, "Excluir este investimento?",
                    "Confirmar", JOptionPane.YES_NO_OPTION);
            if (conf == JOptionPane.YES_OPTION) {
                financasSvc.excluirInvestimento(dados.get(row).getId());
                refresh();
            }
        });

        actions.add(btnEditar);
        actions.add(btnResgatar);
        actions.add(btnExcluir);
        body.add(actions, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);
    }

    public void refresh() {
        new SwingWorker<List<Investimento>, Void>() {
            @Override protected List<Investimento> doInBackground() {
                return financasSvc.listarInvestimentos();
            }
            @Override protected void done() {
                try {
                    dados = get();
                    tableModel.setRowCount(0);
                    for (Investimento inv : dados) {
                        BigDecimal lucro = inv.getValorAtual().subtract(inv.getValorInvestido());
                        tableModel.addRow(new Object[]{
                                inv.getNome(),
                                inv.getTipo(),
                                inv.getConta() != null ? inv.getConta().getNome() : "",
                                UIUtils.formatCurrency(inv.getValorInvestido()),
                                UIUtils.formatCurrency(inv.getValorAtual()),
                                (lucro.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "")
                                        + UIUtils.formatCurrency(lucro),
                                UIUtils.formatDate(inv.getDataAplicacao()),
                                inv.getStatus()
                        });
                    }
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        }.execute();
    }

    private void openForm(Investimento existing) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
                existing == null ? "Novo Investimento" : "Editar Investimento",
                Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(420, 420);
        dlg.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(16, 16, 16, 16));

        String[] tipos = {"RENDA_FIXA", "ACOES", "FII", "CRIPTO", "OUTRO"};
        String[] status = {"ATIVO", "RESGATADO", "VENCIDO"};

        JTextField fNome    = new JTextField(existing != null ? existing.getNome() : "");
        JTextField fInvest  = new JTextField(existing != null ? existing.getValorInvestido().toPlainString() : "");
        JTextField fAtual   = new JTextField(existing != null ? existing.getValorAtual().toPlainString() : "");
        JTextField fData    = new JTextField(existing != null
                ? UIUtils.formatDate(existing.getDataAplicacao())
                : UIUtils.formatDate(LocalDate.now()));
        JComboBox<String> cmbTipo   = new JComboBox<>(tipos);
        JComboBox<String> cmbStatus = new JComboBox<>(status);
        JComboBox<ContaBancaria> cmbConta = new JComboBox<>();
        movSvc.listarContas().forEach(cmbConta::addItem);

        if (existing != null) {
            cmbTipo.setSelectedItem(existing.getTipo());
            cmbStatus.setSelectedItem(existing.getStatus());
            if (existing.getConta() != null) {
                for (int i = 0; i < cmbConta.getItemCount(); i++) {
                    if (cmbConta.getItemAt(i).getId().equals(existing.getConta().getId())) {
                        cmbConta.setSelectedIndex(i);
                        break;
                    }
                }
            }
        }

        int r = 0;
        form.add(new JLabel("Nome:"),              UIUtils.gbc(0, r, 1));
        form.add(fNome,                            UIUtils.gbc(1, r++, 1));
        form.add(new JLabel("Tipo:"),              UIUtils.gbc(0, r, 1));
        form.add(cmbTipo,                          UIUtils.gbc(1, r++, 1));
        form.add(new JLabel("Valor Investido:"),   UIUtils.gbc(0, r, 1));
        form.add(fInvest,                          UIUtils.gbc(1, r++, 1));
        form.add(new JLabel("Valor Atual:"),       UIUtils.gbc(0, r, 1));
        form.add(fAtual,                           UIUtils.gbc(1, r++, 1));
        form.add(new JLabel("Data (dd/MM/yyyy):"), UIUtils.gbc(0, r, 1));
        form.add(fData,                            UIUtils.gbc(1, r++, 1));
        form.add(new JLabel("Conta:"),             UIUtils.gbc(0, r, 1));
        form.add(cmbConta,                         UIUtils.gbc(1, r++, 1));
        form.add(new JLabel("Status:"),            UIUtils.gbc(0, r, 1));
        form.add(cmbStatus,                        UIUtils.gbc(1, r++, 1));

        JButton btnSalvar = UIUtils.primaryBtn("Salvar");
        btnSalvar.addActionListener(e -> {
            try {
                Investimento inv = existing != null ? existing : new Investimento();
                inv.setNome(fNome.getText().trim());
                inv.setTipo((String) cmbTipo.getSelectedItem());
                inv.setValorInvestido(new BigDecimal(fInvest.getText().trim().replace(",", ".")));
                inv.setValorAtual(new BigDecimal(fAtual.getText().trim().replace(",", ".")));
                inv.setDataAplicacao(LocalDate.parse(fData.getText().trim(),
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                inv.setConta((ContaBancaria) cmbConta.getSelectedItem());
                inv.setStatus((String) cmbStatus.getSelectedItem());
                financasSvc.salvarInvestimento(inv);
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
