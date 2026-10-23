package com.belamao.agendamento.controller;

import java.time.DayOfWeek;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.belamao.agendamento.dto.HorarioDiaRequest;
import com.belamao.agendamento.dto.HorarioRequest;
import com.belamao.agendamento.dto.HorarioResponse;
import com.belamao.agendamento.service.HorarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/horarios")
public class HorarioController {

    private final HorarioService service;

    public HorarioController(HorarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<HorarioResponse> listar() {
        return service.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HorarioResponse criar(@Valid @RequestBody HorarioRequest request) {
        return service.criar(request);
    }

    @PutMapping("/{id}")
    public HorarioResponse atualizar(@PathVariable Long id, @Valid @RequestBody HorarioRequest request) {
        return service.atualizar(id, request);
    }

    // Exemplo: PUT /api/horarios/dia/MONDAY  {"periodos":[{"horaInicio":"09:00","horaFim":"12:00"}]}
    @PutMapping("/dia/{dia}")
    public List<HorarioResponse> substituirDia(@PathVariable DayOfWeek dia, @Valid @RequestBody HorarioDiaRequest request) {
        return service.substituirDia(dia, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }
}
