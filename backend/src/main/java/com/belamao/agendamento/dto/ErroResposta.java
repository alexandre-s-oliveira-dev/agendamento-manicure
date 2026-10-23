package com.belamao.agendamento.dto;

/** Formato padrão de erro da API: {"mensagem": "..."} (o frontend já espera esse campo). */
public record ErroResposta(String mensagem) {
}
