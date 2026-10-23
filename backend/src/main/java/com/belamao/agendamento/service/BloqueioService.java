package com.belamao.agendamento.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.belamao.agendamento.dto.BloqueioRequest;
import com.belamao.agendamento.dto.BloqueioResponse;
import com.belamao.agendamento.entity.Bloqueio;
import com.belamao.agendamento.exception.RecursoNaoEncontradoException;
import com.belamao.agendamento.exception.RegraNegocioException;
import com.belamao.agendamento.repository.BloqueioRepository;

@Service
public class BloqueioService {

    private final BloqueioRepository repo;

    public BloqueioService(BloqueioRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<BloqueioResponse> listar() {
        return repo.findAll(Sort.by("data", "horaInicio")).stream().map(BloqueioResponse::de).toList();
    }

    @Transactional
    public BloqueioResponse criar(BloqueioRequest r) {
        if ((r.horaInicio() == null) != (r.horaFim() == null)) {
            throw new RegraNegocioException("Informe o horário inicial e o final, ou deixe os dois vazios para bloquear o dia inteiro.");
        }
        if (r.horaInicio() != null && !r.horaFim().isAfter(r.horaInicio())) {
            throw new RegraNegocioException("O horário final deve ser depois do horário inicial.");
        }
        Bloqueio b = new Bloqueio();
        b.setData(r.data());
        b.setHoraInicio(r.horaInicio());
        b.setHoraFim(r.horaFim());
        b.setMotivo(r.motivo() == null || r.motivo().isBlank() ? null : r.motivo().trim());
        return BloqueioResponse.de(repo.save(b));
    }

    @Transactional
    public void excluir(Long id) {
        repo.delete(repo.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Bloqueio não encontrado.")));
    }
}
