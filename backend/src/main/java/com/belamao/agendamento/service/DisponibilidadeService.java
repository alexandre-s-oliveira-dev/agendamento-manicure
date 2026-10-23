package com.belamao.agendamento.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.belamao.agendamento.entity.Agendamento;
import com.belamao.agendamento.entity.Bloqueio;
import com.belamao.agendamento.entity.HorarioFuncionamento;
import com.belamao.agendamento.entity.Servico;
import com.belamao.agendamento.entity.StatusAgendamento;
import com.belamao.agendamento.exception.RecursoNaoEncontradoException;
import com.belamao.agendamento.repository.AgendamentoRepository;
import com.belamao.agendamento.repository.BloqueioRepository;
import com.belamao.agendamento.repository.HorarioFuncionamentoRepository;
import com.belamao.agendamento.repository.ServicoRepository;

/**
 * Descobre quais horários estão realmente livres para um serviço em uma data.
 * Considera: horário de funcionamento, duração do serviço, agendamentos existentes,
 * bloqueios (parciais ou do dia inteiro) e horários que já passaram.
 */
@Service
public class DisponibilidadeService {

    /** De quanto em quanto tempo um horário pode começar (08:00, 08:30, 09:00...). */
    private static final int PASSO_MINUTOS = 30;
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final ServicoRepository servicos;
    private final HorarioFuncionamentoRepository horarios;
    private final BloqueioRepository bloqueios;
    private final AgendamentoRepository agendamentos;

    public DisponibilidadeService(ServicoRepository servicos, HorarioFuncionamentoRepository horarios,
                                  BloqueioRepository bloqueios, AgendamentoRepository agendamentos) {
        this.servicos = servicos;
        this.horarios = horarios;
        this.bloqueios = bloqueios;
        this.agendamentos = agendamentos;
    }

    /** Lista de horários livres no formato "09:00", "09:30"... */
    @Transactional(readOnly = true)
    public List<String> horariosDisponiveis(Long servicoId, LocalDate data) {
        Servico servico = servicos.findById(servicoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado."));
        if (!servico.isAtivo()) return List.of();
        return calcular(servico, data).stream().map(HORA::format).toList();
    }

    /** Usado ao criar o agendamento: confirma que aquele horário continua livre. */
    @Transactional(readOnly = true)
    public boolean horarioLivre(Servico servico, LocalDate data, LocalTime inicio) {
        return calcular(servico, data).contains(inicio);
    }

    private List<LocalTime> calcular(Servico servico, LocalDate data) {
        List<LocalTime> livres = new ArrayList<>();
        LocalDateTime agora = LocalDateTime.now(FUSO);
        if (data.isBefore(agora.toLocalDate())) return livres;

        List<Bloqueio> bloqueiosDoDia = bloqueios.findByData(data);
        if (bloqueiosDoDia.stream().anyMatch(b -> b.getHoraInicio() == null)) return livres; // dia inteiro

        List<Agendamento> ocupados = agendamentos.findByDataAndStatusNot(data, StatusAgendamento.CANCELADO);
        int duracao = servico.getDuracaoMinutos();

        for (HorarioFuncionamento periodo : horarios.findByDiaSemanaAndAtivoTrueOrderByHoraInicio(data.getDayOfWeek())) {
            for (LocalTime t = periodo.getHoraInicio(); ; t = t.plusMinutes(PASSO_MINUTOS)) {
                LocalTime inicio = t;
                LocalTime fim = inicio.plusMinutes(duracao);
                if (!fim.isAfter(inicio) || fim.isAfter(periodo.getHoraFim())) break; // não cabe mais neste período
                if (data.equals(agora.toLocalDate()) && !inicio.isAfter(agora.toLocalTime())) continue; // já passou

                boolean batePedido = ocupados.stream()
                        .anyMatch(a -> inicio.isBefore(a.getHoraFim()) && fim.isAfter(a.getHoraInicio()));
                boolean bateBloqueio = bloqueiosDoDia.stream()
                        .anyMatch(b -> inicio.isBefore(b.getHoraFim()) && fim.isAfter(b.getHoraInicio()));
                if (!batePedido && !bateBloqueio) livres.add(inicio);
            }
        }
        return livres;
    }
}
