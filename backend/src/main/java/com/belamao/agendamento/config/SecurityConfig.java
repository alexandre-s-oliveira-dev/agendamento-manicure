package com.belamao.agendamento.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Quem pode acessar o quê.
 * Públicas (cliente, sem login): ver serviços, ver horários livres, criar agendamento, fazer login.
 * Todo o resto exige o papel ADMIN (a profissional logada).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        // Lê o papel (ADMIN) que guardamos dentro do token.
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName("papel");
        autoridades.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(autoridades);

        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable()) // API sem cookies de sessão: o login vai no cabeçalho Authorization
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/saude", "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/servicos", "/api/servicos/*", "/api/horarios-disponiveis").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/agendamentos").permitAll()
                .anyRequest().hasRole("ADMIN"))
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(conversor)));
        return http.build();
    }

    /** Senhas são guardadas com BCrypt (hash seguro), nunca em texto puro. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
