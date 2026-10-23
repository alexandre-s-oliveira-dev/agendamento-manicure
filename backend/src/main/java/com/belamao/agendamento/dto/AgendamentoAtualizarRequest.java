package com.belamao.agendamento.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.belamao.agendamento.entity.StatusAgendamento;

import jakarta.validation.constraints.Size;

/**
 * Usado pela profissional. Todos os campos são opcionais: envie só o que quer mudar.
 * - status: confirmar ou cancelar
 * - data/horario: remarcar
 */
public record AgendamentoAtualizarRequest(
        StatusAgendamento status,
        LocalDate data,
        LocalTime horario,
        @Size(max = 300, message = "A observação pode ter até 300 letras.") String observacao) {
}
