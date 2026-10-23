package com.belamao.agendamento.dto;

import java.time.LocalDate;

/** Quantidade de agendamentos (não cancelados) de um dia. Usado no gráfico da semana. */
public record ContagemDiaResponse(LocalDate data, int quantidade) {
}
