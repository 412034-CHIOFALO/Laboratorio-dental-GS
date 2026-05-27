package com.gys.ms_auth.config;

import com.gys.ms_auth.model.Rol;
import com.gys.ms_auth.model.Usuario;
import com.gys.ms_auth.repository.UsuarioRepository;
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
    // NUNCA usar defaults fijos en producción — configurar GYS_ADMIN_PASSWORD y GYS_TECNICO_PASSWORD.
    @Value("${GYS_ADMIN_PASSWORD:CHANGE_ME_ADMIN}")
    private String adminPassword;

    @Value("${GYS_TECNICO_PASSWORD:CHANGE_ME_TECNICO}")
    private String tecnicoPassword;

    public DataInitializer(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!usuarioRepository.existsByUsername("admin")) {
            if ("CHANGE_ME_ADMIN".equals(adminPassword)) {
                log.warn("[GYS-SECURITY] La variable GYS_ADMIN_PASSWORD no está configurada. " +
                         "Usando contraseña por defecto — CAMBIAR ANTES DE PRODUCCIÓN.");
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
            log.info("[GYS] Usuario 'admin' creado correctamente.");
        }

        if (!usuarioRepository.existsByUsername("tecnico1")) {
            if ("CHANGE_ME_TECNICO".equals(tecnicoPassword)) {
                log.warn("[GYS-SECURITY] La variable GYS_TECNICO_PASSWORD no está configurada. " +
                         "Usando contraseña por defecto — CAMBIAR ANTES DE PRODUCCIÓN.");
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
            log.info("[GYS] Usuario 'tecnico1' creado correctamente.");
        }
    }
}
