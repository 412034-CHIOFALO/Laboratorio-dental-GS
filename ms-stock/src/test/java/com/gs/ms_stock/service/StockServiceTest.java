package com.gs.ms_stock.service;

import com.gs.ms_stock.dto.MaterialRequest;
import com.gs.ms_stock.dto.MaterialResponse;
import com.gs.ms_stock.dto.MovimientoRequest;
import com.gs.ms_stock.exception.ResourceNotFoundException;
import com.gs.ms_stock.model.CategoriaMaterial;
import com.gs.ms_stock.model.Material;
import com.gs.ms_stock.model.MovimientoStock;
import com.gs.ms_stock.model.TipoMovimiento;
import com.gs.ms_stock.repository.MaterialRepository;
import com.gs.ms_stock.repository.MovimientoStockRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock private MaterialRepository materialRepo;
    @Mock private MovimientoStockRepository movimientoRepo;

    @InjectMocks private StockService service;

    private Material material(double actual, Double minimo) {
        return Material.builder()
                .id(1L).nombre("Zirconio").categoria(CategoriaMaterial.CERAMICA)
                .stockActual(actual).stockMinimo(minimo).unidadMedida("gr")
                .precioUnitario(new BigDecimal("100")).activo(true).descuentaStock(true)
                .build();
    }

    private MovimientoRequest mov(TipoMovimiento tipo, double cantidad) {
        MovimientoRequest r = new MovimientoRequest();
        r.setMaterialId(1L);
        r.setTipo(tipo);
        r.setCantidad(cantidad);
        r.setMotivo("test");
        return r;
    }

    @Test
    void listarActivos_mapea() {
        when(materialRepo.findByActivoTrue()).thenReturn(List.of(material(10, 2.0)));
        assertThat(service.listarActivos()).hasSize(1);
    }

    @Test
    void listarBajoStock_mapea() {
        when(materialRepo.findBajoStock()).thenReturn(List.of(material(1, 5.0)));
        assertThat(service.listarBajoStock()).hasSize(1);
    }

    @Test
    void buscarPorId_inexistente_lanza404() {
        when(materialRepo.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.buscarPorId(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crear_default_descuentaStockTrue() {
        MaterialRequest req = new MaterialRequest();
        req.setNombre("Yeso"); req.setCategoria(CategoriaMaterial.YESO);
        req.setStockActual(50.0); req.setStockMinimo(10.0); req.setUnidadMedida("kg");
        req.setDescuentaStock(null);
        when(materialRepo.save(any(Material.class))).thenAnswer(i -> i.getArgument(0));

        MaterialResponse res = service.crear(req);
        assertThat(res.nombre()).isEqualTo("Yeso");
    }

    @Test
    void actualizar_inexistente_lanza404() {
        when(materialRepo.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.actualizar(9L, new MaterialRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void movimiento_entrada_sumaStock() {
        Material m = material(10, 2.0);
        when(materialRepo.findById(1L)).thenReturn(Optional.of(m));
        when(materialRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.registrarMovimiento(mov(TipoMovimiento.ENTRADA, 5));

        assertThat(m.getStockActual()).isEqualTo(15.0);
        verify(movimientoRepo).save(any(MovimientoStock.class));
    }

    @Test
    void movimiento_salida_restaStockPorDebajoDelMinimo() {
        Material m = material(6, 5.0);
        when(materialRepo.findById(1L)).thenReturn(Optional.of(m));
        when(materialRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.registrarMovimiento(mov(TipoMovimiento.SALIDA, 3)); // 6-3=3 <= 5

        assertThat(m.getStockActual()).isEqualTo(3.0);
    }

    @Test
    void movimiento_salida_aNegativo_noFalla() {
        Material m = material(2, 0.0);
        when(materialRepo.findById(1L)).thenReturn(Optional.of(m));
        when(materialRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.registrarMovimiento(mov(TipoMovimiento.SALIDA, 5)); // 2-5 = -3

        assertThat(m.getStockActual()).isEqualTo(-3.0);
    }

    @Test
    void movimiento_ajuste_reemplazaStock() {
        Material m = material(10, 2.0);
        when(materialRepo.findById(1L)).thenReturn(Optional.of(m));
        when(materialRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.registrarMovimiento(mov(TipoMovimiento.AJUSTE, 42));

        assertThat(m.getStockActual()).isEqualTo(42.0);
    }

    @Test
    void movimiento_materialInexistente_lanza404() {
        when(materialRepo.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.registrarMovimiento(mov(TipoMovimiento.ENTRADA, 1)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void eliminar_hacesoftDelete() {
        Material m = material(10, 2.0);
        when(materialRepo.findById(1L)).thenReturn(Optional.of(m));
        when(materialRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.eliminar(1L);

        assertThat(m.isActivo()).isFalse();
    }
}
