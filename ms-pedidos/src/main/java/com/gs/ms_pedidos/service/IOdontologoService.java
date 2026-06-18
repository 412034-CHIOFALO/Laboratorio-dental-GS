package com.gs.ms_pedidos.service;

import com.gs.ms_pedidos.dto.OdontologoRequest;
import com.gs.ms_pedidos.dto.OdontologoResponse;
import com.gs.ms_pedidos.exception.ResourceNotFoundException;
import com.gs.ms_pedidos.model.Odontologo;

import java.util.List;

/**
 * Contrato del servicio de negocio para la gestión de odontólogos clientes del laboratorio.
 *
 * <p>Los odontólogos son los profesionales que encargan trabajos dentales al laboratorio.
 * Se pueden registrar manualmente o de forma automática durante la carga de un pedido
 * mediante el patrón <em>find-or-create</em> ({@link #buscarOCrearPorNombre(String)}).</p>
 *
 * <p>Los odontólogos no se eliminan físicamente para preservar la integridad referencial
 * con el historial de pedidos. En su lugar se usan soft deletes mediante {@link #desactivar(Long)}.</p>
 */
public interface IOdontologoService {

    /**
     * Devuelve todos los odontólogos activos, ordenados alfabéticamente por nombre.
     *
     * @return lista de odontólogos activos; vacía si no hay ninguno
     */
    List<OdontologoResponse> listarActivos();

    /**
     * Busca odontólogos cuyo nombre contenga el fragmento indicado (búsqueda case-insensitive).
     * Diseñado para el autocomplete del formulario "Nuevo pedido".
     *
     * @param fragmento texto parcial a buscar dentro del nombre; no puede ser {@code null}
     * @return lista de odontólogos activos cuyo nombre contiene el fragmento; vacía si no hay coincidencias
     */
    List<OdontologoResponse> buscarPorNombre(String fragmento);

    /**
     * Busca un odontólogo por su identificador interno.
     *
     * @param id ID técnico del odontólogo
     * @return datos del odontólogo encontrado
     * @throws ResourceNotFoundException si no existe un odontólogo con ese ID
     */
    OdontologoResponse buscarPorId(Long id);

    /**
     * Registra un nuevo odontólogo en el sistema.
     *
     * <p>DNI y CUIT son opcionales pero únicos cuando están presentes.
     * El odontólogo queda activo por defecto ({@code activo = true}).</p>
     *
     * @param request datos del nuevo odontólogo validados por Bean Validation
     * @return odontólogo creado con su ID asignado
     * @throws com.gs.ms_pedidos.exception.ConflictException si ya existe un odontólogo con el mismo DNI o CUIT
     */
    OdontologoResponse crear(OdontologoRequest request);

    /**
     * Actualiza los datos de un odontólogo existente.
     *
     * @param id      ID del odontólogo a actualizar
     * @param request nuevos datos del odontólogo
     * @return odontólogo con los datos actualizados
     * @throws ResourceNotFoundException si no existe un odontólogo con ese ID
     * @throws com.gs.ms_pedidos.exception.ConflictException si DNI o CUIT colisionan con otro registro
     */
    OdontologoResponse actualizar(Long id, OdontologoRequest request);

    /**
     * Desactiva (soft delete) un odontólogo marcándolo como inactivo ({@code activo = false}).
     *
     * <p>No se elimina el registro para preservar las referencias en el historial
     * de pedidos. Un odontólogo desactivado no aparece en listados ni en el autocomplete.</p>
     *
     * @param id ID del odontólogo a desactivar
     * @throws ResourceNotFoundException si no existe un odontólogo con ese ID
     */
    void desactivar(Long id);

    /**
     * Busca un odontólogo por nombre exacto (case-insensitive) y, si no existe, lo crea.
     *
     * <p>Patrón <em>find-or-create</em> utilizado por el flujo "Nuevo pedido" del frontend:
     * el usuario escribe el nombre del odontólogo y el sistema lo resuelve sin salir del flujo.
     * Si se crea uno nuevo, queda sin datos de contacto (se pueden completar después).</p>
     *
     * @param nombre nombre completo del odontólogo; no puede ser {@code null} ni vacío
     * @return entidad {@link Odontologo} existente o recién creada
     */
    Odontologo buscarOCrearPorNombre(String nombre);
}
