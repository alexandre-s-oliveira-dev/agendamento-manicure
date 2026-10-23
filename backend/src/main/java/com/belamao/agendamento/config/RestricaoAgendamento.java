package com.belamao.agendamento.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Última linha de defesa contra horários duplicados: uma regra DENTRO do PostgreSQL que
 * recusa dois agendamentos (não cancelados) com períodos que se sobrepõem.
 * Funciona mesmo se duas clientes clicarem em "confirmar" no mesmo instante.
 */
@Component
public class RestricaoAgendamento implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RestricaoAgendamento.class);
    private static final String NOME = "agendamentos_sem_sobreposicao";

    @PersistenceContext
    private EntityManager em;

    private final PlatformTransactionManager transacoes;

    public RestricaoAgendamento(PlatformTransactionManager transacoes) {
        this.transacoes = transacoes;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            new TransactionTemplate(transacoes).executeWithoutResult(status -> {
                Number existe = (Number) em.createNativeQuery(
                        "select count(*) from pg_constraint where conname = '" + NOME + "'").getSingleResult();
                if (existe.longValue() == 0) {
                    em.createNativeQuery("alter table agendamentos add constraint " + NOME
                            + " exclude using gist (tsrange(data + hora_inicio, data + hora_fim) with &&)"
                            + " where (status <> 'CANCELADO')").executeUpdate();
                    log.info("Regra de horários sem sobreposição criada no banco.");
                }
            });
        } catch (Exception e) {
            log.warn("Não foi possível criar a regra de sobreposição no banco (a API continua conferindo): {}", e.getMessage());
        }
    }
}
