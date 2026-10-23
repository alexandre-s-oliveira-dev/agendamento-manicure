package com.belamao.agendamento.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.belamao.agendamento.entity.Agendamento;
import com.belamao.agendamento.entity.Bloqueio;
import com.belamao.agendamento.entity.HorarioFuncionamento;
import com.belamao.agendamento.entity.Servico;
import com.belamao.agendamento.entity.StatusAgendamento;
import com.belamao.agendamento.repository.AgendamentoRepository;
import com.belamao.agendamento.repository.BloqueioRepository;
import com.belamao.agendamento.repository.HorarioFuncionamentoRepository;
import com.belamao.agendamento.repository.ServicoRepository;

/**
 * Testa a regra dos horários livres sem precisar de banco de dados (os repositórios são "de mentira").
 * Datas no futuro distante para os testes não dependerem do dia em que são rodados.
 * 2030-01-07 é uma segunda-feira; 2030-01-06 é domingo.
 */
class DisponibilidadeServiceTest {

    private static final LocalDate SEGUNDA = LocalDate.of(2030, 1, 7);
    private static final LocalDate DOMINGO = LocalDate.of(2030, 1, 6);

    private ServicoRepository servicos;
    private HorarioFuncionamentoRepository horarios;
    private BloqueioRepository bloqueios;
    private AgendamentoRepository agendamentos;
    private DisponibilidadeService service;
    private Servico manicure;

    @BeforeEach
    void preparar() {
        servicos = mock(ServicoRepository.class);
        horarios = mock(HorarioFuncionamentoRepository.class);
        bloqueios = mock(BloqueioRepository.class);
        agendamentos = mock(AgendamentoRepository.class);
        service = new DisponibilidadeService(servicos, horarios, bloqueios, agendamentos);

        manicure = new Servico();
        manicure.setId(1L);
        manicure.setNome("Manicure");
        manicure.setDuracaoMinutos(45);
        manicure.setAtivo(true);
        when(servicos.findById(1L)).thenReturn(Optional.of(manicure));

        // Segunda-feira: atende de manhã, das 09:00 às 12:00.
        when(horarios.findByDiaSemanaAndAtivoTrueOrderByHoraInicio(DayOfWeek.MONDAY))
                .thenReturn(List.of(periodo("09:00", "12:00")));
    }

    @Test
    void diaLivreOferecaTodosOsHorariosQueCabem() {
        // Manicure de 45 min: o último que cabe começa às 11:00 (termina 11:45).
        assertThat(service.horariosDisponiveis(1L, SEGUNDA))
                .containsExactly("09:00", "09:30", "10:00", "10:30", "11:00");
    }

    @Test
    void agendamentoExistenteTiraOsHorariosQueBatemComEle() {
        when(agendamentos.findByDataAndStatusNot(SEGUNDA, StatusAgendamento.CANCELADO))
                .thenReturn(List.of(agendamento("10:00", "10:45")));
        // 09:30 terminaria 10:15 (invade as 10:00); 10:00 e 10:30 também batem.
        assertThat(service.horariosDisponiveis(1L, SEGUNDA)).containsExactly("09:00", "11:00");
    }

    @Test
    void diaInteiroBloqueadoNaoTemHorarios() {
        Bloqueio dia = new Bloqueio();
        dia.setData(SEGUNDA);
        when(bloqueios.findByData(SEGUNDA)).thenReturn(List.of(dia));
        assertThat(service.horariosDisponiveis(1L, SEGUNDA)).isEmpty();
    }

    @Test
    void bloqueioParcialTiraSoOPeriodoBloqueado() {
        Bloqueio parcial = new Bloqueio();
        parcial.setData(SEGUNDA);
        parcial.setHoraInicio(LocalTime.of(9, 0));
        parcial.setHoraFim(LocalTime.of(10, 0));
        when(bloqueios.findByData(SEGUNDA)).thenReturn(List.of(parcial));
        assertThat(service.horariosDisponiveis(1L, SEGUNDA)).containsExactly("10:00", "10:30", "11:00");
    }

    @Test
    void diaSemExpedienteNaoTemHorarios() {
        assertThat(service.horariosDisponiveis(1L, DOMINGO)).isEmpty();
    }

    @Test
    void dataNoPassadoNaoTemHorarios() {
        assertThat(service.horariosDisponiveis(1L, LocalDate.of(2020, 1, 6))).isEmpty();
    }

    @Test
    void servicoDesativadoNaoTemHorarios() {
        manicure.setAtivo(false);
        assertThat(service.horariosDisponiveis(1L, SEGUNDA)).isEmpty();
    }

    private HorarioFuncionamento periodo(String inicio, String fim) {
        HorarioFuncionamento h = new HorarioFuncionamento();
        h.setDiaSemana(DayOfWeek.MONDAY);
        h.setHoraInicio(LocalTime.parse(inicio));
        h.setHoraFim(LocalTime.parse(fim));
        h.setAtivo(true);
        return h;
    }

    private Agendamento agendamento(String inicio, String fim) {
        Agendamento a = new Agendamento();
        a.setId(99L);
        a.setData(SEGUNDA);
        a.setHoraInicio(LocalTime.parse(inicio));
        a.setHoraFim(LocalTime.parse(fim));
        a.setStatus(StatusAgendamento.CONFIRMADO);
        return a;
    }
}
