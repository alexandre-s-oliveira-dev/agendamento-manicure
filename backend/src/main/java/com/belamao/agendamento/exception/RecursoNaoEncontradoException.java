package com.belamao.agendamento.exception;

/** Algo que foi pedido não existe (vira HTTP 404). */
public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String mensagem) { super(mensagem); }
}
