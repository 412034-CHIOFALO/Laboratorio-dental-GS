package com.gs.ms_pedidos.service;

import com.gs.ms_pedidos.dto.EntregaRequest;
import com.gs.ms_pedidos.dto.PedidoRequest;
import com.gs.ms_pedidos.dto.PedidoResponse;
import com.gs.ms_pedidos.exception.BusinessException;
import com.gs.ms_pedidos.exception.ResourceNotFoundException;
import com.gs.ms_pedidos.model.EstadoPedido;
import com.gs.ms_pedidos.model.Odontologo;
import com.gs.ms_pedidos.model.Pedido;
import com.gs.ms_pedidos.repository.OdontologoRepository;
import com.gs.ms_pedidos.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Implementación de {@link IPedidoService} para la gestión del ciclo de vida de pedidos.
 *
 * <p>Orquesta las operaciones principales:</p>
 * <ul>
 *   <li>Creación de pedidos con numeración automática ({@code PED-YYYYMMDD-NNNN}).</li>
 *   <li>Transiciones de estado en el tablero Kanban.</li>
 *   <li>Descuento automático de stock al entrar en producción (via {@link ConsumoStockService}).</li>
 *   <li>Notificación WhatsApp al odontólogo cuando el pedido queda LISTO.</li>
 *   <li>Emisión de comprobante de deuda en ms-finanzas al marcar como ENTREGADO.</li>
 * </ul>
 *
 * <p>Todas las lecturas son {@code readOnly = true}; las escrituras tienen su propia
 * anotación {@code @Transactional}.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PedidoService implements IPedidoService {

    private final PedidoRepository pedidoRepository;
    private final OdontologoRepository odontologoRepository;
    private final IOdontologoService odontologoService;
    private final ConsumoStockService consumoStockService;
    private final NotificacionBotService notificacionBotService;
    private final EmisionComprobanteService emisionComprobanteService;
    private final AuditoriaClient auditoria;

    /**
     * Umbral de días hábiles a partir del cual un pedido se considera "atrasado".
     * Configurable vía gs.pedidos.dias-limite-atraso en application.properties.
     * Default: 6 días hábiles.
     */
    @Value("${gs.pedidos.dias-limite-atraso:6}")
    private int diasLimiteAtraso;

    /** Helper para no repetir el umbral en cada mapeo. */
    private PedidoResponse toResponse(Pedido p) {
        return PedidoResponse.from(p, diasLimiteAtraso);
    }

    @Override
    public List<PedidoResponse> listarTodos() {
        return pedidoRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<PedidoResponse> listarActivos() {
        return pedidoRepository.findByEstadoNot(EstadoPedido.LISTO)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<PedidoResponse> listarPorEstado(EstadoPedido estado) {
        return pedidoRepository.findByEstado(estado)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public PedidoResponse buscarPorId(Long id) {
        return pedidoRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", id));
    }

    @Override
    public List<PedidoResponse> listarAtrasados() {
        // Trae todos los no terminales y filtra in-memory por el umbral.
        // Para 99% de los labs el volumen es chico (<1000 activos) → fine.
        // Si en el futuro escala, agregar query con condición de fecha en SQL.
        return pedidoRepository.findAll().stream()
                .map(this::toResponse)
                .filter(PedidoResponse::atrasado)
                .toList();
    }

    @Override
    @Transactional
    public PedidoResponse crear(PedidoRequest request) {
        // ── Resolución del odontólogo: si vino con id se usa ese; si no, find-or-create ──
        Odontologo odontologo = resolverOdontologo(request);

        Pedido pedido = Pedido.builder()
                .nroPedido(generarNroPedido())
                .odontologoId(odontologo.getId())
                .odontologoNombre(odontologo.getNombre())
                .paciente(request.getPaciente())
                .catalogoTrabajoId(request.getCatalogoTrabajoId())
                .trabajo(request.getTrabajo())
                .tecnicoId(request.getTecnicoId())
                .tecnicoNombre(request.getTecnicoNombre())
                .fechaEntrega(request.getFechaEntrega())
                .prioridad(request.getPrioridad())
                .precioAcordado(request.getPrecioAcordado())
                .observaciones(request.getObservaciones())
                .build();

        Pedido guardado = pedidoRepository.save(pedido);
        auditoria.registrar("CREAR", "Pedido creado", "Pedido " + guardado.getNroPedido(),
                "Odontólogo " + guardado.getOdontologoNombre() + " · " + guardado.getTrabajo());
        return toResponse(guardado);
    }

    @Override
    @Transactional
    public PedidoResponse actualizarEstado(Long id, EstadoPedido nuevoEstado) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", id));

        EstadoPedido estadoAnterior = pedido.getEstado();
        pedido.setEstado(nuevoEstado);

        // ── Descuento automático de stock al entrar en producción ──
        // El Kanban permite arrastrar una tarjeta a cualquier columna, no solo
        // a la adyacente (ej: RECIBIDO → LISTO de un solo salto), así que no
        // alcanza con comparar contra el valor exacto EN_PROCESO.
        if (nuevoEstado.alcanzoProduccion() && !estadoAnterior.alcanzoProduccion()) {
            consumoStockService.descontarSiCorresponde(pedido);
        }

        Pedido guardado = pedidoRepository.save(pedido);

        // ── Notificación WhatsApp al odontólogo cuando el pedido está LISTO ──
        // Fire-and-forget: el fallo no rompe el cambio de estado.
        if (nuevoEstado == EstadoPedido.LISTO && estadoAnterior != EstadoPedido.LISTO) {
            odontologoRepository.findById(pedido.getOdontologoId()).ifPresent(od ->
                notificacionBotService.notificarPedidoListo(guardado.getNroPedido(), guardado.getTrabajo(), od)
            );
        }

        auditoria.registrar("ESTADO", "Cambio de estado de pedido", "Pedido " + guardado.getNroPedido(),
                estadoAnterior + " → " + nuevoEstado);
        return toResponse(guardado);
    }

    @Override
    @Transactional
    public PedidoResponse actualizar(Long id, PedidoRequest request) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", id));

        Odontologo odontologo = resolverOdontologo(request);

        // Si el pedido ya tiene comprobante emitido (se entregó) y el precio
        // cambia acá, hay que avisarle a ms-finanzas para que el saldo pendiente
        // del odontólogo refleje el monto corregido — ver sincronizarMontoSiCorresponde.
        boolean precioCambio = pedido.isComprobanteGenerado()
                && request.getPrecioAcordado() != null
                && request.getPrecioAcordado().compareTo(
                        pedido.getPrecioAcordado() != null ? pedido.getPrecioAcordado() : java.math.BigDecimal.ZERO) != 0;

        pedido.setOdontologoId(odontologo.getId());
        pedido.setOdontologoNombre(odontologo.getNombre());
        pedido.setPaciente(request.getPaciente());
        pedido.setCatalogoTrabajoId(request.getCatalogoTrabajoId());
        pedido.setTrabajo(request.getTrabajo());
        pedido.setTecnicoId(request.getTecnicoId());
        pedido.setTecnicoNombre(request.getTecnicoNombre());
        pedido.setFechaEntrega(request.getFechaEntrega());
        pedido.setPrioridad(request.getPrioridad());
        pedido.setPrecioAcordado(request.getPrecioAcordado());
        pedido.setObservaciones(request.getObservaciones());

        if (precioCambio) {
            emisionComprobanteService.sincronizarMontoSiCorresponde(pedido, request.getPrecioAcordado());
        }

        return toResponse(pedidoRepository.save(pedido));
    }

    @Override
    @Transactional
    public PedidoResponse marcarEntregado(Long id, EntregaRequest request) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", id));

        if (pedido.getEstado() != EstadoPedido.LISTO) {
            throw new BusinessException(
                "Solo se pueden entregar pedidos en estado LISTO. Estado actual: " + pedido.getEstado());
        }

        pedido.setEstado(EstadoPedido.ENTREGADO);
        pedido.setFechaEntregaReal(
            request.getFechaEntregaReal() != null ? request.getFechaEntregaReal() : LocalDate.now());
        pedido.setRetiradoPor(request.getRetiradoPor().trim());
        pedido.setObservacionesEntrega(
            request.getObservacionesEntrega() != null && !request.getObservacionesEntrega().isBlank()
                ? request.getObservacionesEntrega().trim()
                : null);

        // Generar la cuenta por cobrar en finanzas (la entrega crea la DEUDA, no el
        // cobro). Monto: el del request o, si no vino, el precio acordado del pedido.
        java.math.BigDecimal monto = request.getMonto() != null
            ? request.getMonto()
            : pedido.getPrecioAcordado();
        emisionComprobanteService.emitirSiCorresponde(pedido, monto);

        Pedido guardado = pedidoRepository.save(pedido);
        auditoria.registrar("ENTREGA", "Pedido entregado", "Pedido " + guardado.getNroPedido(),
                "Retiró: " + guardado.getRetiradoPor() + " · facturado $" + (monto != null ? monto : "—"));
        return toResponse(guardado);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        if (!pedidoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Pedido", id);
        }
        pedidoRepository.deleteById(id);
    }

    /**
     * Si el request trae odontologoId → se busca por id (debe existir).
     * Si no → se busca por nombre, y si no existe se crea.
     */
    private Odontologo resolverOdontologo(PedidoRequest request) {
        if (request.getOdontologoId() != null) {
            var dto = odontologoService.buscarPorId(request.getOdontologoId());
            return Odontologo.builder()
                    .id(dto.id())
                    .nombre(dto.nombre())
                    .build();
        }
        return odontologoService.buscarOCrearPorNombre(request.getOdontologoNombre());
    }

    /**
     * Genera PED-yyyyMMdd-XXXX siguiendo al último número DEL DÍA, no contando
     * filas de toda la tabla. {@code count()+1} se rompe apenas se borra un
     * pedido — el contador retrocede y regenera un número ya usado. Como
     * nroPedido es UNIQUE, esa inserción tira una excepción sin capturar acá:
     * crear() queda roto hasta que se corrija a mano. Mismo bug (y mismo fix)
     * que tuvo el número de comprobante en ms-finanzas.
     */
    private String generarNroPedido() {
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefijo = String.format("PED-%s-", fecha);
        String ultimo = pedidoRepository.maxNroPedidoConPrefijo(prefijo);

        long siguiente = 1;
        if (ultimo != null && ultimo.length() > prefijo.length()) {
            try {
                siguiente = Long.parseLong(ultimo.substring(prefijo.length())) + 1;
            } catch (NumberFormatException e) {
                siguiente = pedidoRepository.count() + 1;
            }
        }
        return String.format("%s%04d", prefijo, siguiente);
    }
}
