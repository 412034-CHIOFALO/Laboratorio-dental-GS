package com.gys.ms_pedidos.service;

import com.gys.ms_pedidos.dto.PedidoRequest;
import com.gys.ms_pedidos.dto.PedidoResponse;
import com.gys.ms_pedidos.exception.ResourceNotFoundException;
import com.gys.ms_pedidos.model.EstadoPedido;
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

    public List<PedidoResponse> listarTodos() {
        return pedidoRepository.findAll()
                .stream()
                .map(PedidoResponse::from)
                .toList();
    }

    public List<PedidoResponse> listarActivos() {
        return pedidoRepository.findByEstadoNot(EstadoPedido.LISTO)
                .stream()
                .map(PedidoResponse::from)
                .toList();
    }

    public List<PedidoResponse> listarPorEstado(EstadoPedido estado) {
        return pedidoRepository.findByEstado(estado)
                .stream()
                .map(PedidoResponse::from)
                .toList();
    }

    public PedidoResponse buscarPorId(Long id) {
        return pedidoRepository.findById(id)
                .map(PedidoResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", id));
    }

    @Override
    @Transactional
    public PedidoResponse crear(PedidoRequest request) {
        Pedido pedido = Pedido.builder()
                .nroPedido(generarNroPedido())
                .odontologoId(request.getOdontologoId())
                .odontologoNombre(request.getOdontologoNombre())
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

        pedido.setOdontologoId(request.getOdontologoId());
        pedido.setOdontologoNombre(request.getOdontologoNombre());
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
    public void eliminar(Long id) {
        if (!pedidoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Pedido", id);
        }
        pedidoRepository.deleteById(id);
    }

    // ── Helper: genera NRO-YYYYMMDD-XXXX ──────────────────────────
    private String generarNroPedido() {
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = pedidoRepository.count() + 1;
        return String.format("PED-%s-%04d", fecha, count);
    }
}
