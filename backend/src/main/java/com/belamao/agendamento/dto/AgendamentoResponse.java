package com.belamao.agendamento.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.belamao.agendamento.entity.Agendamento;
import com.belamao.agendamento.entity.StatusAgendamento;

public record AgendamentoResponse(Long id, String nomeCliente, String telefone, Long servicoId, String servicoNome,
                                  LocalDate data, LocalTime horaInicio, LocalTime horaFim,
                                  StatusAgendamento status, String observacao, LocalDateTime criadoEm) {

    public static AgendamentoResponse de(Agendamento a) {
        return new AgendamentoResponse(a.getId(), a.getCliente().getNome(), a.getCliente().getTelefone(),
                a.getServico().getId(), a.getServico().getNome(), a.getData(), a.getHoraInicio(), a.getHoraFim(),
                a.getStatus(), a.getObservacao(), a.getCriadoEm());
    }
}
