package com.belamao.agendamento.exception;

/** Pedido inválido pelas regras do sistema (vira HTTP 400). */
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) { super(mensagem); }
}
