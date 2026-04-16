package com.financas.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity
@Table(name = "investimentos")
public class Investimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private BigDecimal valorInvestido;
    private BigDecimal valorAtual;
    private LocalDate dataAplicacao;
    private String tipo;
    private String status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "conta_id")
    private ContaBancaria conta;

    public Investimento() {}

    public Investimento(String nome, BigDecimal valorInvestido, BigDecimal valorAtual,
                        LocalDate dataAplicacao, String tipo, String status) {
        this.nome = nome;
        this.valorInvestido = valorInvestido;
        this.valorAtual = valorAtual;
        this.dataAplicacao = dataAplicacao;
        this.tipo = tipo;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public BigDecimal getValorInvestido() { return valorInvestido; }
    public void setValorInvestido(BigDecimal valorInvestido) { this.valorInvestido = valorInvestido; }
    public BigDecimal getValorAtual() { return valorAtual; }
    public void setValorAtual(BigDecimal valorAtual) { this.valorAtual = valorAtual; }
    public LocalDate getDataAplicacao() { return dataAplicacao; }
    public void setDataAplicacao(LocalDate dataAplicacao) { this.dataAplicacao = dataAplicacao; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public ContaBancaria getConta() { return conta; }
    public void setConta(ContaBancaria conta) { this.conta = conta; }

    public BigDecimal getRentabilidade() {
        if (valorInvestido.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return valorAtual.subtract(valorInvestido)
                         .divide(valorInvestido, 4, RoundingMode.HALF_UP)
                         .multiply(new BigDecimal("100"))
                         .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getLucro() {
        return valorAtual.subtract(valorInvestido);
    }
}
