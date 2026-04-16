package com.financas.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "contas_bancarias")
public class ContaBancaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private String agencia;

    @Column(name = "numero_conta")
    private String conta;

    @Column(nullable = false)
    private BigDecimal saldoInicial = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean ativa = true;

    public ContaBancaria() {}

    public ContaBancaria(String nome, String agencia, String conta, BigDecimal saldoInicial) {
        this.nome = nome;
        this.agencia = agencia;
        this.conta = conta;
        this.saldoInicial = saldoInicial;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getAgencia() { return agencia; }
    public void setAgencia(String agencia) { this.agencia = agencia; }
    public String getConta() { return conta; }
    public void setConta(String conta) { this.conta = conta; }
    public BigDecimal getSaldoInicial() { return saldoInicial; }
    public void setSaldoInicial(BigDecimal saldoInicial) { this.saldoInicial = saldoInicial; }
    public boolean isAtiva() { return ativa; }
    public void setAtiva(boolean ativa) { this.ativa = ativa; }

    @Override public String toString() { return nome != null ? nome : ""; }
}
