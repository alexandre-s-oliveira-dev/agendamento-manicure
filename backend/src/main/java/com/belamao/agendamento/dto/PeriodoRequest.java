package com.belamao.agendamento.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

public record PeriodoRequest(
        @NotNull(message = "Informe o horário inicial.") LocalTime horaInicio,
        @NotNull(message = "Informe o horário final.") LocalTime horaFim) {
}
