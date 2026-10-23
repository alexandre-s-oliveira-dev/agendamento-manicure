package com.belamao.agendamento.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.belamao.agendamento.entity.TipoUsuario;
import com.belamao.agendamento.entity.Usuario;
import com.belamao.agendamento.repository.UsuarioRepository;

/**
 * Cria a conta da profissional na primeira execução, usando ADMIN_NOME, ADMIN_EMAIL e ADMIN_SENHA
 * (variáveis de ambiente / .env). A senha é guardada como hash BCrypt. Nada de senha no código.
 */
@Component
public class AdminInicial implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicial.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final String nome;
    private final String email;
    private final String senha;

    public AdminInicial(UsuarioRepository usuarios, PasswordEncoder encoder,
                        @Value("${app.admin.nome}") String nome,
                        @Value("${app.admin.email}") String email,
                        @Value("${app.admin.senha}") String senha) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.nome = nome.trim();
        this.email = email.trim().toLowerCase();
        this.senha = senha;
    }

    @Override
    public void run(String... args) {
        if (nome.isEmpty() || email.isEmpty() || senha.isEmpty()) {
            log.warn("ADMIN_NOME, ADMIN_EMAIL e ADMIN_SENHA não definidos: nenhuma conta de profissional foi criada.");
            return;
        }
        if (usuarios.findByEmail(email).isPresent()) {
            return; // já existe
        }
        if (senha.length() < 10) {
            log.warn("ADMIN_SENHA precisa ter pelo menos 10 caracteres. Conta não criada.");
            return;
        }
        Usuario admin = new Usuario();
        admin.setNome(nome);
        admin.setEmail(email);
        admin.setSenha(encoder.encode(senha));
        admin.setTipo(TipoUsuario.ADMIN);
        usuarios.save(admin);
        log.info("Conta da profissional criada: {}", email);
    }
}
