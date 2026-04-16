package com.financas.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "cartoes_credito")
public class CartaoCredito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Bandeira bandeira;

    @Column(nullable = false)
    private BigDecimal limiteTotal;

    private String cor;

    @Column(nullable = false)
    private int diaVencimento;

    @Column(nullable = false)
    private int diaFechamento;

    @Column(nullable = false)
    private boolean ativo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_id")
    private ContaBancaria conta;

    public CartaoCredito() {}

    public CartaoCredito(String nome, Bandeira bandeira, BigDecimal limiteTotal,
                         String cor, int diaVencimento, int diaFechamento,
                         ContaBancaria conta) {
        this.nome = nome;
        this.bandeira = bandeira;
        this.limiteTotal = limiteTotal;
        this.cor = cor;
        this.diaVencimento = diaVencimento;
        this.diaFechamento = diaFechamento;
        this.conta = conta;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public Bandeira getBandeira() { return bandeira; }
    public void setBandeira(Bandeira bandeira) { this.bandeira = bandeira; }
    public BigDecimal getLimiteTotal() { return limiteTotal; }
    public void setLimiteTotal(BigDecimal limiteTotal) { this.limiteTotal = limiteTotal; }
    public String getCor() { return cor; }
    public void setCor(String cor) { this.cor = cor; }
    public int getDiaVencimento() { return diaVencimento; }
    public void setDiaVencimento(int diaVencimento) { this.diaVencimento = diaVencimento; }
    public int getDiaFechamento() { return diaFechamento; }
    public void setDiaFechamento(int diaFechamento) { this.diaFechamento = diaFechamento; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public ContaBancaria getConta() { return conta; }
    public void setConta(ContaBancaria conta) { this.conta = conta; }
}
