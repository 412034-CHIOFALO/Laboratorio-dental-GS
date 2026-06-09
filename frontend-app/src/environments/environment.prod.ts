/**
 * Producción — el frontend se sirve desde nginx que hace proxy de /api/* y
 * /ms-auth/* hacia el api-gateway interno del container. Por eso todas las
 * URLs son relativas: no hay CORS porque comparten el mismo origen.
 */
export const environment = {
  production: false,

  // Mocks deshabilitados en producción
  useMocks: true,

  // URLs relativas — nginx hace proxy al api-gateway
  gatewayUrl:    '',
  apiUrl:        '',
  produccionUrl: '',
  pedidosUrl:    '',
  finanzasUrl:   '',
  stockUrl:      '',
};
