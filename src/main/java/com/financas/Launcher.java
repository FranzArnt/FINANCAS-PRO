package com.financas;

/**
 * Ponto de entrada separado para contornar o conflito entre
 * o classloader do Spring Boot fat-jar e o JavaFX Application.
 */
public class Launcher {
    public static void main(String[] args) {
        FinancasApplication.main(args);
    }
}
