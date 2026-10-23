package com.belamao.agendamento.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.belamao.agendamento.dto.BloqueioRequest;
import com.belamao.agendamento.dto.BloqueioResponse;
import com.belamao.agendamento.service.BloqueioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bloqueios")
public class BloqueioController {

    private final BloqueioService service;

    public BloqueioController(BloqueioService service) {
        this.service = service;
    }

    @GetMapping
    public List<BloqueioResponse> listar() {
        return service.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BloqueioResponse criar(@Valid @RequestBody BloqueioRequest request) {
        return service.criar(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }
}
