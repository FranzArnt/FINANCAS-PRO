package com.financas.ui.util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class UIUtils {

    public static final Color SIDEBAR_BG   = new Color(0x0F172A);
    public static final Color SIDEBAR_ITEM = new Color(0x1E293B);
    public static final Color ACCENT       = new Color(0x6366F1);
    public static final Color BG           = new Color(0xF1F5F9);
    public static final Color SUCCESS      = new Color(0x10B981);
    public static final Color DANGER       = new Color(0xEF4444);
    public static final Color WARNING      = new Color(0xF59E0B);
    public static final Color INFO         = new Color(0x3B82F6);
    public static final Color TEXT_PRIMARY   = new Color(0x1E293B);
    public static final Color TEXT_SECONDARY = new Color(0x64748B);
    public static final Color BORDER       = new Color(0xE2E8F0);
    public static final Color WHITE        = Color.WHITE;

    private static final NumberFormat CURRENCY =
            NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private UIUtils() {}

    public static String formatCurrency(BigDecimal v) {
        return v == null ? "R$ 0,00" : CURRENCY.format(v);
    }

    public static String formatDate(LocalDate d) {
        return d == null ? "" : d.format(DATE_FMT);
    }

    public static JPanel card(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setBackground(WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(16, 16, 16, 16)));
        return p;
    }

    public static JButton primaryBtn(String text) {
        JButton b = new JButton(text);
        b.setBackground(ACCENT);
        b.setForeground(WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setFont(b.getFont().deriveFont(Font.BOLD));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JButton dangerBtn(String text) {
        JButton b = new JButton(text);
        b.setBackground(DANGER);
        b.setForeground(WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JButton secondaryBtn(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JLabel badge(String text, Color bg) {
        JLabel l = new JLabel(" " + text + " ");
        l.setBackground(bg);
        l.setForeground(WHITE);
        l.setOpaque(true);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 11f));
        l.setBorder(new EmptyBorder(2, 6, 2, 6));
        return l;
    }

    public static Color statusColor(String status) {
        if (status == null) return TEXT_SECONDARY;
        return switch (status.toUpperCase()) {
            case "ABERTA"              -> INFO;
            case "FECHADA"             -> WARNING;
            case "PAGA"                -> SUCCESS;
            case "VENCIDA"             -> DANGER;
            case "ATIVO", "ATIVA"      -> SUCCESS;
            case "PENDENTE"            -> WARNING;
            case "PAGO"                -> SUCCESS;
            case "RESGATADO"           -> TEXT_SECONDARY;
            default                    -> TEXT_SECONDARY;
        };
    }

    public static void styleTable(JTable t) {
        t.setRowHeight(36);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.getTableHeader().setBackground(new Color(0xF8FAFC));
        t.getTableHeader().setFont(t.getTableHeader().getFont().deriveFont(Font.BOLD, 12f));
        t.setSelectionBackground(new Color(0xEEF2FF));
        t.setSelectionForeground(TEXT_PRIMARY);
        t.setFillsViewportHeight(true);
        t.setFont(t.getFont().deriveFont(13f));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }

    public static JScrollPane scrollPane(Component c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        sp.getViewport().setBackground(WHITE);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    public static JPanel headerPanel(String title) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(24, 24, 12, 24));
        JLabel lbl = new JLabel(title);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 22f));
        lbl.setForeground(TEXT_PRIMARY);
        p.add(lbl, BorderLayout.WEST);
        return p;
    }

    public static GridBagConstraints gbc(int x, int y, int w) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x; c.gridy = y; c.gridwidth = w;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 4, 4, 4);
        c.weightx = 1;
        return c;
    }
}
