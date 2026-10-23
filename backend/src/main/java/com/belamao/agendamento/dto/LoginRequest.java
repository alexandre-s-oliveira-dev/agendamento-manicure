package com.belamao.agendamento.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Informe o e-mail e a senha.") String email,
        @NotBlank(message = "Informe o e-mail e a senha.") String senha) {
}
