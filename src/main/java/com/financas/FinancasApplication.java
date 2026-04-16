package com.financas;

import com.financas.service.*;
import com.financas.ui.MainWindow;
import com.formdev.flatlaf.FlatIntelliJLaf;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;

@SpringBootApplication
public class FinancasApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(FinancasApplication.class, args);

        SwingUtilities.invokeLater(() -> {
            FlatIntelliJLaf.setup();
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("ScrollBar.width", 8);

            MainWindow window = new MainWindow(
                    ctx.getBean(DashboardService.class),
                    ctx.getBean(MovimentacaoService.class),
                    ctx.getBean(CartaoService.class),
                    ctx.getBean(FaturaService.class),
                    ctx.getBean(ConfiguracoesService.class),
                    ctx.getBean(FinancasService.class)
            );
            window.setVisible(true);

            Runtime.getRuntime().addShutdownHook(new Thread(ctx::close));
        });
    }
}
