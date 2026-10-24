package com.belamao.agendamento.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.belamao.agendamento.service.PerfilService;

/** "Meu perfil": só quem está logado (ADMIN) chega aqui, a SecurityConfig já protege. */
@RestController
@RequestMapping("/api/perfil")
public class PerfilController {

    private final PerfilService service;

    public PerfilController(PerfilService service) {
        this.service = service;
    }

    @GetMapping
    public PerfilService.Dados ver(@AuthenticationPrincipal Jwt token) {
        return service.ver(token.getSubject());
    }

    @PutMapping
    public PerfilService.Resultado atualizar(@AuthenticationPrincipal Jwt token,
                                             @RequestBody PerfilService.Atualizacao corpo) {
        return service.atualizar(token.getSubject(), corpo);
    }

    /** Devolve os erros no mesmo formato do resto da API: {"mensagem": "..."} */
    @ExceptionHandler(PerfilService.PerfilException.class)
    public ResponseEntity<Map<String, String>> erro(PerfilService.PerfilException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("mensagem", e.getMessage()));
    }
}
