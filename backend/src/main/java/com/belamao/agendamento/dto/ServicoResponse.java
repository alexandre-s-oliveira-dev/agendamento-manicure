package com.belamao.agendamento.dto;

import java.math.BigDecimal;

import com.belamao.agendamento.entity.Servico;

public record ServicoResponse(Long id, String nome, String descricao, BigDecimal preco, int duracaoMinutos, boolean ativo) {

    public static ServicoResponse de(Servico s) {
        return new ServicoResponse(s.getId(), s.getNome(), s.getDescricao(), s.getPreco(), s.getDuracaoMinutos(), s.isAtivo());
    }
}
