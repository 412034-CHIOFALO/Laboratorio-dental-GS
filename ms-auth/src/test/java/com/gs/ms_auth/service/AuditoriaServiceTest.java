package com.gs.ms_auth.service;

import com.gs.ms_auth.model.AuditoriaEvento;
import com.gs.ms_auth.repository.AuditoriaEventoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTest {

    @Mock private AuditoriaEventoRepository repo;
    @InjectMocks private AuditoriaService service;

    @Test
    void registrar_persisteEvento() {
        service.registrar("admin", "LOGIN", "Inicio de sesion", "Sesion", "ok");
        verify(repo).save(org.mockito.ArgumentMatchers.any(AuditoriaEvento.class));
    }

    @Test
    void listarTodos_delegaOrdenado() {
        when(repo.findAllByOrderByTimestampDesc())
                .thenReturn(List.of(new AuditoriaEvento("admin", "LOGIN", "x", "y", "z")));
        assertThat(service.listarTodos()).hasSize(1);
    }
}
