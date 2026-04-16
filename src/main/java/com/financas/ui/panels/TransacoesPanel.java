package com.financas.ui.panels;

import com.financas.model.*;
import com.financas.service.ConfiguracoesService;
import com.financas.service.MovimentacaoService;
import com.financas.ui.util.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class TransacoesPanel extends JPanel {

    private final MovimentacaoService   movSvc;
    private final ConfiguracoesService  confSvc;

    private DefaultTableModel tableModel;
    private List<Transacao>   dados;

    public TransacoesPanel(MovimentacaoService movSvc, ConfiguracoesService confSvc) {
        this.movSvc  = movSvc;
        this.confSvc = confSvc;
        setBackground(UIUtils.BG);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel header = UIUtils.headerPanel("Transações");
        JButton btnNova = UIUtils.primaryBtn("+ Nova Transação");
        btnNova.addActionListener(e -> openForm(null));
        header.add(btnNova, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        String[] cols = {"Descrição", "Tipo", "Direção", "Categoria", "Conta", "Data", "Valor"};
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
        JButton btnExcluir = UIUtils.dangerBtn("Excluir Selecionado");
        btnExcluir.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "Selecione uma transação."); return; }
            int conf = JOptionPane.showConfirmDialog(this, "Excluir esta transação?",
                    "Confirmar", JOptionPane.YES_NO_OPTION);
            if (conf == JOptionPane.YES_OPTION) {
                movSvc.excluirTransacao(dados.get(row).getId());
                refresh();
            }
        });
        actions.add(btnExcluir);
        body.add(actions, BorderLayout.SOUTH);

        add(body, BorderLayout.CENTER);
    }

    public void refresh() {
        new SwingWorker<List<Transacao>, Void>() {
            @Override protected List<Transacao> doInBackground() {
                return movSvc.listarTodasTransacoes();
            }
            @Override protected void done() {
                try {
                    dados = get();
                    tableModel.setRowCount(0);
                    for (Transacao t : dados) {
                        tableModel.addRow(new Object[]{
                                t.getDescricao(),
                                t.getTipo() != null ? t.getTipo().name() : "",
                                t.getDirecao() != null ? t.getDirecao().name() : "",
                                t.getCategoria() != null ? t.getCategoria().getNome() : "",
                                t.getConta() != null ? t.getConta().getNome() : "",
                                UIUtils.formatDate(t.getData()),
                                (t.getDirecao() == DirecaoTransacao.ENTRADA ? "+" : "-")
                                        + UIUtils.formatCurrency(t.getValor())
                        });
                    }
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        }.execute();
    }

    private void openForm(Transacao existing) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Nova Transação", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(440, 480);
        dlg.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(16, 16, 16, 16));

        JTextField fDesc   = new JTextField();
        JTextField fValor  = new JTextField();
        JTextField fData   = new JTextField(UIUtils.formatDate(LocalDate.now()));
        JTextField fOrig   = new JTextField();
        JTextField fDest   = new JTextField();
        JTextField fObs    = new JTextField();
        JComboBox<TipoTransacao>    cmbTipo  = new JComboBox<>(TipoTransacao.values());
        JComboBox<DirecaoTransacao> cmbDir   = new JComboBox<>(DirecaoTransacao.values());
        JComboBox<ContaBancaria>    cmbConta = new JComboBox<>();
        JComboBox<Categoria>        cmbCat   = new JComboBox<>();

        movSvc.listarContas().forEach(cmbConta::addItem);
        confSvc.listarTodasAtivas().forEach(cmbCat::addItem);

        int r = 0;
        form.add(new JLabel("Descrição:"),         UIUtils.gbc(0, r, 1));
        form.add(fDesc,                            UIUtils.gbc(1, r++, 1));
        form.add(new JLabel("Tipo:"),              UIUtils.gbc(0, r, 1));
        form.add(cmbTipo,                          UIUtils.gbc(1, r++, 1));
        form.add(new JLabel("Direção:"),           UIUtils.gbc(0, r, 1));
        form.add(cmbDir,                           UIUtils.gbc(1, r++, 1));
        form.add(new JLabel("Valor (R$):"),        UIUtils.gbc(0, r, 1));
        form.add(fValor,                           UIUtils.gbc(1, r++, 1));
        form.add(new JLabel("Data (dd/MM/yyyy):"), UIUtils.gbc(0, r, 1));
        form.add(fData,                            UIUtils.gbc(1, r++, 1));
        form.add(new JLabel("Origem:"),            UIUtils.gbc(0, r, 1));
        form.add(fOrig,                            UIUtils.gbc(1, r++, 1));
        form.add(new JLabel("Destino:"),           UIUtils.gbc(0, r, 1));
        form.add(fDest,                            UIUtils.gbc(1, r++, 1));
        form.add(new JLabel("Conta:"),             UIUtils.gbc(0, r, 1));
        form.add(cmbConta,                         UIUtils.gbc(1, r++, 1));
        form.add(new JLabel("Categoria:"),         UIUtils.gbc(0, r, 1));
        form.add(cmbCat,                           UIUtils.gbc(1, r++, 1));
        form.add(new JLabel("Observação:"),        UIUtils.gbc(0, r, 1));
        form.add(fObs,                             UIUtils.gbc(1, r++, 1));

        JButton btnSalvar = UIUtils.primaryBtn("Salvar");
        btnSalvar.addActionListener(e -> {
            try {
                Transacao t = new Transacao();
                t.setDescricao(fDesc.getText().trim());
                t.setTipo((TipoTransacao) cmbTipo.getSelectedItem());
                t.setDirecao((DirecaoTransacao) cmbDir.getSelectedItem());
                t.setValor(new BigDecimal(fValor.getText().trim().replace(",", ".")));
                t.setData(LocalDate.parse(fData.getText().trim(),
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                t.setOrigem(fOrig.getText().trim());
                t.setDestino(fDest.getText().trim());
                t.setConta((ContaBancaria) cmbConta.getSelectedItem());
                t.setCategoria((Categoria) cmbCat.getSelectedItem());
                t.setObservacao(fObs.getText().trim());
                movSvc.salvarTransacao(t);
                dlg.dispose();
                refresh();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        GridBagConstraints gc = UIUtils.gbc(0, r, 2);
        gc.anchor = GridBagConstraints.EAST;
        form.add(btnSalvar, gc);

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(null);
        dlg.add(scroll);
        dlg.setVisible(true);
    }
}
