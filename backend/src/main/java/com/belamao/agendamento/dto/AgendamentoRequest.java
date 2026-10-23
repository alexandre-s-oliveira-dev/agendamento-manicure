package com.belamao.agendamento.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Dados que a cliente envia ao agendar (o frontend já usa exatamente estes nomes). */
public record AgendamentoRequest(
        @NotNull(message = "Escolha um serviço.") Long servicoId,
        @NotNull(message = "Escolha uma data.") LocalDate data,
        @NotNull(message = "Escolha um horário.") LocalTime horario,
        @NotBlank(message = "Preencha seu nome e telefone.") @Size(max = 100, message = "O nome pode ter até 100 letras.") String nomeCliente,
        @NotBlank(message = "Preencha seu nome e telefone.") String telefone,
        @Size(max = 300, message = "A observação pode ter até 300 letras.") String observacao) {
}
