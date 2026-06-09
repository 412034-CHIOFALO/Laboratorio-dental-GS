package com.gys.ms_pedidos.service;

import com.gys.ms_pedidos.dto.EntregaRequest;
import com.gys.ms_pedidos.dto.PedidoRequest;
import com.gys.ms_pedidos.dto.PedidoResponse;
import com.gys.ms_pedidos.exception.BusinessException;
import com.gys.ms_pedidos.exception.ResourceNotFoundException;
import com.gys.ms_pedidos.model.EstadoPedido;
import com.gys.ms_pedidos.model.Odontologo;
import com.gys.ms_pedidos.model.Pedido;
import com.gys.ms_pedidos.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PedidoService implements IPedidoService {

    private final PedidoRepository pedidoRepository;
    private final IOdontologoService odontologoService;
    private final ConsumoStockService consumoStockService;

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

        return toResponse(pedidoRepository.save(pedido));
    }

    @Override
    @Transactional
    public PedidoResponse actualizarEstado(Long id, EstadoPedido nuevoEstado) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", id));

        EstadoPedido estadoAnterior = pedido.getEstado();
        pedido.setEstado(nuevoEstado);

        // ── Descuento automático de stock al entrar en producción ──
        // Solo dispara cuando hace transición a EN_PROCESO. Idempotente (ver
        // ConsumoStockService): si ya se consumió antes, no descuenta de nuevo.
        // El fallo no rompe el cambio de estado, solo loggea.
        if (nuevoEstado == EstadoPedido.EN_PROCESO && estadoAnterior != EstadoPedido.EN_PROCESO) {
            consumoStockService.descontarSiCorresponde(pedido);
        }

        return toResponse(pedidoRepository.save(pedido));
    }

    @Override
    @Transactional
    public PedidoResponse actualizar(Long id, PedidoRequest request) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", id));

        Odontologo odontologo = resolverOdontologo(request);

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

        return toResponse(pedidoRepository.save(pedido));
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

    // ── Helper: genera NRO-YYYYMMDD-XXXX ──────────────────────────
    private String generarNroPedido() {
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = pedidoRepository.count() + 1;
        return String.format("PED-%s-%04d", fecha, count);
    }
}
