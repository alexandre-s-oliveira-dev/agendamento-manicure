package com.belamao.agendamento.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.belamao.agendamento.dto.AgendamentoAtualizarRequest;
import com.belamao.agendamento.dto.AgendamentoRequest;
import com.belamao.agendamento.dto.AgendamentoResponse;
import com.belamao.agendamento.entity.Agendamento;
import com.belamao.agendamento.entity.Cliente;
import com.belamao.agendamento.entity.Servico;
import com.belamao.agendamento.entity.StatusAgendamento;
import com.belamao.agendamento.exception.ConflitoException;
import com.belamao.agendamento.exception.RecursoNaoEncontradoException;
import com.belamao.agendamento.exception.RegraNegocioException;
import com.belamao.agendamento.repository.AgendamentoRepository;
import com.belamao.agendamento.repository.ClienteRepository;
import com.belamao.agendamento.repository.ServicoRepository;

@Service
public class AgendamentoService {

    // Horário de Brasília. Importante: o servidor da Render roda em UTC.
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final AgendamentoRepository agendamentos;
    private final ClienteRepository clientes;
    private final ServicoRepository servicos;
    private final DisponibilidadeService disponibilidade;

    public AgendamentoService(AgendamentoRepository agendamentos, ClienteRepository clientes,
                              ServicoRepository servicos, DisponibilidadeService disponibilidade) {
        this.agendamentos = agendamentos;
        this.clientes = clientes;
        this.servicos = servicos;
        this.disponibilidade = disponibilidade;
    }

    /** Sem filtros lista tudo; com "data" lista um dia; com "de" e "ate" lista um período. */
    @Transactional(readOnly = true)
    public List<AgendamentoResponse> listar(LocalDate data, LocalDate de, LocalDate ate) {
        List<Agendamento> lista;
        if (data != null) {
            lista = agendamentos.findByDataBetweenOrderByDataAscHoraInicioAsc(data, data);
        } else if (de != null && ate != null) {
            lista = agendamentos.findByDataBetweenOrderByDataAscHoraInicioAsc(de, ate);
        } else {
            lista = agendamentos.findAll(Sort.by("data", "horaInicio"));
        }
        return lista.stream().map(AgendamentoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public AgendamentoResponse buscar(Long id) {
        return AgendamentoResponse.de(buscarEntidade(id));
    }

    @Transactional
    public AgendamentoResponse criar(AgendamentoRequest r) {
        Servico servico = servicos.findById(r.servicoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado."));
        if (!servico.isAtivo()) {
            throw new RegraNegocioException("Esse serviço não está disponível no momento.");
        }
        String telefone = r.telefone().replaceAll("\\D", "");
        if (telefone.length() < 10 || telefone.length() > 11) {
            throw new RegraNegocioException("Informe um telefone válido com DDD.");
        }
        LocalTime inicio = r.horario().truncatedTo(ChronoUnit.MINUTES);
        LocalTime fim = calcularFim(inicio, servico);
        if (LocalDateTime.of(r.data(), inicio).isBefore(LocalDateTime.now(FUSO))) {
            throw new RegraNegocioException("Esse horário já passou. Escolha outro.");
        }
        verificarConflito(r.data(), inicio, fim, null);
        // Funcionamento, bloqueios e duração: o horário precisa estar na lista de disponíveis.
        if (!disponibilidade.horarioLivre(servico, r.data(), inicio)) {
            throw new RegraNegocioException("Esse serviço não está disponível nesse horário.");
        }

        // Reaproveita a cliente se o telefone já existe; senão cria uma nova.
        Cliente cliente = clientes.findByTelefone(telefone).orElseGet(Cliente::new);
        cliente.setNome(r.nomeCliente().trim());
        cliente.setTelefone(telefone);
        cliente = clientes.save(cliente);

        Agendamento a = new Agendamento();
        a.setCliente(cliente);
        a.setServico(servico);
        a.setData(r.data());
        a.setHoraInicio(inicio);
        a.setHoraFim(fim);
        a.setStatus(StatusAgendamento.PENDENTE);
        a.setObservacao(r.observacao() == null || r.observacao().isBlank() ? null : r.observacao().trim());
        return AgendamentoResponse.de(agendamentos.saveAndFlush(a)); // saveAndFlush: a regra do banco age aqui
    }

    @Transactional
    public AgendamentoResponse atualizar(Long id, AgendamentoAtualizarRequest r) {
        Agendamento a = buscarEntidade(id);
        LocalDate data = r.data() != null ? r.data() : a.getData();
        LocalTime inicio = r.horario() != null ? r.horario() : a.getHoraInicio();
        LocalTime fim = calcularFim(inicio, a.getServico());
        StatusAgendamento status = r.status() != null ? r.status() : a.getStatus();

        boolean remarcou = !data.equals(a.getData()) || !inicio.equals(a.getHoraInicio());
        boolean reativou = a.getStatus() == StatusAgendamento.CANCELADO && status != StatusAgendamento.CANCELADO;
        if (status != StatusAgendamento.CANCELADO && (remarcou || reativou)) {
            verificarConflito(data, inicio, fim, a.getId());
        }
        a.setData(data);
        a.setHoraInicio(inicio);
        a.setHoraFim(fim);
        a.setStatus(status);
        if (r.observacao() != null) a.setObservacao(r.observacao().isBlank() ? null : r.observacao().trim());
        agendamentos.flush();
        return AgendamentoResponse.de(a);
    }

    @Transactional
    public void excluir(Long id) {
        agendamentos.delete(buscarEntidade(id));
    }

    private LocalTime calcularFim(LocalTime inicio, Servico servico) {
        LocalTime fim = inicio.plusMinutes(servico.getDuracaoMinutos());
        if (!fim.isAfter(inicio)) { // passou da meia-noite
            throw new RegraNegocioException("O atendimento não pode passar da meia-noite.");
        }
        return fim;
    }

    /** Nenhum outro agendamento (não cancelado) pode ocupar o mesmo período. */
    private void verificarConflito(LocalDate data, LocalTime inicio, LocalTime fim, Long ignorarId) {
        boolean ocupado = agendamentos.buscarConflitos(data, inicio, fim, StatusAgendamento.CANCELADO).stream()
                .anyMatch(outro -> !outro.getId().equals(ignorarId));
        if (ocupado) {
            throw new ConflitoException("Esse horário acabou de ser reservado. Escolha outro horário.");
        }
    }

    private Agendamento buscarEntidade(Long id) {
        return agendamentos.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado."));
    }
}
