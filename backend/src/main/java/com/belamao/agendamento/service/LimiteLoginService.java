package com.belamao.agendamento.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.belamao.agendamento.exception.MuitasTentativasException;

/**
 * Protege o login contra "adivinhar a senha": depois de 5 erros seguidos para o mesmo e-mail,
 * o login desse e-mail fica bloqueado por 15 minutos. Um login certo zera a contagem.
 *
 * Importante:
 * - A contagem é feita por e-mail (e não por IP), porque o IP pode ser falsificado em cabeçalhos.
 * - Vale também para e-mails que não existem, para ninguém descobrir quais e-mails estão cadastrados.
 * - Fica na memória do servidor: se o servidor reiniciar, as contagens zeram.
 */
@Service
public class LimiteLoginService {

    public static final int MAX_FALHAS = 5;
    public static final int MINUTOS_BLOQUEIO = 15;
    private static final Duration JANELA = Duration.ofMinutes(15);   // falhas antigas deixam de contar
    private static final Duration BLOQUEIO = Duration.ofMinutes(MINUTOS_BLOQUEIO);
    private static final int LIMITE_DE_REGISTROS = 5000;             // evita crescer sem parar

    private record Estado(int falhas, Instant primeiraFalha, Instant bloqueadoAte) {
        boolean bloqueado(Instant agora) {
            return bloqueadoAte != null && agora.isBefore(bloqueadoAte);
        }

        boolean expirado(Instant agora) {
            return bloqueadoAte != null ? !agora.isBefore(bloqueadoAte) : agora.isAfter(primeiraFalha.plus(JANELA));
        }
    }

    private final Map<String, Estado> estados = new ConcurrentHashMap<>();
    private final Clock relogio;

    public LimiteLoginService() {
        this(Clock.systemUTC());
    }

    // Usado nos testes, para "mexer no relógio".
    LimiteLoginService(Clock relogio) {
        this.relogio = relogio;
    }

    /** Lança erro (HTTP 429) se esse e-mail estiver bloqueado agora. Chame ANTES de conferir a senha. */
    public void verificarBloqueio(String email) {
        Instant agora = relogio.instant();
        Estado estado = estados.get(chave(email));
        if (estado != null && estado.bloqueado(agora)) {
            throw new MuitasTentativasException(Duration.between(agora, estado.bloqueadoAte()).toSeconds());
        }
    }

    /** Anota uma falha e devolve quantas tentativas ainda restam (0 = acabou de bloquear). */
    public int registrarFalha(String email) {
        Instant agora = relogio.instant();
        if (estados.size() > LIMITE_DE_REGISTROS) {
            estados.values().removeIf(e -> e.expirado(agora));
        }
        Estado novo = estados.compute(chave(email), (k, atual) -> {
            if (atual == null || atual.expirado(agora)) {
                return new Estado(1, agora, null);
            }
            int falhas = atual.falhas() + 1;
            return new Estado(falhas, atual.primeiraFalha(), falhas >= MAX_FALHAS ? agora.plus(BLOQUEIO) : null);
        });
        return Math.max(0, MAX_FALHAS - novo.falhas());
    }

    /** Login certo: zera a contagem. */
    public void registrarSucesso(String email) {
        estados.remove(chave(email));
    }

    private String chave(String email) {
        String c = email == null ? "" : email.trim().toLowerCase();
        return c.length() > 150 ? c.substring(0, 150) : c;
    }
}
