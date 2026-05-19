export const environment = {
  production: false,

  // 🎬 MODO DEMO — usar mocks en lugar de hacer peticiones HTTP.
  // Cuando es true: el login es fake (admin/admin123), los services
  // devuelven datos hardcodeados y no se requiere ningún backend.
  // Útil para presentaciones al cliente con compu sin recursos.
  useMocks: true,

  // En dev apuntamos directo a cada microservicio (sin gateway)
  apiUrl: 'http://localhost:8083',         // ms-catalogo
  produccionUrl: 'http://localhost:8084',  // ms-produccion
  pedidosUrl: 'http://localhost:8082',     // ms-pedidos
  finanzasUrl: 'http://localhost:8085',    // ms-finanzas
  stockUrl: 'http://localhost:8086',       // ms-stock
};
