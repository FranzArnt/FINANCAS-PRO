package com.financas.service;

import com.financas.model.CartaoCredito;
import com.financas.repository.CartaoCreditoRepository;
import com.financas.repository.FaturaRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
public class CartaoService {

    private final CartaoCreditoRepository cartaoRepo;
    private final FaturaRepository faturaRepo;
    private final FaturaService faturaService;

    public CartaoService(CartaoCreditoRepository cartaoRepo,
                         FaturaRepository faturaRepo,
                         @Lazy FaturaService faturaService) {
        this.cartaoRepo = cartaoRepo;
        this.faturaRepo = faturaRepo;
        this.faturaService = faturaService;
    }

    public List<CartaoCredito> listarAtivos() {
        return cartaoRepo.findByAtivoTrue();
    }

    public List<CartaoCredito> listarTodos() {
        return cartaoRepo.findAll();
    }

    public Optional<CartaoCredito> buscar(Long id) {
        return cartaoRepo.findById(id);
    }

    public CartaoCredito salvar(CartaoCredito cartao) {
        if (cartao.getDiaFechamento() == 0) {
            cartao.setDiaFechamento(calcularDiaFechamento(cartao));
        }
        boolean isNovo = cartao.getId() == null;
        CartaoCredito salvo = cartaoRepo.save(cartao);
        if (isNovo) {
            faturaService.gerarFaturaSeNaoExistir(salvo, YearMonth.now());
        }
        return salvo;
    }

    public void desativar(Long id) {
        cartaoRepo.findById(id).ifPresent(c -> {
            c.setAtivo(false);
            cartaoRepo.save(c);
        });
    }

    public int calcularDiaFechamento(CartaoCredito cartao) {
        int fechamento = cartao.getDiaVencimento() - 7;
        return fechamento <= 0 ? fechamento + 31 : fechamento;
    }

    public BigDecimal calcularLimiteDisponivel(CartaoCredito cartao) {
        BigDecimal utilizado = faturaRepo.sumFaturasAbertas(cartao);
        return cartao.getLimiteTotal().subtract(utilizado);
    }

    public LocalDate calcularProximoVencimento(CartaoCredito cartao) {
        LocalDate hoje = LocalDate.now();
        int dia = Math.min(cartao.getDiaVencimento(), hoje.lengthOfMonth());
        LocalDate candidato = hoje.withDayOfMonth(dia);
        return candidato.isBefore(hoje) ? candidato.plusMonths(1) : candidato;
    }
}
