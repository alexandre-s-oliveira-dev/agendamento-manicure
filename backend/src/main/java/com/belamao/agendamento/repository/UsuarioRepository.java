package com.belamao.agendamento.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.belamao.agendamento.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);
}
