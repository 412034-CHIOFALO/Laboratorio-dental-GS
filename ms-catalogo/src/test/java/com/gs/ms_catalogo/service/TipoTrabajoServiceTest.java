package com.gs.ms_catalogo.service;

import com.gs.ms_catalogo.dto.IngredienteRecetaRequest;
import com.gs.ms_catalogo.dto.TipoTrabajoRequest;
import com.gs.ms_catalogo.dto.TipoTrabajoResponse;
import com.gs.ms_catalogo.exception.ResourceNotFoundException;
import com.gs.ms_catalogo.model.Categoria;
import com.gs.ms_catalogo.model.TipoTrabajo;
import com.gs.ms_catalogo.repository.TipoTrabajoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios del TipoTrabajoService: delegacion al repositorio, mapeo a
 * DTO, soft-delete y manejo del caso "no encontrado".
 */
@ExtendWith(MockitoExtension.class)
class TipoTrabajoServiceTest {

    @Mock
    private TipoTrabajoRepository repository;

    @InjectMocks
    private TipoTrabajoService service;

    private TipoTrabajo corona() {
        return TipoTrabajo.builder()
                .id(1L)
                .nombre("Corona de porcelana")
                .descripcion("Corona ceramica")
                .precio(new BigDecimal("15000"))
                .categoria(Categoria.FIJA)
                .tiempoEstimadoDias(7)
                .activo(true)
                .build();
    }

    private TipoTrabajoRequest request() {
        TipoTrabajoRequest req = new TipoTrabajoRequest();
        req.setNombre("Corona de porcelana");
        req.setDescripcion("Corona ceramica");
        req.setPrecio(new BigDecimal("15000"));
        req.setCategoria(Categoria.FIJA);
        req.setTiempoEstimadoDias(7);
        IngredienteRecetaRequest ing = new IngredienteRecetaRequest();
        ing.setMaterialId(10L);
        ing.setMaterialNombre("Zirconio");
        ing.setCantidad(new BigDecimal("2"));
        ing.setUnidad("gr");
        req.setReceta(List.of(ing));
        return req;
    }

    @Test
    void listarActivos_mapeaTodos() {
        when(repository.findByActivoTrue()).thenReturn(List.of(corona()));

        List<TipoTrabajoResponse> result = service.listarActivos();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).nombre()).isEqualTo("Corona de porcelana");
    }

    @Test
    void listarPorCategoria_filtraPorCategoria() {
        when(repository.findByCategoriaAndActivoTrue(Categoria.FIJA)).thenReturn(List.of(corona()));

        assertThat(service.listarPorCategoria(Categoria.FIJA)).hasSize(1);
        verify(repository).findByCategoriaAndActivoTrue(Categoria.FIJA);
    }

    @Test
    void buscarPorNombre_delegaAlRepo() {
        when(repository.findByNombreContainingIgnoreCaseAndActivoTrue("coro")).thenReturn(List.of(corona()));

        assertThat(service.buscarPorNombre("coro")).hasSize(1);
    }

    @Test
    void buscarPorId_existente_devuelveDto() {
        when(repository.findById(1L)).thenReturn(Optional.of(corona()));

        assertThat(service.buscarPorId(1L).id()).isEqualTo(1L);
    }

    @Test
    void buscarPorId_inexistente_lanza404() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crear_construyeYGuardaConReceta() {
        when(repository.save(any(TipoTrabajo.class))).thenAnswer(inv -> {
            TipoTrabajo t = inv.getArgument(0);
            t.setId(5L);
            return t;
        });

        TipoTrabajoResponse res = service.crear(request());

        ArgumentCaptor<TipoTrabajo> captor = ArgumentCaptor.forClass(TipoTrabajo.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Corona de porcelana");
        assertThat(captor.getValue().getReceta()).hasSize(1);
        assertThat(res.nombre()).isEqualTo("Corona de porcelana");
    }

    @Test
    void crear_sinReceta_noFalla() {
        TipoTrabajoRequest req = request();
        req.setReceta(null);
        when(repository.save(any(TipoTrabajo.class))).thenAnswer(inv -> inv.getArgument(0));

        service.crear(req);

        ArgumentCaptor<TipoTrabajo> captor = ArgumentCaptor.forClass(TipoTrabajo.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getReceta()).isEmpty();
    }

    @Test
    void actualizar_existente_modificaCampos() {
        when(repository.findById(1L)).thenReturn(Optional.of(corona()));
        when(repository.save(any(TipoTrabajo.class))).thenAnswer(inv -> inv.getArgument(0));

        TipoTrabajoRequest req = request();
        req.setPrecio(new BigDecimal("20000"));
        TipoTrabajoResponse res = service.actualizar(1L, req);

        assertThat(res.precio()).isEqualByComparingTo("20000");
    }

    @Test
    void actualizar_inexistente_lanza404() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(99L, request()))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void eliminar_existente_hacesoftDelete() {
        TipoTrabajo t = corona();
        when(repository.findById(1L)).thenReturn(Optional.of(t));
        when(repository.save(any(TipoTrabajo.class))).thenAnswer(inv -> inv.getArgument(0));

        service.eliminar(1L);

        assertThat(t.isActivo()).isFalse();
        verify(repository).save(t);
    }

    @Test
    void eliminar_inexistente_lanza404() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
