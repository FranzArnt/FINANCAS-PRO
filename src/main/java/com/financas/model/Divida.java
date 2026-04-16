package com.financas.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity
@Table(name = "dividas")
public class Divida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private BigDecimal valorTotal;
    private BigDecimal valorRestante;
    private LocalDate vencimento;
    private String tipo;
    private String status;

    // Campos de parcelamento
    private Integer numeroParcelas;    // total de parcelas
    private Integer parcelaAtual;      // parcela em que está
    private BigDecimal valorParcela;   // valor mensal de cada parcela

    public Divida() {}

    public Divida(String nome, BigDecimal valorTotal, BigDecimal valorRestante,
                  LocalDate vencimento, String tipo, String status) {
        this.nome = nome;
        this.valorTotal = valorTotal;
        this.valorRestante = valorRestante;
        this.vencimento = vencimento;
        this.tipo = tipo;
        this.status = status;
    }

    public Divida(String nome, BigDecimal valorTotal, BigDecimal valorRestante,
                  LocalDate vencimento, String tipo, String status,
                  Integer numeroParcelas, Integer parcelaAtual, BigDecimal valorParcela) {
        this(nome, valorTotal, valorRestante, vencimento, tipo, status);
        this.numeroParcelas = numeroParcelas;
        this.parcelaAtual = parcelaAtual;
        this.valorParcela = valorParcela;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }
    public BigDecimal getValorRestante() { return valorRestante; }
    public void setValorRestante(BigDecimal valorRestante) { this.valorRestante = valorRestante; }
    public LocalDate getVencimento() { return vencimento; }
    public void setVencimento(LocalDate vencimento) { this.vencimento = vencimento; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getNumeroParcelas() { return numeroParcelas; }
    public void setNumeroParcelas(Integer numeroParcelas) { this.numeroParcelas = numeroParcelas; }
    public Integer getParcelaAtual() { return parcelaAtual; }
    public void setParcelaAtual(Integer parcelaAtual) { this.parcelaAtual = parcelaAtual; }
    public BigDecimal getValorParcela() { return valorParcela; }
    public void setValorParcela(BigDecimal valorParcela) { this.valorParcela = valorParcela; }

    public boolean isParcelada() {
        return numeroParcelas != null && numeroParcelas > 0;
    }

    public Integer getParcelasRestantes() {
        if (!isParcelada()) return null;
        return numeroParcelas - (parcelaAtual != null ? parcelaAtual : 0);
    }

    public BigDecimal getPercentualPago() {
        if (valorTotal == null || valorTotal.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        if (isParcelada() && parcelaAtual != null && numeroParcelas != null) {
            return new BigDecimal(parcelaAtual)
                    .divide(new BigDecimal(numeroParcelas), 2, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
        BigDecimal restante = valorRestante != null ? valorRestante : BigDecimal.ZERO;
        BigDecimal pago = valorTotal.subtract(restante);
        return pago.divide(valorTotal, 2, RoundingMode.HALF_UP)
                   .multiply(new BigDecimal("100"));
    }
}
