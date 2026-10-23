package com.belamao.agendamento.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.belamao.agendamento.dto.ServicoRequest;
import com.belamao.agendamento.dto.ServicoResponse;
import com.belamao.agendamento.entity.Servico;
import com.belamao.agendamento.exception.RecursoNaoEncontradoException;
import com.belamao.agendamento.repository.ServicoRepository;

@Service
public class ServicoService {

    private final ServicoRepository repo;

    public ServicoService(ServicoRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<ServicoResponse> listar(Boolean apenasAtivos) {
        return repo.findAll(Sort.by("id")).stream()
                .filter(s -> !Boolean.TRUE.equals(apenasAtivos) || s.isAtivo())
                .map(ServicoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public ServicoResponse buscar(Long id) {
        return ServicoResponse.de(buscarEntidade(id));
    }

    @Transactional
    public ServicoResponse criar(ServicoRequest r) {
        Servico s = new Servico();
        preencher(s, r);
        s.setAtivo(r.ativo() == null || r.ativo());
        return ServicoResponse.de(repo.save(s));
    }

    @Transactional
    public ServicoResponse atualizar(Long id, ServicoRequest r) {
        Servico s = buscarEntidade(id);
        preencher(s, r);
        if (r.ativo() != null) s.setAtivo(r.ativo());
        return ServicoResponse.de(s);
    }

    @Transactional
    public void excluir(Long id) {
        Servico s = buscarEntidade(id);
        repo.delete(s);
        repo.flush(); // se houver agendamentos usando este serviço, o erro aparece aqui (HTTP 409)
    }

    private void preencher(Servico s, ServicoRequest r) {
        s.setNome(r.nome().trim());
        s.setDescricao(r.descricao() == null ? null : r.descricao().trim());
        s.setPreco(r.preco());
        s.setDuracaoMinutos(r.duracaoMinutos());
    }

    private Servico buscarEntidade(Long id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado."));
    }
}
