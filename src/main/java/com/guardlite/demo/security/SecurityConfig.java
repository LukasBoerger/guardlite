package com.guardlite.demo.security;

import com.guardlite.demo.security.jwt.JwtAuthenticationFilter;
import com.guardlite.demo.security.jwt.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // === Beans, die wir weiter unten im FilterChain nutzen ===

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Für /auth/login: validiert E-Mail+Passwort gegen DB
     */
    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService uds, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(uds);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    /**
     * CORS: erlaube deine Frontend-Origins
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        // in prod aus properties lesen!
        cfg.setAllowedOrigins(List.of("http://localhost:4200"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cfg.setExposedHeaders(List.of("Authorization"));
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    /**
     * 401 JSON statt Default-Login-Page
     */
    @Bean
    public AuthenticationEntryPoint authEntryPoint() {
        return (req, res, ex) -> {
            res.setStatus(401);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"unauthorized\"}");
        };
    }

    /**
     * 403 JSON bei fehlenden Rechten
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (req, res, ex) -> {
            res.setStatus(403);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"forbidden\"}");
        };
    }

    /**
     * Der Filter, der JWT aus dem Authorization-Header prüft
     */
    @Bean
    public JwtAuthenticationFilter jwtFilter(JwtService jwtService, UserDetailsService uds) {
        return new JwtAuthenticationFilter(jwtService, uds);
    }

    // === Eigentliche Security-Chain ===

    @Bean
    public SecurityFilterChain filter(HttpSecurity http,
                                      JwtAuthenticationFilter jwtFilter,
                                      AuthenticationEntryPoint entryPoint,
                                      AccessDeniedHandler deniedHandler) throws Exception {

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable()) // stateless API → kein CSRF
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(deniedHandler)
                )
                .authorizeHttpRequests(reg -> reg
                        // offen: Health, Login/Registrierung
                        .requestMatchers("/auth/**", "/actuator/health").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        // optional: statische Files, Swagger, etc.
                        // .requestMatchers("/v3/api-docs/**","/swagger-ui/**").permitAll()
                        // optional: Preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // alles andere: Auth nötig
                        .anyRequest().authenticated()
                )
                // JWT muss vor UsernamePasswordAuthenticationFilter laufen
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
