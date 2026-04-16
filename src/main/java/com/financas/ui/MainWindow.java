package com.financas.ui;

import com.financas.service.*;
import com.financas.ui.panels.*;

import javax.swing.*;
import java.awt.*;

public class MainWindow extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel content = new JPanel(cardLayout);

    private final DashboardPanel     dashboardPanel;
    private final CartoesPanel       cartoesPanel;
    private final TransacoesPanel    transacoesPanel;
    private final InvestimentosPanel investimentosPanel;
    private final ConfiguracoesPanel configuracoesPanel;
    private final DividasPanel       dividasPanel;

    public MainWindow(DashboardService dashSvc,
                      MovimentacaoService movSvc,
                      CartaoService cartaoSvc,
                      FaturaService faturaSvc,
                      ConfiguracoesService confSvc,
                      FinancasService financasSvc) {
        super("FinancasPro");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        dashboardPanel     = new DashboardPanel(dashSvc, movSvc);
        cartoesPanel       = new CartoesPanel(cartaoSvc, faturaSvc, confSvc, movSvc);
        transacoesPanel    = new TransacoesPanel(movSvc, confSvc);
        investimentosPanel = new InvestimentosPanel(financasSvc, movSvc);
        configuracoesPanel = new ConfiguracoesPanel(confSvc, movSvc);
        dividasPanel       = new DividasPanel(financasSvc);

        content.add(dashboardPanel,     "dashboard");
        content.add(cartoesPanel,       "cartoes");
        content.add(transacoesPanel,    "transacoes");
        content.add(investimentosPanel, "investimentos");
        content.add(configuracoesPanel, "configuracoes");
        content.add(dividasPanel,       "dividas");

        setLayout(new BorderLayout());
        add(new SidebarPanel(this::navigate), BorderLayout.WEST);
        add(content, BorderLayout.CENTER);

        navigate("dashboard");
    }

    public void navigate(String panel) {
        cardLayout.show(content, panel);
        switch (panel) {
            case "dashboard"     -> dashboardPanel.refresh();
            case "cartoes"       -> cartoesPanel.refresh();
            case "transacoes"    -> transacoesPanel.refresh();
            case "investimentos" -> investimentosPanel.refresh();
            case "configuracoes" -> configuracoesPanel.refresh();
            case "dividas"       -> dividasPanel.refresh();
        }
    }
}
