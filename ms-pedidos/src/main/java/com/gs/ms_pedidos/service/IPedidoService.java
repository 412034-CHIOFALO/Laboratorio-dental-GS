package com.gs.ms_pedidos.service;

import com.gs.ms_pedidos.dto.EntregaRequest;
import com.gs.ms_pedidos.dto.PedidoRequest;
import com.gs.ms_pedidos.dto.PedidoResponse;
import com.gs.ms_pedidos.exception.ResourceNotFoundException;
import com.gs.ms_pedidos.model.EstadoPedido;

import java.util.List;

/**
 * Contrato del servicio de negocio para la gestión de pedidos del laboratorio dental.
 *
 * <p>Define las operaciones del ciclo de vida completo de un pedido:
 * desde su creación hasta su entrega o cancelación, incluyendo
 * la transición de estados y el consumo automático de stock de materiales.</p>
 */
public interface IPedidoService {

    /**
     * Devuelve todos los pedidos registrados, independientemente de su estado.
     *
     * @return lista completa de pedidos; vacía si no hay registros
     */
    List<PedidoResponse> listarTodos();

    /**
     * Devuelve los pedidos que se encuentran en producción activa
     * (estados distintos de {@link EstadoPedido#ENTREGADO} y {@link EstadoPedido#CANCELADO}).
     *
     * @return lista de pedidos activos; vacía si no hay ninguno
     */
    List<PedidoResponse> listarActivos();

    /**
     * Filtra los pedidos por un estado específico.
     *
     * @param estado el estado por el que se filtra; no puede ser {@code null}
     * @return lista de pedidos en ese estado; vacía si no hay ninguno
     */
    List<PedidoResponse> listarPorEstado(EstadoPedido estado);

    /**
     * Busca un pedido por su identificador interno.
     *
     * @param id ID técnico del pedido
     * @return datos completos del pedido encontrado
     * @throws ResourceNotFoundException si no existe un pedido con ese ID
     */
    PedidoResponse buscarPorId(Long id);

    /**
     * Registra un nuevo pedido de trabajo dental.
     *
     * <p>Durante la creación se:
     * <ul>
     *   <li>Genera el número de pedido ({@code nroPedido}) de forma automática.</li>
     *   <li>Calcula la fecha de entrega estimada en días hábiles.</li>
     *   <li>Aplica el estado inicial {@link EstadoPedido#RECIBIDO}.</li>
     *   <li>Busca o crea el odontólogo por nombre (patrón find-or-create).</li>
     * </ul>
     *
     * @param request datos del nuevo pedido validados por Bean Validation
     * @return pedido creado con todos sus datos calculados y persistidos
     * @throws com.gs.ms_pedidos.exception.ConflictException si hay un conflicto de unicidad
     */
    PedidoResponse crear(PedidoRequest request);

    /**
     * Actualiza los datos editables de un pedido existente.
     *
     * <p>No modifica el estado actual ni el número de pedido ({@code nroPedido}).
     * Campos actualizables: odontólogo, paciente, trabajo, técnico asignado,
     * fecha de entrega, precio acordado y observaciones.</p>
     *
     * @param id      ID del pedido a actualizar
     * @param request nuevos datos del pedido
     * @return pedido actualizado
     * @throws ResourceNotFoundException si no existe un pedido con ese ID
     */
    PedidoResponse actualizar(Long id, PedidoRequest request);

    /**
     * Cambia el estado de un pedido (operación del tablero kanban).
     *
     * <p>Al transitar a {@link EstadoPedido#EN_PROCESO} se dispara el
     * consumo automático de stock de materiales (solo si no fue consumido
     * previamente — ver {@code Pedido#stockConsumido}).</p>
     *
     * @param id          ID del pedido
     * @param nuevoEstado estado destino de la transición
     * @return pedido con el estado actualizado
     * @throws ResourceNotFoundException si no existe un pedido con ese ID
     * @throws com.gs.ms_pedidos.exception.BusinessException si la transición es inválida
     */
    PedidoResponse actualizarEstado(Long id, EstadoPedido nuevoEstado);

    /**
     * Transición {@link EstadoPedido#LISTO} → {@link EstadoPedido#ENTREGADO}.
     *
     * <p>Registra la fecha real de entrega, quién retiró el trabajo y
     * observaciones de la entrega. Solo puede aplicarse si el pedido
     * se encuentra actualmente en estado {@link EstadoPedido#LISTO}.</p>
     *
     * @param id      ID del pedido a marcar como entregado
     * @param request datos de la entrega (quién retiró, observaciones)
     * @return pedido con estado ENTREGADO y datos de entrega persistidos
     * @throws ResourceNotFoundException si no existe un pedido con ese ID
     * @throws com.gs.ms_pedidos.exception.BusinessException si el pedido no está en estado LISTO
     */
    PedidoResponse marcarEntregado(Long id, EntregaRequest request);

    /**
     * Elimina definitivamente un pedido de la base de datos.
     *
     * <p>Operación irreversible. Se recomienda cancelar en vez de eliminar
     * para preservar el historial de producción.</p>
     *
     * @param id ID del pedido a eliminar
     * @throws ResourceNotFoundException si no existe un pedido con ese ID
     */
    void eliminar(Long id);

    /**
     * Lista todos los pedidos considerados atrasados según la política del laboratorio.
     *
     * <p>Un pedido está atrasado cuando lleva más días hábiles sin entregarse
     * que el umbral configurado en {@code pedidos.umbral-dias-atraso}.
     * Se excluyen los pedidos en estado {@link EstadoPedido#ENTREGADO}
     * y {@link EstadoPedido#CANCELADO}.</p>
     *
     * @return lista de pedidos atrasados; vacía si todos están al día
     */
    List<PedidoResponse> listarAtrasados();
}
