package com.gs.ms_auth.service;

import com.gs.ms_auth.model.Rol;
import com.gs.ms_auth.model.Usuario;
import com.gs.ms_auth.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock private UsuarioRepository repo;
    @InjectMocks private CustomUserDetailsService service;

    private Usuario usuario(boolean enabled) {
        return Usuario.builder().id(1L).username("admin").password("HASH")
                .rol(Rol.ADMIN).enabled(enabled).build();
    }

    @Test
    void loadByUsername_inexistente_lanza() {
        when(repo.findByUsername("nadie")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.loadUserByUsername("nadie"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void loadByUsername_habilitado_mapeaRolYAuthorities() {
        when(repo.findByUsername("admin")).thenReturn(Optional.of(usuario(true)));

        UserDetails ud = service.loadUserByUsername("admin");

        assertThat(ud.getUsername()).isEqualTo("admin");
        assertThat(ud.getPassword()).isEqualTo("HASH");
        assertThat(ud.isEnabled()).isTrue();
        assertThat(ud.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @Test
    void loadByUsername_deshabilitado_disabledTrue() {
        when(repo.findByUsername("admin")).thenReturn(Optional.of(usuario(false)));

        UserDetails ud = service.loadUserByUsername("admin");

        assertThat(ud.isEnabled()).isFalse();
    }
}
