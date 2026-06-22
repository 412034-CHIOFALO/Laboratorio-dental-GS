package com.gs.ms_finanzas.service;

import com.gs.ms_finanzas.dto.SueldoRequest;
import com.gs.ms_finanzas.exception.BusinessException;
import com.gs.ms_finanzas.model.EstadoSueldo;
import com.gs.ms_finanzas.model.SueldoEmpleado;
import com.gs.ms_finanzas.repository.SueldoEmpleadoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SueldoServiceTest {

    @Mock private SueldoEmpleadoRepository sueldoRepo;
    @InjectMocks private SueldoService service;

    private SueldoEmpleado sueldo() {
        return SueldoEmpleado.builder().empleadoId(1L).empleadoNombre("Ana")
                .monto(new BigDecimal("300")).mes(1).anio(2026).estado(EstadoSueldo.PENDIENTE).build();
    }

    private SueldoRequest req() {
        SueldoRequest r = new SueldoRequest();
        r.setEmpleadoId(1L); r.setEmpleadoNombre("Ana");
        r.setMonto(new BigDecimal("300")); r.setMes(1); r.setAnio(2026);
        return r;
    }

    @Test
    void listarPorMes_mapea() {
        when(sueldoRepo.findByAnioAndMesOrderByEmpleadoNombreAsc(2026, 1)).thenReturn(List.of(sueldo()));
        assertThat(service.listarPorMes(2026, 1)).hasSize(1);
    }

    @Test
    void listarPendientes_mapea() {
        when(sueldoRepo.findByAnioAndMesAndEstadoOrderByEmpleadoNombreAsc(2026, 1, EstadoSueldo.PENDIENTE))
                .thenReturn(List.of(sueldo()));
        assertThat(service.listarPendientesMes(2026, 1)).hasSize(1);
    }

    @Test
    void registrar_duplicado_lanzaBusiness() {
        when(sueldoRepo.existsByEmpleadoIdAndAnioAndMes(1L, 2026, 1)).thenReturn(true);
        assertThatThrownBy(() -> service.registrar(req())).isInstanceOf(BusinessException.class);
    }

    @Test
    void registrar_ok_guarda() {
        when(sueldoRepo.existsByEmpleadoIdAndAnioAndMes(1L, 2026, 1)).thenReturn(false);
        when(sueldoRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        assertThat(service.registrar(req())).isNotNull();
    }
}
