package com.belamao.agendamento.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import com.belamao.agendamento.entity.Servico;

public interface ServicoRepository extends JpaRepository<Servico, Long> {

    List<Servico> findByAtivoTrue();
}
