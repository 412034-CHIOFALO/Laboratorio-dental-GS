package com.gs.ms_finanzas.service;

import com.gs.ms_finanzas.dto.PagoAutomaticoRequest;
import com.gs.ms_finanzas.dto.RegistroPagoBotResponse;
import com.gs.ms_finanzas.model.*;
import com.gs.ms_finanzas.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Tests de la clasificación del receptor del bot (la lógica central):
 * empleado → sueldo, proveedor → pago a proveedor, ninguno → rechazo.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GestionSueldoServiceTest {

    @Mock private ConfiguracionSueldoRepository configRepo;
    @Mock private PagoSueldoRepository pagoRepo;
    @Mock private MinioStorageService minioStorage;
    @Mock private ProveedorRepository proveedorRepo;
    @Mock private RegistroPagoBotRepository registroRepo;
    @Mock private ComprobanteRepository comprobanteRepo;
    @Mock private DeudaProveedorRepository deudaProveedorRepo;
    @Mock private CajaMovimientoRepository cajaMovimientoRepo;
    @Mock private AuditoriaClient auditoria;

    @InjectMocks
    private GestionSueldoService service;

    private PagoAutomaticoRequest req(String receptor, String emisor, String idOp, String monto) {
        PagoAutomaticoRequest r = new PagoAutomaticoRequest();
        r.setReceptorNombre(receptor);
        r.setEmisor(emisor);
        r.setIdOperacion(idOp);
        r.setMonto(new BigDecimal(monto));
        return r;
    }

    @Test
    void receptorEmpleado_registraComoSueldo() {
        ConfiguracionSueldo carlos = ConfiguracionSueldo.builder()
                .empleadoId(2L).empleadoNombre("Carlos López")
                .activo(true).saldoDevengado(new BigDecimal("50000"))
                .saldoSobrante(BigDecimal.ZERO).build();

        when(configRepo.findAllByOrderByEmpleadoNombreAsc()).thenReturn(List.of(carlos));
        when(registroRepo.existsByIdOperacionAndEstado(anyString(), any())).thenReturn(false);
        when(registroRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(pagoRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(configRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        RegistroPagoBotResponse res = service.registrarPagoAutomatico(req("Carlos", "Lab", "OP-1", "30000"));

        assertThat(res.estado()).isEqualTo(EstadoRegistroBot.REGISTRADO);
        assertThat(res.tipoReceptor()).isEqualTo(TipoReceptorBot.EMPLEADO);
        assertThat(res.receptorResuelto()).isEqualTo("Carlos López");
    }

    @Test
    void receptorProveedorSinOdontologo_registraComoPagoDirecto() {
        Proveedor luciano = Proveedor.builder().id(2L).nombre("Luciano Giménez").activo(true).build();

        when(configRepo.findAllByOrderByEmpleadoNombreAsc()).thenReturn(Collections.emptyList());
        when(proveedorRepo.findByActivoTrue()).thenReturn(List.of(luciano));
        when(comprobanteRepo.findAll()).thenReturn(Collections.emptyList());   // emisor no es odontólogo
        when(deudaProveedorRepo.findByProveedorIdOrderByFechaCreacionDesc(any())).thenReturn(Collections.emptyList());
        when(registroRepo.existsByIdOperacionAndEstado(anyString(), any())).thenReturn(false);
        when(registroRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        RegistroPagoBotResponse res = service.registrarPagoAutomatico(req("Luciano", "Lab Tesorería", "OP-2", "5000"));

        assertThat(res.estado()).isEqualTo(EstadoRegistroBot.REGISTRADO);
        assertThat(res.tipoReceptor()).isEqualTo(TipoReceptorBot.PROVEEDOR);
        assertThat(res.receptorResuelto()).isEqualTo("Luciano Giménez");
    }

    @Test
    void receptorDesconocido_rechaza() {
        when(configRepo.findAllByOrderByEmpleadoNombreAsc()).thenReturn(Collections.emptyList());
        when(proveedorRepo.findByActivoTrue()).thenReturn(Collections.emptyList());
        when(registroRepo.existsByIdOperacionAndEstado(anyString(), any())).thenReturn(false);
        when(registroRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        RegistroPagoBotResponse res = service.registrarPagoAutomatico(req("Fulano Inexistente", "X", "OP-3", "100"));

        assertThat(res.estado()).isEqualTo(EstadoRegistroBot.RECHAZADO);
        assertThat(res.tipoReceptor()).isEqualTo(TipoReceptorBot.DESCONOCIDO);
    }
}
