package com.belamao.agendamento.service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.belamao.agendamento.dto.HorarioDiaRequest;
import com.belamao.agendamento.dto.HorarioRequest;
import com.belamao.agendamento.dto.PeriodoRequest;
import com.belamao.agendamento.dto.HorarioResponse;
import com.belamao.agendamento.entity.HorarioFuncionamento;
import com.belamao.agendamento.exception.RecursoNaoEncontradoException;
import com.belamao.agendamento.exception.RegraNegocioException;
import com.belamao.agendamento.repository.HorarioFuncionamentoRepository;

@Service
public class HorarioService {

    private final HorarioFuncionamentoRepository repo;

    public HorarioService(HorarioFuncionamentoRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<HorarioResponse> listar() {
        return repo.findAll().stream()
                .sorted(Comparator.comparing(HorarioFuncionamento::getDiaSemana).thenComparing(HorarioFuncionamento::getHoraInicio))
                .map(HorarioResponse::de).toList();
    }

    @Transactional
    public HorarioResponse criar(HorarioRequest r) {
        boolean ativo = r.ativo() == null || r.ativo();
        if (ativo) validar(r.diaSemana(), r.horaInicio(), r.horaFim(), null);
        HorarioFuncionamento h = new HorarioFuncionamento();
        preencher(h, r, ativo);
        return HorarioResponse.de(repo.save(h));
    }

    @Transactional
    public HorarioResponse atualizar(Long id, HorarioRequest r) {
        HorarioFuncionamento h = repo.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Horário não encontrado."));
        boolean ativo = r.ativo() == null ? h.isAtivo() : r.ativo();
        if (ativo) validar(r.diaSemana(), r.horaInicio(), r.horaFim(), id);
        preencher(h, r, ativo);
        return HorarioResponse.de(h);
    }

    @Transactional
    public void excluir(Long id) {
        repo.delete(repo.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Horário não encontrado.")));
    }

    /** Troca, de uma vez só, todos os períodos de um dia da semana (lista vazia = dia fechado). */
    @Transactional
    public List<HorarioResponse> substituirDia(DayOfWeek dia, HorarioDiaRequest r) {
        List<PeriodoRequest> novos = r.periodos().stream()
                .sorted(Comparator.comparing(PeriodoRequest::horaInicio)).toList();
        for (int i = 0; i < novos.size(); i++) {
            PeriodoRequest p = novos.get(i);
            if (!p.horaFim().isAfter(p.horaInicio())) {
                throw new RegraNegocioException("O horário final deve ser depois do horário inicial.");
            }
            if (i > 0 && p.horaInicio().isBefore(novos.get(i - 1).horaFim())) {
                throw new RegraNegocioException("Os períodos do mesmo dia não podem se sobrepor.");
            }
        }
        repo.deleteAll(repo.findAll().stream().filter(h -> h.getDiaSemana() == dia).toList());
        repo.flush(); // garante que apaga antes de inserir
        List<HorarioFuncionamento> criados = novos.stream().map(p -> {
            HorarioFuncionamento h = new HorarioFuncionamento();
            h.setDiaSemana(dia);
            h.setHoraInicio(p.horaInicio());
            h.setHoraFim(p.horaFim());
            h.setAtivo(true);
            return h;
        }).toList();
        return repo.saveAll(criados).stream().map(HorarioResponse::de).toList();
    }

    private void preencher(HorarioFuncionamento h, HorarioRequest r, boolean ativo) {
        h.setDiaSemana(r.diaSemana());
        h.setHoraInicio(r.horaInicio());
        h.setHoraFim(r.horaFim());
        h.setAtivo(ativo);
    }

    private void validar(DayOfWeek dia, LocalTime inicio, LocalTime fim, Long ignorarId) {
        if (!fim.isAfter(inicio)) {
            throw new RegraNegocioException("O horário final deve ser depois do horário inicial.");
        }
        boolean sobrepoe = repo.findByDiaSemanaAndAtivoTrueOrderByHoraInicio(dia).stream()
                .filter(outro -> !outro.getId().equals(ignorarId))
                .anyMatch(outro -> inicio.isBefore(outro.getHoraFim()) && fim.isAfter(outro.getHoraInicio()));
        if (sobrepoe) {
            throw new RegraNegocioException("Esse período se sobrepõe a outro período do mesmo dia.");
        }
    }
}
