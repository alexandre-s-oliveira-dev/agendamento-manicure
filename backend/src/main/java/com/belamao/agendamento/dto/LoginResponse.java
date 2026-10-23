package com.belamao.agendamento.dto;

public record LoginResponse(String token, String nome, long expiraEmSegundos) {
}
