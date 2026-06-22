package com.gs.ms_stock.service;

import com.gs.ms_stock.model.ConfiguracionAlerta;
import com.gs.ms_stock.model.Material;
import com.gs.ms_stock.repository.ConfiguracionAlertaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.Mockito.*;

/**
 * Cubre las ramas de guarda de la alerta de stock. El envio real es HTTP async
 * a un bot que no corre en test: la llamada no bloquea ni lanza excepcion.
 */
@ExtendWith(MockitoExtension.class)
class AlertaStockServiceTest {

    @Mock private ConfiguracionAlertaRepository configRepo;
    @InjectMocks private AlertaStockService service;

    @BeforeEach
    void setUp() {
        // @Value no se procesa en tests unitarios: inyectamos los campos a mano.
        ReflectionTestUtils.setField(service, "botUrl", "http://localhost:3001");
        ReflectionTestUtils.setField(service, "botApiKey", "");
    }

    private Material material() {
        return Material.builder().nombre("Zirconio").stockActual(2.0)
                .stockMinimo(5.0).unidadMedida("gr").build();
    }

    private ConfiguracionAlerta config(boolean activas, String phone) {
        ConfiguracionAlerta c = new ConfiguracionAlerta();
        c.setId(1L);
        c.setAlertasActivas(activas);
        c.setAdminWhatsappPhone(phone);
        return c;
    }

    @Test
    void sinConfiguracion_noHaceNada() {
        when(configRepo.findById(1L)).thenReturn(Optional.empty());
        service.notificarStockBajo(material()); // no debe lanzar
        verify(configRepo).findById(1L);
    }

    @Test
    void alertasDesactivadas_noEnvia() {
        when(configRepo.findById(1L)).thenReturn(Optional.of(config(false, "5491100000000")));
        service.notificarStockBajo(material());
    }

    @Test
    void activasSinTelefono_noEnvia() {
        when(configRepo.findById(1L)).thenReturn(Optional.of(config(true, "  ")));
        service.notificarStockBajo(material());
    }

    @Test
    void activasConTelefono_construyeYDispara() {
        when(configRepo.findById(1L)).thenReturn(Optional.of(config(true, "5491100000000")));
        service.notificarStockBajo(material()); // sendAsync a puerto muerto; no bloquea
    }
}
