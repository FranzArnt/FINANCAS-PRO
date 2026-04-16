package com.financas.controller;

import com.financas.model.*;
import com.financas.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/configuracoes")
public class ConfiguracoesController {

    private final ConfiguracoesService configService;
    private final MovimentacaoService movService;
    private final CartaoService cartaoService;

    public ConfiguracoesController(ConfiguracoesService configService,
                                   MovimentacaoService movService,
                                   CartaoService cartaoService) {
        this.configService = configService;
        this.movService = movService;
        this.cartaoService = cartaoService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("categorias", configService.listarRaizesComFilhas());
        model.addAttribute("contas", movService.listarContas());
        model.addAttribute("cartoes", cartaoService.listarAtivos());
        model.addAttribute("novaCategoria", new Categoria());
        model.addAttribute("novaConta", new ContaBancaria());
        model.addAttribute("currentPage", "configuracoes");
        return "configuracoes/index";
    }

    // ── Contas bancárias ──────────────────────────────────────────
    @PostMapping("/contas")
    public String salvarConta(@ModelAttribute ContaBancaria conta, RedirectAttributes ra) {
        try {
            movService.salvarConta(conta);
            ra.addFlashAttribute("sucesso", "Conta salva com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/configuracoes";
    }

    @GetMapping("/contas/{id}/desativar")
    public String desativarConta(@PathVariable Long id, RedirectAttributes ra) {
        movService.buscarConta(id).ifPresent(c -> {
            c.setAtiva(false);
            movService.salvarConta(c);
        });
        ra.addFlashAttribute("sucesso", "Conta desativada.");
        return "redirect:/configuracoes";
    }

    // ── Categorias ────────────────────────────────────────────────
    @PostMapping("/categorias")
    public String salvarCategoria(@ModelAttribute Categoria categoria, RedirectAttributes ra) {
        try {
            if (categoria.getPai() != null && categoria.getPai().getId() == null) {
                categoria.setPai(null);
            }
            configService.salvarCategoria(categoria);
            ra.addFlashAttribute("sucesso", "Categoria salva!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/configuracoes#categorias";
    }

    @GetMapping("/categorias/{id}/excluir")
    public String excluirCategoria(@PathVariable Long id, RedirectAttributes ra) {
        try {
            configService.excluirCategoria(id);
            ra.addFlashAttribute("sucesso", "Categoria excluída.");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/configuracoes#categorias";
    }

    @GetMapping("/categorias/{id}/desativar")
    public String desativarCategoria(@PathVariable Long id, RedirectAttributes ra) {
        configService.desativarCategoria(id);
        ra.addFlashAttribute("sucesso", "Categoria desativada.");
        return "redirect:/configuracoes#categorias";
    }

    @GetMapping("/categorias/{id}/ativar")
    public String ativarCategoria(@PathVariable Long id, RedirectAttributes ra) {
        configService.ativarCategoria(id);
        ra.addFlashAttribute("sucesso", "Categoria ativada.");
        return "redirect:/configuracoes#categorias";
    }
}
