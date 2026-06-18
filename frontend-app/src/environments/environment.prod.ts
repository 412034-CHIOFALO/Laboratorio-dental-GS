/**
 * Producción — el frontend se sirve desde nginx que hace proxy de /api/* y
 * /ms-auth/* hacia el api-gateway interno del container. Por eso todas las
 * URLs son relativas: no hay CORS porque comparten el mismo origen.
 */
export const environment = {
  production: true,
  useMocks: false,

  // URLs relativas — nginx hace proxy al api-gateway (mismo origen, sin CORS).
  gatewayUrl: '',
  apiUrl:     '',
  pedidosUrl: '',
  finanzasUrl:'',
  stockUrl:   '',

  // Login vía gateway (nginx lo rutea a ms-auth). NUNCA usar localhost:8081 acá.
  loginUrl: '/ms-auth/api/auth/login',
};
