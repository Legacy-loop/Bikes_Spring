package br.edu.ifc.bikes.config;

import br.edu.ifc.bikes.jwt.JwtAuthorizationFilter;
import br.edu.ifc.bikes.jwt.JwtUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@EnableMethodSecurity
@EnableWebMvc
@Configuration
public class SpringSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthorizationFilter jwtAuthorizationFilter) throws Exception{
        return http
                .csrf(csrf->csrf.disable())
                .formLogin(form->form.disable())
                .httpBasic(basic->basic.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/docs-bikes.html",
                                "/docs-bikes",
                                "/docs-bikes/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/usuarios").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth").permitAll()
                        .anyRequest().authenticated()
                ).exceptionHandling(exceptions -> exceptions
                        // Sem credenciais a resposta deve ser 401; 403 fica reservado a perfis sem permissão.
                        .authenticationEntryPoint((request, response, exception) -> {
                            String authorization = request.getHeader(JwtUtils.JWT_AUTHORIZATION);
                            boolean tokenValido = authorization != null
                                    && authorization.startsWith(JwtUtils.JWT_BEARER)
                                    && JwtUtils.isTokenValid(authorization);
                            // Um JWT válido sem permissão é 403; ausência ou invalidade do token é 401.
                            response.sendError(tokenValido
                                    ? HttpServletResponse.SC_FORBIDDEN
                                    : HttpServletResponse.SC_UNAUTHORIZED);
                        })
                        .accessDeniedHandler((request, response, exception) ->
                                response.sendError(HttpServletResponse.SC_FORBIDDEN))
                ).sessionManagement(
                        session->session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                ).addFilterBefore(
                        jwtAuthorizationFilter, UsernamePasswordAuthenticationFilter.class
                ).build();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception{
        return authenticationConfiguration.getAuthenticationManager();
    }

}
