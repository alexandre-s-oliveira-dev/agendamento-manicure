package com.belamao.agendamento.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.belamao.agendamento.service.DisponibilidadeService;

@RestController
@RequestMapping("/api/horarios-disponiveis")
public class DisponibilidadeController {

    private final DisponibilidadeService service;

    public DisponibilidadeController(DisponibilidadeService service) {
        this.service = service;
    }

    // Exemplo: /api/horarios-disponiveis?servicoId=1&data=2026-10-20
    @GetMapping
    public List<String> listar(@RequestParam Long servicoId,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return service.horariosDisponiveis(servicoId, data);
    }
}
