package com.financas;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class FinancasApp extends Application {

    private static final String URL = "http://localhost:8080/dashboard";
    private static final String TITULO = "FinancasPro";
    private static final int LARGURA = 1280;
    private static final int ALTURA = 800;

    @Override
    public void start(Stage stage) {
        // --- Tela de carregamento ---
        Label lblCarregando = new Label("Iniciando FinancasPro...");
        lblCarregando.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; -fx-font-size: 15px; -fx-text-fill: #64748B;");

        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(220);
        progressBar.setStyle("-fx-accent: #6366F1;");

        VBox splash = new VBox(14, lblCarregando, progressBar);
        splash.setAlignment(Pos.CENTER);
        splash.setPadding(new Insets(40));
        splash.setStyle("-fx-background-color: #F1F5F9;");

        // --- WebView ---
        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();

        // Desabilita menu de contexto padrão (clique direito)
        webView.setContextMenuEnabled(false);

        // Bloqueia navegação para fora do localhost
        engine.locationProperty().addListener((obs, oldUrl, newUrl) -> {
            if (newUrl != null && !newUrl.startsWith("http://localhost")) {
                Platform.runLater(() -> engine.load(oldUrl));
            }
        });

        StackPane root = new StackPane(webView, splash);

        // Quando a página carregar, esconde a splash
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                splash.setVisible(false);
            } else if (newState == Worker.State.FAILED) {
                lblCarregando.setText("Erro ao carregar. Tente reabrir.");
            }
        });

        // Fecha o Spring quando a janela for fechada
        stage.setOnCloseRequest(e -> {
            Platform.exit();
            FinancasApplication.springContext.close();
            System.exit(0);
        });

        Scene scene = new Scene(root, LARGURA, ALTURA);
        scene.setFill(Color.web("#F1F5F9"));

        stage.setTitle(TITULO);
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();

        // Carrega a URL depois que a janela abriu
        engine.load(URL);
    }
}
