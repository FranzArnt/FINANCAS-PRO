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
import java.util.List;

public class ConfiguracoesPanel extends JPanel {

    private final ConfiguracoesService confSvc;
    private final MovimentacaoService  movSvc;

    private DefaultTableModel contasModel;
    private DefaultTableModel catsModel;
    private List<ContaBancaria> contasDados;
    private List<Categoria>     catsDados;

    public ConfiguracoesPanel(ConfiguracoesService confSvc, MovimentacaoService movSvc) {
        this.confSvc = confSvc;
        this.movSvc  = movSvc;
        setBackground(UIUtils.BG);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        add(UIUtils.headerPanel("Configurações"), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBorder(new EmptyBorder(0, 24, 24, 24));
        tabs.addTab("Contas Bancárias", buildContasTab());
        tabs.addTab("Categorias",       buildCatsTab());
        add(tabs, BorderLayout.CENTER);
    }

    // ── Contas tab ───────────────────────────────────────────────────
    private JPanel buildContasTab() {
        JPanel p = new JPanel(new BorderLayout(16, 0));
        p.setBackground(UIUtils.BG);
        p.setBorder(new EmptyBorder(16, 0, 0, 0));

        String[] cols = {"Nome", "Agência", "Conta", "Saldo Inicial", "Ativa"};
        contasModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(contasModel);
        UIUtils.styleTable(table);

        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(UIUtils.BG);
        left.add(UIUtils.scrollPane(table), BorderLayout.CENTER);

        JPanel actBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actBar.setBackground(UIUtils.BG);
        JButton btnNova = UIUtils.primaryBtn("+ Nova Conta");
        btnNova.addActionListener(e -> openContaForm(null));
        JButton btnEditar = UIUtils.secondaryBtn("Editar");
        btnEditar.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "Selecione uma conta."); return; }
            openContaForm(contasDados.get(row));
        });
        JButton btnDesativar = UIUtils.dangerBtn("Desativar");
        btnDesativar.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "Selecione uma conta."); return; }
            ContaBancaria c = contasDados.get(row);
            c.setAtiva(false);
            movSvc.salvarConta(c);
            refresh();
        });
        actBar.add(btnNova); actBar.add(btnEditar); actBar.add(btnDesativar);
        left.add(actBar, BorderLayout.SOUTH);
        p.add(left, BorderLayout.CENTER);
        return p;
    }

    // ── Categorias tab ───────────────────────────────────────────────
    private JPanel buildCatsTab() {
        JPanel p = new JPanel(new BorderLayout(16, 0));
        p.setBackground(UIUtils.BG);
        p.setBorder(new EmptyBorder(16, 0, 0, 0));

        String[] cols = {"Nome", "Tipo", "Ativa", "Categoria Pai"};
        catsModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(catsModel);
        UIUtils.styleTable(table);

        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(UIUtils.BG);
        left.add(UIUtils.scrollPane(table), BorderLayout.CENTER);

        JPanel actBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actBar.setBackground(UIUtils.BG);

        JButton btnNova = UIUtils.primaryBtn("+ Nova Categoria");
        btnNova.addActionListener(e -> openCatForm(null));

        JButton btnEditar = UIUtils.secondaryBtn("Editar");
        btnEditar.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "Selecione uma categoria."); return; }
            openCatForm(catsDados.get(row));
        });

        JButton btnToggle = UIUtils.secondaryBtn("Ativar/Desativar");
        btnToggle.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "Selecione uma categoria."); return; }
            Categoria c = catsDados.get(row);
            if (c.isAtiva()) confSvc.desativarCategoria(c.getId());
            else confSvc.ativarCategoria(c.getId());
            refresh();
        });

        JButton btnExcluir = UIUtils.dangerBtn("Excluir");
        btnExcluir.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "Selecione uma categoria."); return; }
            int conf = JOptionPane.showConfirmDialog(this, "Excluir esta categoria?",
                    "Confirmar", JOptionPane.YES_NO_OPTION);
            if (conf == JOptionPane.YES_OPTION) {
                confSvc.excluirCategoria(catsDados.get(row).getId());
                refresh();
            }
        });

        actBar.add(btnNova); actBar.add(btnEditar); actBar.add(btnToggle); actBar.add(btnExcluir);
        left.add(actBar, BorderLayout.SOUTH);
        p.add(left, BorderLayout.CENTER);
        return p;
    }

    public void refresh() {
        new SwingWorker<Void, Void>() {
            private List<ContaBancaria> contas;
            private List<Categoria>     cats;
            @Override protected Void doInBackground() {
                contas = movSvc.listarContas();
                cats   = confSvc.listarTodasAtivas();
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    contasDados = contas;
                    contasModel.setRowCount(0);
                    for (ContaBancaria c : contas) {
                        contasModel.addRow(new Object[]{
                                c.getNome(), c.getAgencia(), c.getConta(),
                                UIUtils.formatCurrency(c.getSaldoInicial()), c.isAtiva() ? "Sim" : "Não"
                        });
                    }
                    catsDados = cats;
                    catsModel.setRowCount(0);
                    for (Categoria c : cats) {
                        catsModel.addRow(new Object[]{
                                c.getNome(),
                                c.getTipo() != null ? c.getTipo().name() : "",
                                c.isAtiva() ? "Sim" : "Não",
                                c.getPai() != null ? c.getPai().getNome() : ""
                        });
                    }
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        }.execute();
    }

    private void openContaForm(ContaBancaria existing) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
                existing == null ? "Nova Conta" : "Editar Conta",
                Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(380, 300);
        dlg.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(16, 16, 16, 16));

        JTextField fNome   = new JTextField(existing != null ? existing.getNome() : "");
        JTextField fAgenc  = new JTextField(existing != null ? existing.getAgencia() : "");
        JTextField fConta  = new JTextField(existing != null ? existing.getConta() : "");
        JTextField fSaldo  = new JTextField(existing != null
                ? existing.getSaldoInicial().toPlainString() : "0.00");

        int r = 0;
        form.add(new JLabel("Nome:"),           UIUtils.gbc(0, r, 1));
        form.add(fNome,                         UIUtils.gbc(1, r++, 1));
        form.add(new JLabel("Agência:"),        UIUtils.gbc(0, r, 1));
        form.add(fAgenc,                        UIUtils.gbc(1, r++, 1));
        form.add(new JLabel("Conta:"),          UIUtils.gbc(0, r, 1));
        form.add(fConta,                        UIUtils.gbc(1, r++, 1));
        form.add(new JLabel("Saldo Inicial:"),  UIUtils.gbc(0, r, 1));
        form.add(fSaldo,                        UIUtils.gbc(1, r++, 1));

        JButton btnSalvar = UIUtils.primaryBtn("Salvar");
        btnSalvar.addActionListener(e -> {
            try {
                ContaBancaria c = existing != null ? existing : new ContaBancaria();
                c.setNome(fNome.getText().trim());
                c.setAgencia(fAgenc.getText().trim());
                c.setConta(fConta.getText().trim());
                c.setSaldoInicial(new BigDecimal(fSaldo.getText().trim().replace(",", ".")));
                c.setAtiva(true);
                movSvc.salvarConta(c);
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

    private void openCatForm(Categoria existing) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
                existing == null ? "Nova Categoria" : "Editar Categoria",
                Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(380, 300);
        dlg.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(16, 16, 16, 16));

        JTextField fNome = new JTextField(existing != null ? existing.getNome() : "");
        JComboBox<TipoCategoria> cmbTipo = new JComboBox<>(TipoCategoria.values());
        JComboBox<Categoria> cmbPai = new JComboBox<>();
        cmbPai.addItem(null);
        confSvc.listarTodasAtivas().stream()
                .filter(c -> c.getPai() == null)
                .forEach(cmbPai::addItem);

        if (existing != null) {
            cmbTipo.setSelectedItem(existing.getTipo());
            if (existing.getPai() != null) {
                for (int i = 0; i < cmbPai.getItemCount(); i++) {
                    Categoria item = cmbPai.getItemAt(i);
                    if (item != null && item.getId().equals(existing.getPai().getId())) {
                        cmbPai.setSelectedIndex(i);
                        break;
                    }
                }
            }
        }

        int r = 0;
        form.add(new JLabel("Nome:"),       UIUtils.gbc(0, r, 1));
        form.add(fNome,                     UIUtils.gbc(1, r++, 1));
        form.add(new JLabel("Tipo:"),       UIUtils.gbc(0, r, 1));
        form.add(cmbTipo,                   UIUtils.gbc(1, r++, 1));
        form.add(new JLabel("Categoria Pai (opcional):"), UIUtils.gbc(0, r, 1));
        form.add(cmbPai,                    UIUtils.gbc(1, r++, 1));

        JButton btnSalvar = UIUtils.primaryBtn("Salvar");
        btnSalvar.addActionListener(e -> {
            try {
                Categoria c = existing != null ? existing : new Categoria();
                c.setNome(fNome.getText().trim());
                c.setTipo((TipoCategoria) cmbTipo.getSelectedItem());
                c.setPai((Categoria) cmbPai.getSelectedItem());
                c.setAtiva(true);
                confSvc.salvarCategoria(c);
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
