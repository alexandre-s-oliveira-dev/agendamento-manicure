package com.belamao.agendamento.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.belamao.agendamento.entity.Usuario;

/** Gera o "crachá" (token) entregue após o login. Ele vale por algumas horas. */
@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final long horasDeValidade;

    public JwtService(JwtEncoder encoder, @Value("${app.jwt.expiracao-horas}") long horasDeValidade) {
        this.encoder = encoder;
        this.horasDeValidade = horasDeValidade;
    }

    public String gerar(Usuario usuario) {
        Instant agora = Instant.now();
        JwtClaimsSet dados = JwtClaimsSet.builder()
                .issuer("agendamento-api")
                .subject(usuario.getEmail())
                .issuedAt(agora)
                .expiresAt(agora.plus(horasDeValidade, ChronoUnit.HOURS))
                .claim("papel", usuario.getTipo().name())
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(cabecalho, dados)).getTokenValue();
    }

    public long segundosDeValidade() {
        return horasDeValidade * 3600;
    }
}
