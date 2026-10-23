package com.belamao.agendamento.exception;

/** Login recusado (vira HTTP 401). */
public class NaoAutorizadoException extends RuntimeException {
    public NaoAutorizadoException(String mensagem) { super(mensagem); }
}
