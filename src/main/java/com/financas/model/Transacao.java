package com.financas.model;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("TRANSACAO")
public class Transacao extends Movimentacao {

    @Enumerated(EnumType.STRING)
    private TipoTransacao tipo;

    @Enumerated(EnumType.STRING)
    private DirecaoTransacao direcao;

    private String descricao;
    private String origem;
    private String destino;

    public Transacao() {}

    public TipoTransacao getTipo() { return tipo; }
    public void setTipo(TipoTransacao tipo) { this.tipo = tipo; }
    public DirecaoTransacao getDirecao() { return direcao; }
    public void setDirecao(DirecaoTransacao direcao) { this.direcao = direcao; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }
    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }
}
