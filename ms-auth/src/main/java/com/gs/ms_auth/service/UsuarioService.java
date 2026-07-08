package com.gs.ms_auth.service;

import com.gs.ms_auth.dto.RegisterRequest;
import com.gs.ms_auth.model.Usuario;
import com.gs.ms_auth.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio de gestión del ciclo de vida de usuarios del Laboratorio G&amp;S.
 * <p>
 * Encapsula la lógica de negocio para registro, aprobación y administración
 * de integrantes del laboratorio. Las contraseñas se hashean con BCrypt antes
 * de persistirse; nunca se almacenan en texto plano.
 * </p>
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registra un nuevo usuario en estado pendiente de aprobación.
     * <p>
     * El usuario se crea con {@code enabled = false} y {@code pendienteAprobacion = true}.
     * No podrá autenticarse hasta que un administrador lo apruebe con {@link #aprobar(Long)}.
     * </p>
     *
     * @param request DTO con los datos del nuevo usuario (username, password, nombre, apellido, rol)
     * @return entidad {@link Usuario} persistida, sin contraseña en texto plano
     * @throws IllegalArgumentException si el username ya está en uso en el sistema
     */
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

    /**
     * Devuelve todos los usuarios del sistema sin filtros.
     *
     * @return lista de {@link Usuario}; nunca {@code null}, puede ser vacía
     */
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    /**
     * Aprueba (activa) un usuario pendiente de aprobación.
     * <p>
     * Establece {@code enabled = true} y {@code pendienteAprobacion = false},
     * permitiéndole iniciar sesión a partir de ese momento.
     * </p>
     *
     * @param id identificador del usuario a aprobar
     * @return entidad {@link Usuario} actualizada
     * @throws RuntimeException si no existe un usuario con el {@code id} indicado
     */
    public Usuario aprobar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        usuario.setEnabled(true);
        usuario.setPendienteAprobacion(false);
        return usuarioRepository.save(usuario);
    }

    /**
     * Activa o desactiva un usuario (entrada/salida de personal del laboratorio).
     * <p>
     * Si {@code activo} es {@code false} el usuario no podrá autenticarse hasta
     * que sea re-activado. La marca {@code pendienteAprobacion} se limpia siempre,
     * ya que esta operación la realiza un administrador explícitamente.
     * </p>
     *
     * @param id     identificador del usuario
     * @param activo {@code true} para habilitar, {@code false} para deshabilitar
     * @return entidad {@link Usuario} actualizada
     * @throws RuntimeException si no existe un usuario con el {@code id} indicado
     */
    public Usuario cambiarEstado(Long id, boolean activo) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        usuario.setEnabled(activo);
        usuario.setPendienteAprobacion(false);
        return usuarioRepository.save(usuario);
    }

    /**
     * Actualiza el número de teléfono de un integrante.
     * <p>
     * El teléfono es utilizado por el bot de WhatsApp para identificar al usuario
     * que envía comprobantes (mapeo teléfono → usuario interno).
     * Si el valor recibido es {@code null} o está en blanco, se guarda {@code null}
     * (eliminando el teléfono registrado).
     * </p>
     *
     * @param id       identificador del usuario
     * @param telefono nuevo número de teléfono, o {@code null}/{@code ""} para eliminar
     * @return entidad {@link Usuario} actualizada
     * @throws RuntimeException si no existe un usuario con el {@code id} indicado
     */
    public Usuario actualizarTelefono(Long id, String telefono) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        usuario.setTelefono(telefono != null && !telefono.isBlank() ? telefono.trim() : null);
        return usuarioRepository.save(usuario);
    }

    // ── Perfil propio (self-service) ─────────────────────────────

    /** Busca al usuario por su username (el subject del JWT). */
    public Usuario buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
    }

    /**
     * Actualiza los datos propios editables del usuario (nombre, apellido,
     * teléfono). El username y el rol NO se tocan acá (eso es del admin).
     */
    public Usuario actualizarPerfil(String username, String nombre, String apellido, String telefono) {
        Usuario u = buscarPorUsername(username);
        if (nombre != null && !nombre.isBlank())   u.setNombre(nombre.trim());
        if (apellido != null && !apellido.isBlank()) u.setApellido(apellido.trim());
        u.setTelefono(telefono != null && !telefono.isBlank() ? telefono.trim() : null);
        return usuarioRepository.save(u);
    }

    /**
     * Cambia la contraseña propia. Verifica la contraseña actual antes de
     * aplicar la nueva (encriptada con BCrypt).
     *
     * @throws IllegalArgumentException si la contraseña actual no coincide.
     */
    public Usuario cambiarPassword(String username, String actual, String nueva) {
        Usuario u = buscarPorUsername(username);
        if (!passwordEncoder.matches(actual, u.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta.");
        }
        u.setPassword(passwordEncoder.encode(nueva));
        return usuarioRepository.save(u);
    }
}
