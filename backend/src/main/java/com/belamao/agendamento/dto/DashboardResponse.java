package com.belamao.agendamento.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Números da tela inicial do painel. Os tempos estão em minutos (só de hoje).
 * faturamentoRealizadoMes = serviços já concluídos; faturamentoPrevistoMes = pendentes e confirmados.
 */
public record DashboardResponse(LocalDate hoje, int agendamentosHoje, long totalClientes, long servicosAtivos,
                                int minutosOcupadosHoje, int minutosLivresHoje,
                                BigDecimal faturamentoRealizadoMes, BigDecimal faturamentoPrevistoMes,
                                List<ContagemDiaResponse> semana, List<AgendamentoResponse> proximos) {
}
