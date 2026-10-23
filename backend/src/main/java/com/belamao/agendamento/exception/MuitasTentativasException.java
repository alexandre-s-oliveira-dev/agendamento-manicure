package com.belamao.agendamento.exception;

/** Muitas tentativas de login erradas (vira HTTP 429). */
public class MuitasTentativasException extends RuntimeException {

    private final long segundos;

    public MuitasTentativasException(long segundos) {
        super("Muitas tentativas de login. Por segurança, tente novamente em "
                + Math.max(1, (long) Math.ceil(segundos / 60.0)) + " minuto(s).");
        this.segundos = segundos;
    }

    public long getSegundos() { return segundos; }
}
