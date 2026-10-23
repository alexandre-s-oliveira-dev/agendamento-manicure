package com.belamao.agendamento.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import com.belamao.agendamento.entity.Bloqueio;

public interface BloqueioRepository extends JpaRepository<Bloqueio, Long> {

    List<Bloqueio> findByData(LocalDate data);
}
