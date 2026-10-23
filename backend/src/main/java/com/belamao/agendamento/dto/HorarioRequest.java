package com.belamao.agendamento.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

/** diaSemana em inglês: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY. */
public record HorarioRequest(
        @NotNull(message = "Informe o dia da semana.") DayOfWeek diaSemana,
        @NotNull(message = "Informe o horário inicial.") LocalTime horaInicio,
        @NotNull(message = "Informe o horário final.") LocalTime horaFim,
        Boolean ativo) {
}
