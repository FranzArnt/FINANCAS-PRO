package com.financas;

import javafx.application.Application;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class FinancasApplication {

    static ConfigurableApplicationContext springContext;

    public static void main(String[] args) {
        // Sobe o Spring Boot (servidor HTTP na porta 8080)
        springContext = SpringApplication.run(FinancasApplication.class, args);

        // Abre a janela JavaFX
        Application.launch(FinancasApp.class, args);
    }

}
