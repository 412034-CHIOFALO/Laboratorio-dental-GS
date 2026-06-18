package com.gs.ms_auth.service;

import com.gs.ms_auth.model.AuditoriaEvento;
import com.gs.ms_auth.repository.AuditoriaEventoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio de bitácora de auditoría del Laboratorio G&amp;S.
 * <p>
 * Persiste eventos inmutables en la tabla {@code auditoria_eventos}.
 * Todos los microservicios del sistema deben registrar sus acciones relevantes
 * a través de este servicio para garantizar trazabilidad completa.
 * </p>
 */
@Service
public class AuditoriaService {

    private final AuditoriaEventoRepository repo;

    public AuditoriaService(AuditoriaEventoRepository repo) {
        this.repo = repo;
    }

    /**
     * Registra un nuevo evento en la bitácora de auditoría.
     * <p>
     * El timestamp se establece automáticamente a {@code Instant.now()} en la entidad.
     * Si {@code detalle} es {@code null}, se persiste como cadena vacía.
     * </p>
     *
     * @param usuario username del actor que generó el evento (puede ser un username del sistema en eventos automáticos)
     * @param tipo    categoría del evento: {@code LOGIN}, {@code CREAR}, {@code EDITAR}, {@code ELIMINAR}, {@code PAGO}, {@code ESTADO}
     * @param accion  descripción breve de la operación (ej: "Inicio de sesión", "Activación de usuario")
     * @param entidad nombre de la entidad afectada (ej: "Usuario jperez", "Pago #123")
     * @param detalle información adicional de contexto; puede ser {@code null}
     */
    public void registrar(String usuario, String tipo, String accion, String entidad, String detalle) {
        repo.save(new AuditoriaEvento(usuario, tipo, accion, entidad, detalle));
    }

    /**
     * Devuelve todos los eventos de auditoría ordenados por timestamp descendente (más reciente primero).
     *
     * @return lista inmutable de {@link AuditoriaEvento}; nunca {@code null}, puede ser vacía
     */
    public List<AuditoriaEvento> listarTodos() {
        return repo.findAllByOrderByTimestampDesc();
    }
}
