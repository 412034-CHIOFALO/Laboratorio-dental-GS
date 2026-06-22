package com.gs.ms_pedidos.service;

import com.gs.ms_pedidos.dto.OdontologoRequest;
import com.gs.ms_pedidos.dto.OdontologoResponse;
import com.gs.ms_pedidos.exception.BusinessException;
import com.gs.ms_pedidos.exception.ConflictException;
import com.gs.ms_pedidos.exception.ResourceNotFoundException;
import com.gs.ms_pedidos.model.Odontologo;
import com.gs.ms_pedidos.repository.OdontologoRepository;
import com.gs.ms_pedidos.repository.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OdontologoServiceTest {

    @Mock private OdontologoRepository repository;
    @Mock private PedidoRepository pedidoRepository;
    @InjectMocks private OdontologoService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "mesesInactividad", 6);
    }

    private Odontologo odo() {
        return Odontologo.builder().id(1L).nombre("Garcia").activo(true).build();
    }

    private OdontologoRequest req(String nombre) {
        OdontologoRequest r = new OdontologoRequest();
        r.setNombre(nombre);
        return r;
    }

    @Test
    void listarActivos_calculaInactividad() {
        when(pedidoRepository.ultimaActividadPorOdontologo()).thenReturn(List.of());
        when(repository.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of(odo()));

        List<OdontologoResponse> res = service.listarActivos();

        assertThat(res).hasSize(1);
    }

    @Test
    void buscar_blank_listaActivos() {
        when(pedidoRepository.ultimaActividadPorOdontologo()).thenReturn(List.of());
        when(repository.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of());

        assertThat(service.buscarPorNombre("   ")).isEmpty();
    }

    @Test
    void buscar_porDni_usaMatchExacto() {
        when(repository.findByActivoTrueAndDni("12345678")).thenReturn(Optional.of(odo()));

        assertThat(service.buscarPorNombre("12345678")).hasSize(1);
        verify(repository).findByActivoTrueAndDni("12345678");
    }

    @Test
    void buscar_porCuit_normalizaYbusca() {
        when(repository.findByActivoTrueAndCuit("20-12345678-9")).thenReturn(Optional.of(odo()));

        assertThat(service.buscarPorNombre("20123456789")).hasSize(1);
        verify(repository).findByActivoTrueAndCuit("20-12345678-9");
    }

    @Test
    void buscar_porMatricula_matchExacto() {
        when(repository.findByActivoTrueAndMatriculaIgnoreCase("MN 123")).thenReturn(Optional.of(odo()));

        assertThat(service.buscarPorNombre("MN 123")).hasSize(1);
    }

    @Test
    void buscar_porNombre_fragmento() {
        when(repository.findByActivoTrueAndNombreContainingIgnoreCaseOrderByNombreAsc("gar"))
                .thenReturn(List.of(odo()));

        assertThat(service.buscarPorNombre("gar")).hasSize(1);
    }

    @Test
    void buscarPorId_inexistente_404() {
        when(repository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.buscarPorId(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crear_ok_normalizaCuit() {
        when(repository.findByActivoTrueAndNombreIgnoreCase(any())).thenReturn(Optional.empty());
        when(repository.existsByDni(any())).thenReturn(false);
        when(repository.existsByCuit(any())).thenReturn(false);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        OdontologoRequest r = req("  Dr.  Garcia  ");
        r.setDni("12345678");
        r.setCuit("20123456789");
        service.crear(r);

        ArgumentCaptor<Odontologo> cap = ArgumentCaptor.forClass(Odontologo.class);
        verify(repository).save(cap.capture());
        assertThat(cap.getValue().getNombre()).isEqualTo("Dr. Garcia");      // normalizado
        assertThat(cap.getValue().getCuit()).isEqualTo("20-12345678-9");     // normalizado
    }

    @Test
    void crear_nombreDuplicado_conflict() {
        when(repository.findByActivoTrueAndNombreIgnoreCase(any())).thenReturn(Optional.of(odo()));

        assertThatThrownBy(() -> service.crear(req("Garcia")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void crear_dniDuplicado_conflict() {
        when(repository.findByActivoTrueAndNombreIgnoreCase(any())).thenReturn(Optional.empty());
        when(repository.existsByDni("12345678")).thenReturn(true);

        OdontologoRequest r = req("Garcia");
        r.setDni("12345678");
        assertThatThrownBy(() -> service.crear(r)).isInstanceOf(ConflictException.class);
    }

    @Test
    void actualizar_inexistente_404() {
        when(repository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.actualizar(9L, req("X")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void actualizar_cambiaDniAOtroExistente_conflict() {
        Odontologo o = odo();
        o.setDni("111");
        when(repository.findById(1L)).thenReturn(Optional.of(o));
        when(repository.existsByDni("222")).thenReturn(true);

        OdontologoRequest r = req("Garcia");
        r.setDni("222");
        assertThatThrownBy(() -> service.actualizar(1L, r)).isInstanceOf(ConflictException.class);
    }

    @Test
    void desactivar_ok() {
        Odontologo o = odo();
        when(repository.findById(1L)).thenReturn(Optional.of(o));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.desactivar(1L);

        assertThat(o.getActivo()).isFalse();
    }

    @Test
    void buscarOCrear_blank_lanzaBusiness() {
        assertThatThrownBy(() -> service.buscarOCrearPorNombre("  "))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void buscarOCrear_existente_devuelve() {
        when(repository.findByActivoTrueAndNombreIgnoreCase("Garcia")).thenReturn(Optional.of(odo()));

        Odontologo r = service.buscarOCrearPorNombre("Garcia");

        assertThat(r.getNombre()).isEqualTo("Garcia");
        verify(repository, never()).save(any());
    }

    @Test
    void buscarOCrear_inexistente_creaOnTheFly() {
        when(repository.findByActivoTrueAndNombreIgnoreCase("Nuevo")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        Odontologo r = service.buscarOCrearPorNombre("Nuevo");

        assertThat(r.getNombre()).isEqualTo("Nuevo");
        verify(repository).save(any());
    }
}
