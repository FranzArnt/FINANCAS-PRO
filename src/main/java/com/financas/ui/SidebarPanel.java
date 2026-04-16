package com.financas.ui;

import com.financas.ui.util.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.Consumer;

public class SidebarPanel extends JPanel {

    private JButton activeBtn;

    public SidebarPanel(Consumer<String> navigator) {
        setBackground(UIUtils.SIDEBAR_BG);
        setPreferredSize(new Dimension(220, 0));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JLabel logo = new JLabel("FinancasPro");
        logo.setForeground(Color.WHITE);
        logo.setFont(logo.getFont().deriveFont(Font.BOLD, 20f));
        logo.setBorder(new EmptyBorder(28, 20, 28, 20));
        logo.setAlignmentX(LEFT_ALIGNMENT);
        add(logo);

        addSep();
        addNav("Dashboard",    "dashboard",     navigator);
        addNav("Cartões",      "cartoes",        navigator);
        addNav("Transações",   "transacoes",     navigator);

        addSection("OUTROS");
        addNav("Investimentos", "investimentos", navigator);
        addNav("Dívidas",       "dividas",       navigator);
        addNav("Configurações", "configuracoes", navigator);

        add(Box.createVerticalGlue());
    }

    private void addNav(String label, String key, Consumer<String> nav) {
        JButton btn = new JButton(label);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btn.setAlignmentX(LEFT_ALIGNMENT);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBackground(UIUtils.SIDEBAR_BG);
        btn.setForeground(new Color(0x94A3B8));
        btn.setFont(btn.getFont().deriveFont(14f));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(10, 20, 10, 20));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (btn != activeBtn) btn.setForeground(Color.WHITE);
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (btn != activeBtn) btn.setForeground(new Color(0x94A3B8));
            }
        });

        btn.addActionListener(e -> {
            activate(btn);
            nav.accept(key);
        });

        if (activeBtn == null) activate(btn);
        add(btn);
    }

    private void activate(JButton btn) {
        if (activeBtn != null) {
            activeBtn.setBackground(UIUtils.SIDEBAR_BG);
            activeBtn.setForeground(new Color(0x94A3B8));
        }
        activeBtn = btn;
        btn.setBackground(UIUtils.SIDEBAR_ITEM);
        btn.setForeground(Color.WHITE);
    }

    private void addSection(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(0x475569));
        l.setFont(l.getFont().deriveFont(Font.BOLD, 11f));
        l.setBorder(new EmptyBorder(16, 20, 6, 20));
        l.setAlignmentX(LEFT_ALIGNMENT);
        add(l);
    }

    private void addSep() {
        JSeparator s = new JSeparator();
        s.setForeground(new Color(0x1E293B));
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        add(s);
    }
}
