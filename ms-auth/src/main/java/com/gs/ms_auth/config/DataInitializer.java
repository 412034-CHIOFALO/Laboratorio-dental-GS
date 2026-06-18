package com.gs.ms_auth.config;

import com.gs.ms_auth.model.Rol;
import com.gs.ms_auth.model.Usuario;
import com.gs.ms_auth.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    // Contraseñas iniciales inyectadas desde variables de entorno.
    // En desarrollo, los defaults son sencillos para arrancar sin configurar nada.
    // En producción, SIEMPRE override vía env vars GS_ADMIN_PASSWORD / GS_TECNICO_PASSWORD.
    @Value("${GS_ADMIN_PASSWORD:${GS_ADMIN_PASSWORD:admin123}}")
    private String adminPassword;

    @Value("${GS_TECNICO_PASSWORD:${GS_TECNICO_PASSWORD:tecnico123}}")
    private String tecnicoPassword;

    public DataInitializer(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!usuarioRepository.existsByUsername("admin")) {
            if ("admin123".equals(adminPassword)) {
                log.warn("[GS-SECURITY] Usando contraseña por defecto para 'admin'. " +
                         "CAMBIAR antes de producción vía GS_ADMIN_PASSWORD.");
            }
            Usuario admin = Usuario.builder()
                .nombre("Rebeca")
                .apellido("González")
                .username("admin")
                .password(passwordEncoder.encode(adminPassword))
                .rol(Rol.ADMIN)
                .enabled(true)
                .pendienteAprobacion(false)
                .build();
            usuarioRepository.save(admin);
            log.info("[GS] Usuario 'admin' creado correctamente.");
        }

        if (!usuarioRepository.existsByUsername("tecnico1")) {
            if ("tecnico123".equals(tecnicoPassword)) {
                log.warn("[GS-SECURITY] Usando contraseña por defecto para 'tecnico1'. " +
                         "CAMBIAR antes de producción vía GS_TECNICO_PASSWORD.");
            }
            Usuario tecnico = Usuario.builder()
                .nombre("Carlos")
                .apellido("López")
                .username("tecnico1")
                .password(passwordEncoder.encode(tecnicoPassword))
                .rol(Rol.TECNICO)
                .enabled(true)
                .pendienteAprobacion(false)
                .build();
            usuarioRepository.save(tecnico);
            log.info("[GS] Usuario 'tecnico1' creado correctamente.");
        }
    }
}
