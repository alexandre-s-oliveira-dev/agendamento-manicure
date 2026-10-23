package com.belamao.agendamento.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.belamao.agendamento.dto.AgendamentoResponse;
import com.belamao.agendamento.dto.ContagemDiaResponse;
import com.belamao.agendamento.dto.DashboardResponse;
import com.belamao.agendamento.entity.Agendamento;
import com.belamao.agendamento.entity.Bloqueio;
import com.belamao.agendamento.entity.StatusAgendamento;
import com.belamao.agendamento.repository.AgendamentoRepository;
import com.belamao.agendamento.repository.BloqueioRepository;
import com.belamao.agendamento.repository.ClienteRepository;
import com.belamao.agendamento.repository.HorarioFuncionamentoRepository;
import com.belamao.agendamento.repository.ServicoRepository;

@Service
public class DashboardService {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final AgendamentoRepository agendamentos;
    private final ClienteRepository clientes;
    private final ServicoRepository servicos;
    private final HorarioFuncionamentoRepository horarios;
    private final BloqueioRepository bloqueios;

    public DashboardService(AgendamentoRepository agendamentos, ClienteRepository clientes, ServicoRepository servicos,
                            HorarioFuncionamentoRepository horarios, BloqueioRepository bloqueios) {
        this.agendamentos = agendamentos;
        this.clientes = clientes;
        this.servicos = servicos;
        this.horarios = horarios;
        this.bloqueios = bloqueios;
    }

    @Transactional(readOnly = true)
    public DashboardResponse resumo() {
        LocalDateTime agora = LocalDateTime.now(FUSO);
        LocalDate hoje = agora.toLocalDate();

        List<Agendamento> deHoje = agendamentos.findByDataAndStatusNot(hoje, StatusAgendamento.CANCELADO);
        int ocupados = deHoje.stream().mapToInt(a -> minutos(a.getHoraInicio(), a.getHoraFim())).sum();

        // Tempo de atendimento do dia = períodos de funcionamento menos bloqueios.
        List<Bloqueio> bloqueiosHoje = bloqueios.findByData(hoje);
        int expediente = 0;
        if (bloqueiosHoje.stream().noneMatch(b -> b.getHoraInicio() == null)) {
            expediente = horarios.findByDiaSemanaAndAtivoTrueOrderByHoraInicio(hoje.getDayOfWeek()).stream()
                    .mapToInt(h -> minutos(h.getHoraInicio(), h.getHoraFim())).sum();
            int bloqueado = bloqueiosHoje.stream().mapToInt(b -> minutos(b.getHoraInicio(), b.getHoraFim())).sum();
            expediente = Math.max(0, expediente - bloqueado);
        }

        List<AgendamentoResponse> proximos = agendamentos
                .findTop20ByDataGreaterThanEqualAndStatusNotOrderByDataAscHoraInicioAsc(hoje, StatusAgendamento.CANCELADO)
                .stream()
                .filter(a -> a.getStatus() != StatusAgendamento.CONCLUIDO)
                .filter(a -> a.getData().isAfter(hoje) || a.getHoraFim().isAfter(agora.toLocalTime()))
                .limit(5)
                .map(AgendamentoResponse::de).toList();

        // Faturamento do mês: soma do preço dos serviços (hoje, o preço vem do cadastro do serviço).
        List<Agendamento> doMes = agendamentos.findByDataBetweenOrderByDataAscHoraInicioAsc(
                hoje.withDayOfMonth(1), hoje.withDayOfMonth(hoje.lengthOfMonth()));
        BigDecimal realizado = somar(doMes, a -> a.getStatus() == StatusAgendamento.CONCLUIDO);
        BigDecimal previsto = somar(doMes, a -> a.getStatus() == StatusAgendamento.PENDENTE
                || a.getStatus() == StatusAgendamento.CONFIRMADO);

        // Semana de domingo a sábado (igual ao calendário do painel).
        LocalDate domingo = hoje.minusDays(hoje.getDayOfWeek().getValue() % 7);
        List<Agendamento> daSemana = agendamentos.findByDataBetweenOrderByDataAscHoraInicioAsc(domingo, domingo.plusDays(6));
        List<ContagemDiaResponse> semana = IntStream.range(0, 7).mapToObj(i -> {
            LocalDate dia = domingo.plusDays(i);
            int qtd = (int) daSemana.stream()
                    .filter(a -> a.getData().equals(dia) && a.getStatus() != StatusAgendamento.CANCELADO).count();
            return new ContagemDiaResponse(dia, qtd);
        }).toList();

        return new DashboardResponse(hoje, deHoje.size(), clientes.count(), servicos.findByAtivoTrue().size(),
                ocupados, Math.max(0, expediente - ocupados), realizado, previsto, semana, proximos);
    }

    private BigDecimal somar(List<Agendamento> lista, Predicate<Agendamento> filtro) {
        return lista.stream().filter(filtro).map(a -> a.getServico().getPreco()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int minutos(java.time.LocalTime inicio, java.time.LocalTime fim) {
        return (int) Duration.between(inicio, fim).toMinutes();
    }
}
