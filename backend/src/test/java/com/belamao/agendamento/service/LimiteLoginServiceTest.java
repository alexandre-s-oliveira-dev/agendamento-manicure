package com.belamao.agendamento.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.belamao.agendamento.exception.MuitasTentativasException;

/** Testa o bloqueio de login depois de muitas tentativas erradas (com um relógio que a gente controla). */
class LimiteLoginServiceTest {

    private static final String EMAIL = "profissional@exemplo.com";

    private RelogioManual relogio;
    private LimiteLoginService limite;

    @BeforeEach
    void preparar() {
        relogio = new RelogioManual();
        limite = new LimiteLoginService(relogio);
    }

    @Test
    void bloqueiaNaQuintaFalha() {
        for (int i = 0; i < 4; i++) limite.registrarFalha(EMAIL);
        assertThatCode(() -> limite.verificarBloqueio(EMAIL)).doesNotThrowAnyException(); // 4 erros ainda pode

        assertThat(limite.registrarFalha(EMAIL)).isZero(); // 5º erro: acabaram as tentativas
        assertThatThrownBy(() -> limite.verificarBloqueio(EMAIL)).isInstanceOf(MuitasTentativasException.class);
    }

    @Test
    void liberaDepoisDeQuinzeMinutos() {
        for (int i = 0; i < 5; i++) limite.registrarFalha(EMAIL);
        relogio.avancar(Duration.ofMinutes(16));
        assertThatCode(() -> limite.verificarBloqueio(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void loginCertoZeraAContagem() {
        for (int i = 0; i < 4; i++) limite.registrarFalha(EMAIL);
        limite.registrarSucesso(EMAIL);
        assertThat(limite.registrarFalha(EMAIL)).isEqualTo(4); // recomeçou do zero
    }

    @Test
    void emailsDiferentesNaoSeAfetam() {
        for (int i = 0; i < 5; i++) limite.registrarFalha(EMAIL);
        assertThatCode(() -> limite.verificarBloqueio("outra@exemplo.com")).doesNotThrowAnyException();
    }

    @Test
    void falhasAntigasNaoContam() {
        for (int i = 0; i < 4; i++) limite.registrarFalha(EMAIL);
        relogio.avancar(Duration.ofMinutes(16));
        assertThat(limite.registrarFalha(EMAIL)).isEqualTo(4); // contou como a primeira falha de novo
    }

    @Test
    void ignoraMaiusculasEEspacosNoEmail() {
        for (int i = 0; i < 5; i++) limite.registrarFalha("  PROFISSIONAL@Exemplo.com ");
        assertThatThrownBy(() -> limite.verificarBloqueio(EMAIL)).isInstanceOf(MuitasTentativasException.class);
    }

    /** Relógio de mentira: só anda quando mandamos. */
    private static class RelogioManual extends Clock {
        private Instant agora = Instant.parse("2030-01-07T12:00:00Z");

        void avancar(Duration duracao) { agora = agora.plus(duracao); }

        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return agora; }
    }
}
