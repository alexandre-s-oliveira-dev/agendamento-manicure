package com.belamao.agendamento.config;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.belamao.agendamento.entity.HorarioFuncionamento;
import com.belamao.agendamento.entity.Servico;
import com.belamao.agendamento.repository.HorarioFuncionamentoRepository;
import com.belamao.agendamento.repository.ServicoRepository;

/**
 * Cria dados de exemplo na primeira execução (só se as tabelas estiverem vazias).
 * Assim você já consegue ver serviços e horários no banco.
 */
@Component
public class DadosIniciais implements CommandLineRunner {

    private final ServicoRepository servicos;
    private final HorarioFuncionamentoRepository horarios;

    public DadosIniciais(ServicoRepository servicos, HorarioFuncionamentoRepository horarios) {
        this.servicos = servicos;
        this.horarios = horarios;
    }

    @Override
    public void run(String... args) {
        if (servicos.count() == 0) {
            servicos.save(servico("Manicure", "Cutilagem, lixamento e esmaltação.", "35.00", 45));
            servicos.save(servico("Pedicure", "Cuidado completo dos pés com esmaltação.", "50.00", 60));
            servicos.save(servico("Mão e pé", "Manicure e pedicure no mesmo horário.", "80.00", 105));
            servicos.save(servico("Esmaltação em gel", "Brilho e durabilidade por até 3 semanas.", "70.00", 60));
            servicos.save(servico("Alongamento em fibra", "Alongamento com acabamento natural.", "140.00", 120));
            servicos.save(servico("Remoção de gel", "Remoção segura, sem agredir a unha.", "30.00", 30));
        }
        if (horarios.count() == 0) {
            for (DayOfWeek dia : new DayOfWeek[] {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY}) {
                horarios.save(periodo(dia, "09:00", "12:00"));
                horarios.save(periodo(dia, "14:00", "18:00"));
            }
            horarios.save(periodo(DayOfWeek.SATURDAY, "09:00", "13:00"));
            // Domingo: sem registros = fechado.
        }
    }

    private Servico servico(String nome, String descricao, String preco, int duracaoMinutos) {
        Servico s = new Servico();
        s.setNome(nome);
        s.setDescricao(descricao);
        s.setPreco(new BigDecimal(preco));
        s.setDuracaoMinutos(duracaoMinutos);
        s.setAtivo(true);
        return s;
    }

    private HorarioFuncionamento periodo(DayOfWeek dia, String inicio, String fim) {
        HorarioFuncionamento h = new HorarioFuncionamento();
        h.setDiaSemana(dia);
        h.setHoraInicio(LocalTime.parse(inicio));
        h.setHoraFim(LocalTime.parse(fim));
        h.setAtivo(true);
        return h;
    }
}
