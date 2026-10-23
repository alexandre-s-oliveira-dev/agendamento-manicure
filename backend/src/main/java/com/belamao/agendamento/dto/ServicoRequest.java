package com.belamao.agendamento.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ServicoRequest(
        @NotBlank(message = "Informe o nome do serviço.") @Size(max = 100, message = "O nome pode ter até 100 letras.") String nome,
        @Size(max = 500, message = "A descrição pode ter até 500 letras.") String descricao,
        @NotNull(message = "Informe o preço.") @DecimalMin(value = "0.0", message = "O preço não pode ser negativo.") BigDecimal preco,
        @NotNull(message = "Informe a duração em minutos.") @Min(value = 5, message = "A duração mínima é de 5 minutos.") Integer duracaoMinutos,
        Boolean ativo) {
}
