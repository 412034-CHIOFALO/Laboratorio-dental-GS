package com.gs.ms_finanzas.service;

import com.gs.ms_finanzas.dto.DeudaProveedorRequest;
import com.gs.ms_finanzas.exception.BusinessException;
import com.gs.ms_finanzas.exception.ResourceNotFoundException;
import com.gs.ms_finanzas.model.DeudaProveedor;
import com.gs.ms_finanzas.model.EstadoDeuda;
import com.gs.ms_finanzas.model.Proveedor;
import com.gs.ms_finanzas.repository.DeudaProveedorRepository;
import com.gs.ms_finanzas.repository.ProveedorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeudaProveedorServiceTest {

    @Mock private DeudaProveedorRepository deudaRepo;
    @Mock private ProveedorRepository proveedorRepo;
    @InjectMocks private DeudaProveedorService service;

    private Proveedor proveedor() {
        return Proveedor.builder().id(1L).nombre("Dental SA").activo(true).build();
    }

    private DeudaProveedor deuda() {
        return DeudaProveedor.builder().id(1L).proveedor(proveedor())
                .descripcion("Insumos").monto(new BigDecimal("500"))
                .estado(EstadoDeuda.PENDIENTE).build();
    }

    @Test
    void listarPorProveedor_mapea() {
        when(deudaRepo.findByProveedorIdOrderByFechaCreacionDesc(1L)).thenReturn(List.of(deuda()));
        assertThat(service.listarPorProveedor(1L)).hasSize(1);
    }

    @Test
    void listarPendientes_mapea() {
        when(deudaRepo.findByEstadoOrderByFechaVencimientoAsc(EstadoDeuda.PENDIENTE)).thenReturn(List.of(deuda()));
        assertThat(service.listarPendientes()).hasSize(1);
    }

    @Test
    void buscarPorId_inexistente_404() {
        when(deudaRepo.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.buscarPorId(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void registrar_proveedorInexistente_404() {
        DeudaProveedorRequest r = new DeudaProveedorRequest();
        r.setProveedorId(9L);
        r.setMonto(new BigDecimal("100"));
        when(proveedorRepo.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.registrar(r)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void registrar_ok() {
        DeudaProveedorRequest r = new DeudaProveedorRequest();
        r.setProveedorId(1L);
        r.setDescripcion("Insumos");
        r.setMonto(new BigDecimal("500"));
        when(proveedorRepo.findById(1L)).thenReturn(Optional.of(proveedor()));
        when(deudaRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        assertThat(service.registrar(r)).isNotNull();
    }

    @Test
    void pagar_inexistente_404() {
        when(deudaRepo.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.pagar(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void pagar_yaPagada_business() {
        DeudaProveedor pagada = deuda();
        pagada.setEstado(EstadoDeuda.PAGADO);
        when(deudaRepo.findById(1L)).thenReturn(Optional.of(pagada));
        assertThatThrownBy(() -> service.pagar(1L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void pagar_ok() {
        when(deudaRepo.findById(1L)).thenReturn(Optional.of(deuda()));
        when(deudaRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        assertThat(service.pagar(1L).estado()).isEqualTo(EstadoDeuda.PAGADO);
    }
}
