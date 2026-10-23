package com.belamao.agendamento.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.belamao.agendamento.dto.ErroResposta;

/** Transforma exceções em respostas HTTP claras, sempre no formato {"mensagem": "..."}. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErroResposta naoEncontrado(RecursoNaoEncontradoException e) {
        return new ErroResposta(e.getMessage());
    }

    @ExceptionHandler(RegraNegocioException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResposta regraNegocio(RegraNegocioException e) {
        return new ErroResposta(e.getMessage());
    }

    @ExceptionHandler(NaoAutorizadoException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErroResposta naoAutorizado(NaoAutorizadoException e) {
        return new ErroResposta(e.getMessage());
    }

    @ExceptionHandler(MuitasTentativasException.class)
    public ResponseEntity<ErroResposta> muitasTentativas(MuitasTentativasException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(e.getSegundos()))
                .body(new ErroResposta(e.getMessage()));
    }

    @ExceptionHandler(ConflitoException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErroResposta conflito(ConflitoException e) {
        return new ErroResposta(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResposta validacao(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getDefaultMessage()).findFirst()
                .orElse("Dados inválidos. Confira os campos enviados.");
        return new ErroResposta(msg);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResposta formatoInvalido(Exception e) {
        return new ErroResposta("Dados inválidos. Confira os campos enviados (datas como 2026-10-20 e horas como 09:30).");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErroResposta integridade(DataIntegrityViolationException e) {
        String causa = e.getMostSpecificCause().getMessage();
        if (causa != null && causa.contains("agendamentos_sem_sobreposicao")) {
            return new ErroResposta("Esse horário acabou de ser reservado. Escolha outro horário.");
        }
        return new ErroResposta("Não foi possível concluir: este registro está em uso por outros dados.");
    }
}
