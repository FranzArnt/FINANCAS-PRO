package com.financas.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categorias")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private String icone;
    private String cor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoCategoria tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pai_id")
    private Categoria pai;

    @OneToMany(mappedBy = "pai", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Categoria> filhas = new ArrayList<>();

    @Column(nullable = false)
    private boolean ativa = true;

    public Categoria() {}

    public Categoria(String nome, String icone, String cor, TipoCategoria tipo) {
        this.nome = nome;
        this.icone = icone;
        this.cor = cor;
        this.tipo = tipo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getIcone() { return icone; }
    public void setIcone(String icone) { this.icone = icone; }
    public String getCor() { return cor; }
    public void setCor(String cor) { this.cor = cor; }
    public TipoCategoria getTipo() { return tipo; }
    public void setTipo(TipoCategoria tipo) { this.tipo = tipo; }
    public Categoria getPai() { return pai; }
    public void setPai(Categoria pai) { this.pai = pai; }
    public List<Categoria> getFilhas() { return filhas; }
    public void setFilhas(List<Categoria> filhas) { this.filhas = filhas; }
    public boolean isAtiva() { return ativa; }
    public void setAtiva(boolean ativa) { this.ativa = ativa; }

    public boolean isRaiz() { return pai == null; }
    public boolean isSubcategoria() { return pai != null; }
}
