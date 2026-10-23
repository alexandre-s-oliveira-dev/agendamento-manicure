package com.belamao.agendamento.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.belamao.agendamento.dto.LoginRequest;
import com.belamao.agendamento.dto.LoginResponse;
import com.belamao.agendamento.entity.Usuario;
import com.belamao.agendamento.exception.NaoAutorizadoException;
import com.belamao.agendamento.repository.UsuarioRepository;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final LimiteLoginService limite;
    // Usado quando o e-mail não existe, para gastar o mesmo tempo e não revelar quais e-mails existem.
    private final String hashFalso;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder encoder, JwtService jwt, LimiteLoginService limite) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwt = jwt;
        this.limite = limite;
        this.hashFalso = encoder.encode("senha-falsa-apenas-para-igualar-o-tempo");
    }

    public LoginResponse entrar(LoginRequest r) {
        String email = r.email().trim().toLowerCase();
        limite.verificarBloqueio(email); // se estiver bloqueado, nem confere a senha

        Usuario usuario = usuarios.findByEmail(email).orElse(null);
        boolean senhaConfere = encoder.matches(r.senha(), usuario != null ? usuario.getSenha() : hashFalso);
        if (usuario == null || !senhaConfere) {
            int restantes = limite.registrarFalha(email);
            log.warn("Tentativa de login falhou para {} (restam {} antes do bloqueio)", email, restantes);
            if (restantes > 0) {
                throw new NaoAutorizadoException("E-mail ou senha incorretos. Restam " + restantes + " tentativa(s).");
            }
            throw new NaoAutorizadoException("E-mail ou senha incorretos. Por segurança, o login ficou bloqueado por "
                    + LimiteLoginService.MINUTOS_BLOQUEIO + " minutos.");
        }
        limite.registrarSucesso(email);
        return new LoginResponse(jwt.gerar(usuario), usuario.getNome(), jwt.segundosDeValidade());
    }
}
