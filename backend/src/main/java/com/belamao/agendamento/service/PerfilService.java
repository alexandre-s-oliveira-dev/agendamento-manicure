package com.belamao.agendamento.service;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.belamao.agendamento.entity.Usuario;
import com.belamao.agendamento.repository.UsuarioRepository;

/** Regras do "Meu perfil": a profissional troca nome, e-mail e senha. */
@Service
public class PerfilService {

    /** O que a tela mostra. */
    public record Dados(String nome, String email) {}

    /** O que a tela envia ao salvar. senhaAtual só é obrigatória para trocar e-mail ou senha. */
    public record Atualizacao(String nome, String email, String senhaAtual, String novaSenha) {}

    /** A resposta depois de salvar. Se trocou e-mail ou senha, a tela manda entrar de novo. */
    public record Resultado(String nome, String email, boolean precisaEntrarNovamente) {}

    /** Erro com o código HTTP certo (400, 401 ou 409). O PerfilController transforma em {"mensagem": "..."}. */
    public static class PerfilException extends RuntimeException {
        private final HttpStatus status;

        public PerfilException(HttpStatus status, String mensagem) {
            super(mensagem);
            this.status = status;
        }

        public HttpStatus getStatus() {
            return status;
        }
    }

    private static final int SENHA_MINIMA = 10;
    private static final int SENHA_MAXIMA_BYTES = 72; // limite do BCrypt

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final LimiteLoginService limite;

    public PerfilService(UsuarioRepository usuarios, PasswordEncoder encoder, LimiteLoginService limite) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.limite = limite;
    }

    @Transactional(readOnly = true)
    public Dados ver(String assuntoDoToken) {
        Usuario u = usuarioLogado(assuntoDoToken);
        return new Dados(u.getNome(), u.getEmail());
    }

    @Transactional
    public Resultado atualizar(String assuntoDoToken, Atualizacao a) {
        Usuario u = usuarioLogado(assuntoDoToken);

        String nome = a.nome() == null ? "" : a.nome().trim();
        String email = a.email() == null ? "" : a.email().trim().toLowerCase();
        String nova = a.novaSenha() == null ? "" : a.novaSenha();

        if (nome.isEmpty() || nome.length() > 100) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe um nome com até 100 caracteres.");
        }
        if (email.length() > 150 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe um e-mail válido.");
        }

        boolean mudouEmail = !email.equals(u.getEmail());
        boolean trocaSenha = !nova.isEmpty();

        if (trocaSenha) {
            if (nova.length() < SENHA_MINIMA) {
                throw erro(HttpStatus.BAD_REQUEST, "A nova senha precisa ter pelo menos " + SENHA_MINIMA + " caracteres.");
            }
            if (nova.getBytes(StandardCharsets.UTF_8).length > SENHA_MAXIMA_BYTES) {
                throw erro(HttpStatus.BAD_REQUEST, "A nova senha é longa demais (máximo de 72 caracteres).");
            }
        }

        // Para trocar e-mail ou senha, precisa provar que é ela mesma digitando a senha atual.
        if (mudouEmail || trocaSenha) {
            String chave = u.getEmail();
            limite.verificarBloqueio(chave); // mesmo bloqueio do login: 5 erros = 15 minutos
            if (a.senhaAtual() == null || !encoder.matches(a.senhaAtual(), u.getSenha())) {
                int restantes = limite.registrarFalha(chave);
                if (restantes > 0) {
                    throw erro(HttpStatus.BAD_REQUEST, "Senha atual incorreta. Restam " + restantes + " tentativa(s).");
                }
                throw erro(HttpStatus.BAD_REQUEST, "Senha atual incorreta. Por segurança, ficou bloqueado por "
                        + LimiteLoginService.MINUTOS_BLOQUEIO + " minutos.");
            }
            limite.registrarSucesso(chave);
        }

        if (mudouEmail) {
            Optional<Usuario> existente = usuarios.findByEmail(email);
            if (existente.isPresent() && !existente.get().getId().equals(u.getId())) {
                throw erro(HttpStatus.CONFLICT, "Esse e-mail já está em uso.");
            }
        }
        if (trocaSenha && encoder.matches(nova, u.getSenha())) {
            throw erro(HttpStatus.BAD_REQUEST, "A nova senha precisa ser diferente da atual.");
        }

        u.setNome(nome);
        u.setEmail(email);
        if (trocaSenha) {
            u.setSenha(encoder.encode(nova)); // guarda só o hash, nunca a senha
        }
        usuarios.save(u);
        return new Resultado(u.getNome(), u.getEmail(), mudouEmail || trocaSenha);
    }

    /** Descobre quem está logado a partir do "assunto" do token (pode ser o e-mail ou o id). */
    private Usuario usuarioLogado(String assunto) {
        if (assunto != null && !assunto.isBlank()) {
            Optional<Usuario> porEmail = usuarios.findByEmail(assunto.trim().toLowerCase());
            if (porEmail.isPresent()) {
                return porEmail.get();
            }
            try {
                Optional<Usuario> porId = usuarios.findById(Long.parseLong(assunto.trim()));
                if (porId.isPresent()) {
                    return porId.get();
                }
            } catch (NumberFormatException ignorado) {
                // não era um número; segue para o erro abaixo
            }
        }
        throw erro(HttpStatus.UNAUTHORIZED, "Sessão expirada. Entre novamente.");
    }

    private static PerfilException erro(HttpStatus status, String mensagem) {
        return new PerfilException(status, mensagem);
    }
}
