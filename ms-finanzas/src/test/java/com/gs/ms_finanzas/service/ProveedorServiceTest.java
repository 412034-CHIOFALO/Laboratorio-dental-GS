package com.gs.ms_finanzas.service;

import com.gs.ms_finanzas.dto.ProveedorRequest;
import com.gs.ms_finanzas.exception.BusinessException;
import com.gs.ms_finanzas.exception.ResourceNotFoundException;
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
class ProveedorServiceTest {

    @Mock private ProveedorRepository proveedorRepo;
    @Mock private DeudaProveedorRepository deudaRepo;
    @InjectMocks private ProveedorService service;

    private Proveedor proveedor() {
        return Proveedor.builder().id(1L).nombre("Dental SA").cuit("20-12345678-9").activo(true).build();
    }

    @Test
    void listarActivos_conDeuda() {
        when(proveedorRepo.findByActivoTrue()).thenReturn(List.of(proveedor()));
        when(deudaRepo.sumDeudaPendienteByProveedor(1L)).thenReturn(new BigDecimal("500"));
        assertThat(service.listarActivos()).hasSize(1);
    }

    @Test
    void buscarPorId_inexistente_404() {
        when(proveedorRepo.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.buscarPorId(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crear_cuitDuplicado_business() {
        ProveedorRequest r = new ProveedorRequest();
        r.setNombre("Dental SA"); r.setCuit("20-12345678-9");
        when(proveedorRepo.existsByCuit("20-12345678-9")).thenReturn(true);
        assertThatThrownBy(() -> service.crear(r)).isInstanceOf(BusinessException.class);
    }

    @Test
    void crear_ok() {
        ProveedorRequest r = new ProveedorRequest();
        r.setNombre("Nuevo Prov"); r.setCuit("");
        when(proveedorRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        assertThat(service.crear(r)).isNotNull();
    }

    @Test
    void actualizar_inexistente_404() {
        when(proveedorRepo.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.actualizar(9L, new ProveedorRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void actualizar_ok() {
        when(proveedorRepo.findById(1L)).thenReturn(Optional.of(proveedor()));
        when(proveedorRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        ProveedorRequest r = new ProveedorRequest();
        r.setNombre("Editado");
        assertThat(service.actualizar(1L, r)).isNotNull();
    }

    @Test
    void desactivar_ok() {
        Proveedor p = proveedor();
        when(proveedorRepo.findById(1L)).thenReturn(Optional.of(p));
        when(proveedorRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        service.desactivar(1L);
        assertThat(p.isActivo()).isFalse();
    }
}
