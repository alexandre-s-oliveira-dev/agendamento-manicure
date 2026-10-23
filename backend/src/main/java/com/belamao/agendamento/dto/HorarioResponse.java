package com.belamao.agendamento.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.belamao.agendamento.entity.HorarioFuncionamento;

public record HorarioResponse(Long id, DayOfWeek diaSemana, LocalTime horaInicio, LocalTime horaFim, boolean ativo) {

    public static HorarioResponse de(HorarioFuncionamento h) {
        return new HorarioResponse(h.getId(), h.getDiaSemana(), h.getHoraInicio(), h.getHoraFim(), h.isAtivo());
    }
}
