package com.financas.controller;

import com.financas.model.*;
import com.financas.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/transacoes")
public class TransacaoController {

    private final MovimentacaoService movService;
    private final ConfiguracoesService configService;

    public TransacaoController(MovimentacaoService movService, ConfiguracoesService configService) {
        this.movService = movService;
        this.configService = configService;
    }

    @GetMapping
    public String lista(Model model) {
        model.addAttribute("transacoes", movService.listarTodasTransacoes());
        model.addAttribute("contas", movService.listarContas());
        model.addAttribute("categorias", configService.listarTodasAtivas());
        model.addAttribute("novaTransacao", new Transacao());
        model.addAttribute("currentPage", "transacoes");
        return "transacoes/lista";
    }

    @PostMapping
    public String salvar(@ModelAttribute Transacao transacao, RedirectAttributes ra) {
        try {
            movService.salvarTransacao(transacao);
            ra.addFlashAttribute("sucesso", "Transação registrada!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/transacoes";
    }

    @GetMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes ra) {
        movService.excluirTransacao(id);
        ra.addFlashAttribute("sucesso", "Transação excluída.");
        return "redirect:/transacoes";
    }
}
