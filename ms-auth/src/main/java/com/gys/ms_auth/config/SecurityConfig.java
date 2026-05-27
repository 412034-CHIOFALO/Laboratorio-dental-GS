package com.gys.ms_auth.config;

import com.gys.ms_auth.service.CustomUserDetailsService;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.cert.X509Certificate;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    // Keystore PKCS12 persistido en classpath:keys/gys-auth.p12
    // La contraseña se inyecta desde env var GYS_KEYSTORE_PASSWORD (con default de desarrollo)
    @Value("${gys.auth.keystore.path:classpath:keys/gys-auth.p12}")
    private Resource keystorePath;

    @Value("${gys.auth.keystore.password:gys_keystore_2025}")
    private String keystorePassword;

    @Value("${gys.auth.keystore.alias:gys-auth}")
    private String keystoreAlias;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    // 1. Filtro del Servidor de Autorización OAuth2 (flujo OIDC estándar)
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
            .oidc(Customizer.withDefaults());
        http.exceptionHandling(ex -> ex
            .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
        ).oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }

    // 2. Filtro general — login abierto, H2 console restringida, register requiere JWT ADMIN
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // H2 Console solo en desarrollo — requiere autenticación básica
                .requestMatchers("/h2-console/**").hasRole("ADMIN")
                .requestMatchers("/api/auth/register", "/api/auth/usuarios/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            // Necesario para que H2 console cargue en iframe (solo dev)
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }

    // 3. CORS — permite peticiones solo desde Angular (localhost:4200)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // 4. BCrypt para contraseñas
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 5. AuthenticationManager con DaoAuthenticationProvider + BCrypt
    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
    }

    /**
     * Carga la RSAKey desde el keystore PKCS12 persistido en disco.
     * Esto garantiza que los JWT emitidos antes de un reinicio siguen siendo válidos,
     * ya que la clave pública no cambia entre reinicios.
     */
    @Bean
    public RSAKey rsaKey() {
        try (InputStream is = keystorePath.getInputStream()) {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(is, keystorePassword.toCharArray());

            RSAPrivateKey privateKey = (RSAPrivateKey) keyStore.getKey(
                keystoreAlias, keystorePassword.toCharArray());
            RSAPublicKey publicKey = (RSAPublicKey)
                ((X509Certificate) keyStore.getCertificate(keystoreAlias)).getPublicKey();

            return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(keystoreAlias)
                .build();
        } catch (Exception e) {
            throw new IllegalStateException(
                "No se pudo cargar el keystore RSA desde: " + keystorePath, e);
        }
    }

    // 7. JWKSource usando la RSAKey del keystore
    @Bean
    public JWKSource<SecurityContext> jwkSource(RSAKey rsaKey) {
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    // 8. JwtDecoder — validación de tokens entrantes en ms-auth
    @Bean
    public JwtDecoder jwtDecoder(RSAKey rsaKey) {
        try {
            return NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
        } catch (Exception e) {
            throw new IllegalStateException("Error configurando JwtDecoder", e);
        }
    }

    // 9. JwtEncoder — emisión de tokens en AuthController
    @Bean
    public NimbusJwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    // 10. Configuración general del Authorization Server
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().build();
    }
}
