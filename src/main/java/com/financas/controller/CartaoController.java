package com.financas.controller;

import com.financas.model.*;
import com.financas.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/cartoes")
public class CartaoController {

    private final CartaoService cartaoService;
    private final FaturaService faturaService;
    private final MovimentacaoService movService;

    public CartaoController(CartaoService cartaoService,
                            FaturaService faturaService,
                            MovimentacaoService movService) {
        this.cartaoService = cartaoService;
        this.faturaService = faturaService;
        this.movService = movService;
    }

    @GetMapping
    public String lista(Model model) {
        var cartoes = cartaoService.listarAtivos();
        model.addAttribute("cartoes", cartoes);
        model.addAttribute("cartao", new CartaoCredito());
        model.addAttribute("contas", movService.listarContas());

        Map<Long, BigDecimal> limites = cartoes.stream()
                .collect(Collectors.toMap(CartaoCredito::getId, cartaoService::calcularLimiteDisponivel));

        // Percentual usado por cartão (0-100), calculado no servidor
        Map<Long, Integer> percentuaisUsados = cartoes.stream()
                .collect(Collectors.toMap(CartaoCredito::getId, c -> {
                    BigDecimal disponivel = limites.get(c.getId());
                    if (c.getLimiteTotal().compareTo(BigDecimal.ZERO) == 0) return 0;
                    BigDecimal usado = c.getLimiteTotal().subtract(disponivel);
                    return usado.divide(c.getLimiteTotal(), 2, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100)).intValue();
                }));

        model.addAttribute("limites", limites);
        model.addAttribute("percentuaisUsados", percentuaisUsados);
        model.addAttribute("proximosVencimentos", cartoes.stream()
                .collect(Collectors.toMap(CartaoCredito::getId, cartaoService::calcularProximoVencimento)));
        model.addAttribute("currentPage", "cartoes");
        return "cartoes/lista";
    }

    @PostMapping
    public String salvar(@ModelAttribute CartaoCredito cartao, RedirectAttributes ra) {
        try {
            cartaoService.salvar(cartao);
            ra.addFlashAttribute("sucesso", "Cartão salvo com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/cartoes";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        cartaoService.buscar(id).ifPresent(c -> model.addAttribute("cartao", c));
        // Reutiliza a mesma lógica do lista()
        lista(model);
        return "cartoes/lista";
    }

    @GetMapping("/{id}/desativar")
    public String desativar(@PathVariable Long id, RedirectAttributes ra) {
        cartaoService.desativar(id);
        ra.addFlashAttribute("sucesso", "Cartão desativado.");
        return "redirect:/cartoes";
    }

    @GetMapping("/{id}/faturas")
    public String faturas(@PathVariable Long id, Model model) {
        CartaoCredito cartao = cartaoService.buscar(id)
                .orElseThrow(() -> new IllegalArgumentException("Cartão não encontrado"));
        var faturas = faturaService.listarPorCartao(cartao);
        model.addAttribute("cartao", cartao);
        model.addAttribute("faturas", faturas);
        model.addAttribute("limiteDisponivel", cartaoService.calcularLimiteDisponivel(cartao));
        model.addAttribute("proximoVencimento", cartaoService.calcularProximoVencimento(cartao));
        model.addAttribute("currentPage", "cartoes");
        return "cartoes/faturas";
    }
}
