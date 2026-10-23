package com.belamao.agendamento.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Sem horaInicio/horaFim = o dia inteiro fica bloqueado. */
public record BloqueioRequest(
        @NotNull(message = "Informe a data do bloqueio.") LocalDate data,
        LocalTime horaInicio,
        LocalTime horaFim,
        @Size(max = 200, message = "O motivo pode ter até 200 letras.") String motivo) {
}
