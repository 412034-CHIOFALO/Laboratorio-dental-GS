package com.gs.ms_auth.service;

import com.gs.ms_auth.dto.RegisterRequest;
import com.gs.ms_auth.model.Rol;
import com.gs.ms_auth.model.Usuario;
import com.gs.ms_auth.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock private UsuarioRepository repo;
    @Mock private PasswordEncoder encoder;
    @InjectMocks private UsuarioService service;

    private Usuario usuario() {
        return Usuario.builder().id(1L).username("jperez").nombre("Juan").apellido("Perez")
                .rol(Rol.TECNICO).enabled(false).pendienteAprobacion(true).build();
    }

    private RegisterRequest req() {
        return new RegisterRequest("Juan", "Perez", "jperez", "password1", Rol.TECNICO);
    }

    @Test
    void registrar_usernameDuplicado_lanza() {
        when(repo.existsByUsername("jperez")).thenReturn(true);
        assertThatThrownBy(() -> service.registrar(req())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void registrar_ok_hasheaPasswordYQuedaPendiente() {
        when(repo.existsByUsername("jperez")).thenReturn(false);
        when(encoder.encode("password1")).thenReturn("HASH");
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        Usuario u = service.registrar(req());

        assertThat(u.getPassword()).isEqualTo("HASH");
        assertThat(u.isEnabled()).isFalse();
        assertThat(u.isPendienteAprobacion()).isTrue();
    }

    @Test
    void listarTodos_delega() {
        when(repo.findAll()).thenReturn(List.of(usuario()));
        assertThat(service.listarTodos()).hasSize(1);
    }

    @Test
    void aprobar_inexistente_lanza() {
        when(repo.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.aprobar(9L)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void aprobar_ok_habilita() {
        Usuario u = usuario();
        when(repo.findById(1L)).thenReturn(Optional.of(u));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.aprobar(1L);

        assertThat(u.isEnabled()).isTrue();
        assertThat(u.isPendienteAprobacion()).isFalse();
    }

    @Test
    void cambiarEstado_desactiva() {
        Usuario u = usuario();
        u.setEnabled(true);
        when(repo.findById(1L)).thenReturn(Optional.of(u));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.cambiarEstado(1L, false);

        assertThat(u.isEnabled()).isFalse();
    }

    @Test
    void cambiarEstado_inexistente_lanza() {
        when(repo.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.cambiarEstado(9L, true)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void actualizarTelefono_conValor_trim() {
        Usuario u = usuario();
        when(repo.findById(1L)).thenReturn(Optional.of(u));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.actualizarTelefono(1L, "  1122334455  ");

        assertThat(u.getTelefono()).isEqualTo("1122334455");
    }

    @Test
    void actualizarTelefono_blank_guardaNull() {
        Usuario u = usuario();
        u.setTelefono("viejo");
        when(repo.findById(1L)).thenReturn(Optional.of(u));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.actualizarTelefono(1L, "   ");

        assertThat(u.getTelefono()).isNull();
    }

    @Test
    void actualizarTelefono_inexistente_lanza() {
        when(repo.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.actualizarTelefono(9L, "123")).isInstanceOf(RuntimeException.class);
    }
}
