export const environment = {
  production: false,

  // ─── MODO ────────────────────────────────────────────────────────────────────
  // true  → mocks en memoria (demo sin backend, login: admin/admin123)
  // false → backend real a través del api-gateway (requiere todos los ms corriendo)
  useMocks: true,

  // ─── GATEWAY ─────────────────────────────────────────────────────────────────
  // Todos los microservicios se acceden a través del gateway en puerto 8080.
  // Para usar el backend real: cambiar useMocks a false y levantar los servicios.
  gatewayUrl: 'http://localhost:8080',

  // Aliases por ms — apuntan todos al gateway (el gateway rutea internamente)
  apiUrl:      'http://localhost:8080',  // → ms-catalogo  (/api/catalogo/**)
  pedidosUrl:  'http://localhost:8080',  // → ms-pedidos    (/api/pedidos/**)
  finanzasUrl: 'http://localhost:8080',  // → ms-finanzas   (/api/finanzas/**)
  stockUrl:    'http://localhost:8080',  // → ms-stock      (/api/stock/**)

  // URL del login. En dev va directo a ms-auth (:8081) — workaround mientras el
  // gateway (Spring Security 7) no deja pasar el endpoint público. En prod va por
  // el gateway. El JWT resultante sirve igual en el gateway para el resto.
  loginUrl: 'http://localhost:8081/api/auth/login',
};
