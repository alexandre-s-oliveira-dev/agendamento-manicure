package com.belamao.agendamento.exception;

/** Conflito, como horário já ocupado (vira HTTP 409). */
public class ConflitoException extends RuntimeException {
    public ConflitoException(String mensagem) { super(mensagem); }
}
