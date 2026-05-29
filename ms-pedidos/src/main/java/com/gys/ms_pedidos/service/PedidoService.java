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

    @Override
    public List<PedidoResponse> listarTodos() {
        return pedidoRepository.findAll()
                .stream()
                .map(PedidoResponse::from)
                .toList();
    }

    @Override
    public List<PedidoResponse> listarActivos() {
        return pedidoRepository.findByEstadoNot(EstadoPedido.LISTO)
                .stream()
                .map(PedidoResponse::from)
                .toList();
    }

    @Override
    public List<PedidoResponse> listarPorEstado(EstadoPedido estado) {
        return pedidoRepository.findByEstado(estado)
                .stream()
                .map(PedidoResponse::from)
                .toList();
    }

    @Override
    public PedidoResponse buscarPorId(Long id) {
        return pedidoRepository.findById(id)
                .map(PedidoResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", id));
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

        return PedidoResponse.from(pedidoRepository.save(pedido));
    }

    @Override
    @Transactional
    public PedidoResponse actualizarEstado(Long id, EstadoPedido nuevoEstado) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", id));
        pedido.setEstado(nuevoEstado);
        return PedidoResponse.from(pedidoRepository.save(pedido));
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

        return PedidoResponse.from(pedidoRepository.save(pedido));
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

        return PedidoResponse.from(pedidoRepository.save(pedido));
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
