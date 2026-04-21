package com.financas.model;

import com.financas.converter.YearMonthConverter;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("FATURA")
public class FaturaCartao extends Movimentacao {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cartao_id")
    private CartaoCredito cartao;

    @Convert(converter = YearMonthConverter.class)
    @Column(name = "mes_referencia")
    private YearMonth mesReferencia;

    private LocalDate dataFechamento;
    private LocalDate dataVencimento;

    @Enumerated(EnumType.STRING)
    private StatusFatura status = StatusFatura.ABERTA;

    @OneToMany(mappedBy = "fatura", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LancamentoFatura> lancamentos = new ArrayList<>();

    public FaturaCartao() {}

    public CartaoCredito getCartao() { return cartao; }
    public void setCartao(CartaoCredito cartao) { this.cartao = cartao; }
    public YearMonth getMesReferencia() { return mesReferencia; }
    public void setMesReferencia(YearMonth mesReferencia) { this.mesReferencia = mesReferencia; }
    public LocalDate getDataFechamento() { return dataFechamento; }
    public void setDataFechamento(LocalDate dataFechamento) { this.dataFechamento = dataFechamento; }
    public LocalDate getDataVencimento() { return dataVencimento; }
    public void setDataVencimento(LocalDate dataVencimento) { this.dataVencimento = dataVencimento; }
    public StatusFatura getStatus() { return status; }
    public void setStatus(StatusFatura status) { this.status = status; }
    public List<LancamentoFatura> getLancamentos() { return lancamentos; }
    public void setLancamentos(List<LancamentoFatura> lancamentos) { this.lancamentos = lancamentos; }
}
