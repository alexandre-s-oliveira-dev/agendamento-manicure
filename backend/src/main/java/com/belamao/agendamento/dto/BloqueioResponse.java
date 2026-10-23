package com.belamao.agendamento.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.belamao.agendamento.entity.Bloqueio;

public record BloqueioResponse(Long id, LocalDate data, LocalTime horaInicio, LocalTime horaFim, boolean diaInteiro, String motivo) {

    public static BloqueioResponse de(Bloqueio b) {
        return new BloqueioResponse(b.getId(), b.getData(), b.getHoraInicio(), b.getHoraFim(),
                b.getHoraInicio() == null, b.getMotivo());
    }
}
