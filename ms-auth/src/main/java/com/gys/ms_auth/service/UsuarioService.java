package com.gys.ms_auth.service;

import com.gys.ms_auth.dto.RegisterRequest;
import com.gys.ms_auth.model.Usuario;
import com.gys.ms_auth.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario registrar(RegisterRequest request) {
        if (usuarioRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("El nombre de usuario ya está en uso.");
        }

        Usuario nuevo = Usuario.builder()
            .nombre(request.nombre())
            .apellido(request.apellido())
            .username(request.username())
            .password(passwordEncoder.encode(request.password()))
            .rol(request.rol())
            .enabled(false)
            .pendienteAprobacion(true)
            .build();

        return usuarioRepository.save(nuevo);
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public Usuario aprobar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        usuario.setEnabled(true);
        usuario.setPendienteAprobacion(false);
        return usuarioRepository.save(usuario);
    }
}
