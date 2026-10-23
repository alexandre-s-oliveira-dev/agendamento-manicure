package com.belamao.agendamento.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint simples para confirmar que a API está no ar (também útil no deploy). */
@RestController
@RequestMapping("/api")
public class SaudeController {

    @GetMapping("/saude")
    public Map<String, String> saude() {
        return Map.of("status", "ok", "servico", "agendamento-api");
    }
}
