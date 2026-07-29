package com.gs.ms_auth.config;

import com.gs.ms_auth.service.CustomUserDetailsService;
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
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    // Keystore PKCS12 persistido en classpath:keys/gs-auth.p12
    // La contraseña se inyecta desde env var GS_KEYSTORE_PASSWORD (con default de desarrollo)
    @Value("${gs.auth.keystore.path:classpath:keys/gs-auth.p12}")
    private Resource keystorePath;

    @Value("${gs.auth.keystore.password:gs_keystore_2025}")
    private String keystorePassword;

    @Value("${gs.auth.keystore.alias:gs-auth}")
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
            // CORS lo maneja unicamente el gateway (unico punto de entrada del browser).
            .cors(cors -> cors.disable())
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // H2 Console solo en desarrollo — requiere autenticación básica
                .requestMatchers("/h2-console/**").hasRole("ADMIN")
                // Auditoría — exclusiva de ADMIN (ADMINISTRATIVO ve todo lo demás, esto no)
                .requestMatchers("/api/auth/auditoria").hasRole("ADMIN")
                // Backup manual (botón "hacer backup ahora") — exclusivo de ADMIN
                .requestMatchers("/api/auth/backup/**").hasRole("ADMIN")
                // Crear usuarios — exclusivo de ADMIN. Separación de poderes a propósito:
                // quien crea la cuenta no puede ser quien la activa (ver /usuarios/{id}/aprobar
                // y .../estado más abajo, exclusivos de ADMINISTRATIVO), así ninguno de los
                // dos roles completa el alta de un usuario nuevo por sí solo.
                .requestMatchers("/api/auth/register").hasRole("ADMIN")
                // Listar/editar usuarios (incluye aprobar/activar) — ADMIN y ADMINISTRATIVO.
                // La asimetría "quién puede dar de alta" (ADMINISTRATIVO sí, ADMIN no) se
                // controla dentro de AuthController.cambiarEstado/aprobar, no acá.
                .requestMatchers("/api/auth/usuarios/**").hasAnyRole("ADMIN", "ADMINISTRATIVO")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
            // Necesario para que H2 console cargue en iframe (solo dev)
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }

    /**
     * Mapea el claim "roles" del JWT (emitido por este mismo ms-auth) a authorities.
     * El claim llega como CSV: "ROLE_ADMIN" o "ROLE_TECNICO". Sin este converter,
     * el mapeo por defecto usa el claim "scope"/"scp" y hasRole("ADMIN") daría 403
     * en /api/auth/usuarios, /auditoria y /register. Mismo patrón que el resto de MS.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String roles = jwt.getClaimAsString("roles");
            if (roles == null || roles.isBlank()) return Collections.emptyList();
            return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(r -> !r.isEmpty())
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority(r))
                .collect(Collectors.toList());
        });
        return converter;
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

    /**
     * 11. RegisteredClientRepository
     *
     * Requerido por OAuth2AuthorizationServerConfiguration aunque el login
     * principal se haga vía /api/auth/login (flow custom con username/password).
     *
     * Este client en memoria queda disponible para apps que quieran usar el
     * flow OAuth2 estándar (authorization_code + PKCE) hacia este Auth Server.
     * Si en algún momento migramos el frontend al flow OIDC, ya está listo.
     *
     * El client_id es "gs-frontend" y soporta:
     *  - authorization_code  (con PKCE para SPAs)
     *  - refresh_token
     *  - scopes: openid, profile
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository(PasswordEncoder passwordEncoder) {
        RegisteredClient frontendClient = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("gs-frontend")
            .clientSecret(passwordEncoder.encode("gs-frontend-secret"))
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE) // public client (SPA con PKCE)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUri("http://localhost:4200/login/oauth2/code/gs")
            .redirectUri("http://localhost/login/oauth2/code/gs")
            .scope(OidcScopes.OPENID)
            .scope(OidcScopes.PROFILE)
            .clientSettings(ClientSettings.builder()
                .requireAuthorizationConsent(false)
                .requireProofKey(true) // PKCE obligatorio para SPAs
                .build())
            .build();
        return new InMemoryRegisteredClientRepository(frontendClient);
    }
}
