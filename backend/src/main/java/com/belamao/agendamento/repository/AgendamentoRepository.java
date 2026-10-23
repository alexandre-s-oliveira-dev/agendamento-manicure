package com.belamao.agendamento.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.belamao.agendamento.entity.Agendamento;
import com.belamao.agendamento.entity.StatusAgendamento;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    // Agendamentos de um dia, ignorando um status (ex.: CANCELADO).
    List<Agendamento> findByDataAndStatusNot(LocalDate data, StatusAgendamento status);

    // Usado pelo calendário: dia, semana ou mês (de..ate, inclusive).
    List<Agendamento> findByDataBetweenOrderByDataAscHoraInicioAsc(LocalDate de, LocalDate ate);

    // Próximos agendamentos (a partir de uma data), já em ordem.
    List<Agendamento> findTop20ByDataGreaterThanEqualAndStatusNotOrderByDataAscHoraInicioAsc(LocalDate data, StatusAgendamento status);

    // Dois períodos se sobrepõem quando um começa antes do outro terminar.
    @Query("select a from Agendamento a where a.data = :data and a.status <> :ignorado "
            + "and a.horaInicio < :fim and a.horaFim > :inicio")
    List<Agendamento> buscarConflitos(@Param("data") LocalDate data, @Param("inicio") LocalTime inicio,
                                      @Param("fim") LocalTime fim, @Param("ignorado") StatusAgendamento ignorado);
}
