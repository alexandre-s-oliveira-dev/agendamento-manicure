package com.belamao.agendamento.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.belamao.agendamento.dto.ServicoRequest;
import com.belamao.agendamento.dto.ServicoResponse;
import com.belamao.agendamento.service.ServicoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/servicos")
public class ServicoController {

    private final ServicoService service;

    public ServicoController(ServicoService service) {
        this.service = service;
    }

    @GetMapping
    public List<ServicoResponse> listar(@RequestParam(required = false) Boolean ativo, Authentication autenticacao) {
        // Visitante (sem login) só enxerga serviços ativos; a profissional logada vê todos.
        boolean logada = autenticacao != null && autenticacao.isAuthenticated();
        return service.listar(logada ? ativo : Boolean.TRUE);
    }

    @GetMapping("/{id}")
    public ServicoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServicoResponse criar(@Valid @RequestBody ServicoRequest request) {
        return service.criar(request);
    }

    @PutMapping("/{id}")
    public ServicoResponse atualizar(@PathVariable Long id, @Valid @RequestBody ServicoRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }
}
