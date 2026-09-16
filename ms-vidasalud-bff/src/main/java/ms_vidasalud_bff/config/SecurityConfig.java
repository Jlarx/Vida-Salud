package ms_vidasalud_bff.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final JwtRoleConverter jwtRoleConverter;

    public SecurityConfig(JwtRoleConverter jwtRoleConverter) {
        this.jwtRoleConverter = jwtRoleConverter;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable()) // Para un Gateway / API es común desactivar CSRF
            .authorizeExchange(exchanges -> exchanges
                // 1. RUTA DE AUDITORÍA: Solo para Admin y Auditor
                .pathMatchers("/api/audit/**").hasAnyRole("ADMIN", "AUDITOR")
                
                // 2. RUTA DE CATÁLOGO: Todos pueden verlo (Recepcionistas configuran, Pacientes ven)
                .pathMatchers("/api/catalog/**").hasAnyRole("ADMIN", "OPERADOR", "CLIENTE")
                
                // 3. RUTA DE CITAS: Todos pueden interactuar (Recepcionistas agendan, Pacientes solicitan)
                .pathMatchers("/api/appointments/**").hasAnyRole("ADMIN", "OPERADOR", "CLIENTE")
                
                // Cualquier otra ruta exige estar logueado
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtRoleConverter)
                )
            );
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173")); // URL de React local
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
