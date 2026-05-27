package com.gys.ms_auth.controllers;

import com.gys.ms_auth.dto.RegisterRequest;
import com.gys.ms_auth.dto.UsuarioResponse;
import com.gys.ms_auth.model.Usuario;
import com.gys.ms_auth.service.UsuarioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtEncoder jwtEncoder;
    private final AuthenticationManager authenticationManager;
    private final UsuarioService usuarioService;

    public AuthController(JwtEncoder jwtEncoder,
                          AuthenticationManager authenticationManager,
                          UsuarioService usuarioService) {
        this.jwtEncoder = jwtEncoder;
        this.authenticationManager = authenticationManager;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );

            String roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

            JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("http://localhost:8081")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .subject(auth.getName())
                .claim("roles", roles)
                .build();

            String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
            return ResponseEntity.ok(Map.of("access_token", token));

        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Tu cuenta está pendiente de aprobación por el administrador."));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Usuario o contraseña incorrectos."));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            Usuario nuevo = usuarioService.registrar(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "mensaje", "Solicitud enviada. El administrador activará tu cuenta pronto.",
                "username", nuevo.getUsername()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/usuarios")
    public ResponseEntity<List<UsuarioResponse>> listarUsuarios() {
        List<UsuarioResponse> usuarios = usuarioService.listarTodos()
            .stream()
            .map(UsuarioResponse::from)
            .toList();
        return ResponseEntity.ok(usuarios);
    }

    @PutMapping("/usuarios/{id}/aprobar")
    public ResponseEntity<?> aprobar(@PathVariable Long id) {
        try {
            Usuario aprobado = usuarioService.aprobar(id);
            return ResponseEntity.ok(Map.of(
                "mensaje", "Usuario activado correctamente.",
                "username", aprobado.getUsername()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    public record LoginRequest(
        @NotBlank(message = "El username no puede estar vacío")
        @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
        String username,

        @NotBlank(message = "La contraseña no puede estar vacía")
        @Size(min = 6, max = 100, message = "La contraseña debe tener entre 6 y 100 caracteres")
        String password
    ) {}
}
