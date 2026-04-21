package com.financas.ui;

import com.financas.ui.util.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SidebarPanel extends JPanel {

    private static final int EXPANDED_WIDTH  = 220;
    private static final int COLLAPSED_WIDTH = 52;

    private boolean expanded = true;
    private JButton activeBtn;
    private final List<JLabel>  sectionLabels = new ArrayList<>();
    private final List<NavItem> navItems      = new ArrayList<>();
    private final JLabel        logo;
    private final JButton       toggleBtn;

    public SidebarPanel(Consumer<String> navigator) {
        setBackground(UIUtils.SIDEBAR_BG);
        setPreferredSize(new Dimension(EXPANDED_WIDTH, 0));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Toggle button
        toggleBtn = new JButton("◀");
        toggleBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        toggleBtn.setAlignmentX(LEFT_ALIGNMENT);
        toggleBtn.setHorizontalAlignment(SwingConstants.RIGHT);
        toggleBtn.setBackground(UIUtils.SIDEBAR_BG);
        toggleBtn.setForeground(new Color(0x94A3B8));
        toggleBtn.setFont(toggleBtn.getFont().deriveFont(12f));
        toggleBtn.setBorderPainted(false);
        toggleBtn.setFocusPainted(false);
        toggleBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggleBtn.setBorder(new EmptyBorder(8, 8, 8, 12));
        toggleBtn.addActionListener(e -> toggle());
        add(toggleBtn);

        logo = new JLabel("FinancasPro");
        logo.setForeground(Color.WHITE);
        logo.setFont(logo.getFont().deriveFont(Font.BOLD, 20f));
        logo.setBorder(new EmptyBorder(8, 20, 20, 20));
        logo.setAlignmentX(LEFT_ALIGNMENT);
        add(logo);

        addSep();
        addNav("Dashboard",     "dashboard",     "⊞", navigator);
        addNav("Cartões",       "cartoes",        "▣", navigator);
        addNav("Transações",    "transacoes",     "↕", navigator);

        addSection("OUTROS");
        addNav("Investimentos", "investimentos",  "◈", navigator);
        addNav("Configurações", "configuracoes",  "⚙", navigator);

        add(Box.createVerticalGlue());
    }

    private void toggle() {
        expanded = !expanded;
        int w = expanded ? EXPANDED_WIDTH : COLLAPSED_WIDTH;
        setPreferredSize(new Dimension(w, 0));
        setMaximumSize(new Dimension(w, Integer.MAX_VALUE));
        setMinimumSize(new Dimension(w, 0));

        logo.setVisible(expanded);
        sectionLabels.forEach(l -> l.setVisible(expanded));
        for (NavItem item : navItems) {
            item.btn.setText(expanded ? item.label : item.icon);
            item.btn.setHorizontalAlignment(expanded ? SwingConstants.LEFT : SwingConstants.CENTER);
            item.btn.setBorder(expanded
                    ? new EmptyBorder(10, 20, 10, 20)
                    : new EmptyBorder(10, 0, 10, 0));
        }
        toggleBtn.setText(expanded ? "◀" : "▶");
        toggleBtn.setHorizontalAlignment(expanded ? SwingConstants.RIGHT : SwingConstants.CENTER);

        revalidate();
        repaint();
        // força o pai a rearranjar
        if (getParent() != null) {
            getParent().revalidate();
            getParent().repaint();
        }
    }

    private void addNav(String label, String key, String icon, Consumer<String> nav) {
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
        navItems.add(new NavItem(btn, label, icon));
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
        sectionLabels.add(l);
        add(l);
    }

    private void addSep() {
        JSeparator s = new JSeparator();
        s.setForeground(new Color(0x1E293B));
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        add(s);
    }

    private record NavItem(JButton btn, String label, String icon) {}
}
