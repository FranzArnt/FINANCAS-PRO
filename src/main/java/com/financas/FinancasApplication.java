package com.financas;

import javafx.application.Application;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.awt.Desktop;
import java.net.URI;

@SpringBootApplication
public class FinancasApplication {

    static ConfigurableApplicationContext springContext;
    private static final String URL = "http://localhost:8080/dashboard";

    public static void main(String[] args) {
        springContext = SpringApplication.run(FinancasApplication.class, args);

        try {
            Application.launch(FinancasApp.class, args);
        } catch (Exception e) {
            System.out.println("\n[FinancasPro] Janela nativa indisponivel. Abrindo no navegador...");
            abrirNavegador();
            System.out.println("[FinancasPro] Acesse: " + URL);
            System.out.println("[FinancasPro] Pressione CTRL+C para encerrar o aplicativo.\n");
            springContext.registerShutdownHook();
        }
    }

    private static void abrirNavegador() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(URL));
            }
        } catch (Exception ignored) {
        }
    }
}
