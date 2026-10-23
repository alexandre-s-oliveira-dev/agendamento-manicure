package com.belamao.agendamento.repository;

import java.time.DayOfWeek;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import com.belamao.agendamento.entity.HorarioFuncionamento;

public interface HorarioFuncionamentoRepository extends JpaRepository<HorarioFuncionamento, Long> {

    List<HorarioFuncionamento> findByDiaSemanaAndAtivoTrueOrderByHoraInicio(DayOfWeek diaSemana);
}
