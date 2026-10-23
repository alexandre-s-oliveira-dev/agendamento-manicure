package com.belamao.agendamento.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.belamao.agendamento.dto.AgendamentoRequest;
import com.belamao.agendamento.dto.AgendamentoResponse;
import com.belamao.agendamento.entity.Agendamento;
import com.belamao.agendamento.entity.Cliente;
import com.belamao.agendamento.entity.Servico;
import com.belamao.agendamento.entity.StatusAgendamento;
import com.belamao.agendamento.exception.ConflitoException;
import com.belamao.agendamento.exception.RegraNegocioException;
import com.belamao.agendamento.repository.AgendamentoRepository;
import com.belamao.agendamento.repository.ClienteRepository;
import com.belamao.agendamento.repository.ServicoRepository;

/** Testa as regras de criar agendamento sem banco de dados. */
class AgendamentoServiceTest {

    private static final LocalDate SEGUNDA = LocalDate.of(2030, 1, 7);

    private AgendamentoRepository agendamentos;
    private ClienteRepository clientes;
    private DisponibilidadeService disponibilidade;
    private AgendamentoService service;

    @BeforeEach
    void preparar() {
        agendamentos = mock(AgendamentoRepository.class);
        clientes = mock(ClienteRepository.class);
        ServicoRepository servicos = mock(ServicoRepository.class);
        disponibilidade = mock(DisponibilidadeService.class);
        service = new AgendamentoService(agendamentos, clientes, servicos, disponibilidade);

        Servico manicure = new Servico();
        manicure.setId(1L);
        manicure.setNome("Manicure");
        manicure.setDuracaoMinutos(45);
        manicure.setAtivo(true);
        when(servicos.findById(1L)).thenReturn(Optional.of(manicure));
    }

    @Test
    void criaAgendamentoQuandoOHorarioEstaLivre() {
        when(clientes.findByTelefone("98912345678")).thenReturn(Optional.empty());
        when(clientes.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));
        when(agendamentos.saveAndFlush(any(Agendamento.class))).thenAnswer(i -> i.getArgument(0));
        when(disponibilidade.horarioLivre(any(), any(), any())).thenReturn(true);

        AgendamentoResponse r = service.criar(pedido("10:00", "(98) 91234-5678"));

        assertThat(r.status()).isEqualTo(StatusAgendamento.PENDENTE);
        assertThat(r.horaInicio()).isEqualTo(LocalTime.of(10, 0));
        assertThat(r.horaFim()).isEqualTo(LocalTime.of(10, 45)); // 10:00 + 45 min
        assertThat(r.telefone()).isEqualTo("98912345678");        // só números
    }

    @Test
    void recusaHorarioJaOcupado() {
        Agendamento existente = new Agendamento();
        existente.setId(99L);
        when(agendamentos.buscarConflitos(any(), any(), any(), any())).thenReturn(List.of(existente));

        assertThatThrownBy(() -> service.criar(pedido("10:00", "(98) 91234-5678")))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Esse horário acabou de ser reservado. Escolha outro horário.");
    }

    @Test
    void recusaHorarioForaDaListaDeDisponiveis() {
        when(disponibilidade.horarioLivre(any(), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.criar(pedido("10:00", "(98) 91234-5678")))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void recusaTelefoneInvalido() {
        assertThatThrownBy(() -> service.criar(pedido("10:00", "123")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("telefone");
    }

    @Test
    void recusaHorarioQueJaPassou() {
        AgendamentoRequest antigo = new AgendamentoRequest(1L, LocalDate.of(2020, 1, 6), LocalTime.of(10, 0),
                "Maria", "(98) 91234-5678", null);
        assertThatThrownBy(() -> service.criar(antigo))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("já passou");
    }

    private AgendamentoRequest pedido(String horario, String telefone) {
        return new AgendamentoRequest(1L, SEGUNDA, LocalTime.parse(horario), "Maria", telefone, null);
    }
}
