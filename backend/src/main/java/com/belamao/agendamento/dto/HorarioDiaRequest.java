package com.belamao.agendamento.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Todos os períodos de um dia. Lista vazia = dia fechado. */
public record HorarioDiaRequest(
        @NotNull(message = "Envie a lista de períodos.")
        @Size(max = 6, message = "No máximo 6 períodos por dia.")
        @Valid List<PeriodoRequest> periodos) {
}
