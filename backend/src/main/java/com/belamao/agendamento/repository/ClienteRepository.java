package com.belamao.agendamento.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.belamao.agendamento.entity.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByTelefone(String telefone);
}
