import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class TutorialService {

  async iniciarTour(): Promise<void> {
    const { driver } = await import('driver.js');

    const driverObj = driver({
      showProgress: true,
      nextBtnText: 'Siguiente →',
      prevBtnText: '← Anterior',
      doneBtnText: 'Finalizar',
      progressText: '{{current}} de {{total}}',
      popoverClass: 'gs-tour-popover',
      steps: [
        {
          popover: {
            title: '👋 Bienvenido al ERP G&S',
            description: 'Este tutorial te guiará por las secciones principales del sistema en unos pocos pasos.',
            align: 'center',
          },
        },
        {
          element: '[data-tour="nav-pedidos"]',
          popover: {
            title: '📦 Pedidos',
            description: 'Aquí se registran todos los trabajos solicitados por los odontólogos. Podés ver el estado, la fecha de entrega y filtrar por atrasados.',
            side: 'right',
            align: 'start',
          },
        },
        {
          element: '[data-tour="nav-produccion"]',
          popover: {
            title: '⚙️ Producción',
            description: 'Vista operativa para los técnicos. Se registran los avances y cuando se completa el trabajo pasa a TERMINADO.',
            side: 'right',
            align: 'start',
          },
        },
        {
          element: '[data-tour="nav-entregas"]',
          popover: {
            title: '🚚 Entregas',
            description: 'Confirmá la entrega de los trabajos terminados. Al confirmar el pedido pasa a ENTREGADO y queda disponible para facturación.',
            side: 'right',
            align: 'start',
          },
        },
        {
          element: '[data-tour="nav-finanzas"]',
          popover: {
            title: '💰 Finanzas',
            description: 'Gestioná las cajas del laboratorio, los sueldos y los pagos. El bot de WhatsApp registra transferencias aquí automáticamente.',
            side: 'right',
            align: 'start',
          },
        },
        {
          element: '[data-tour="nav-stock"]',
          popover: {
            title: '📦 Stock',
            description: 'Control de inventario de materiales. Configurá el stock mínimo de cada material para recibir alertas automáticas por WhatsApp.',
            side: 'right',
            align: 'start',
          },
        },
        {
          element: '[data-tour="nav-bot"]',
          popover: {
            title: '🤖 Bot WhatsApp',
            description: 'El bot lee los comprobantes de transferencia enviados al grupo del laboratorio y los registra automáticamente. Revisá y confirmá los registros pendientes desde aquí.',
            side: 'right',
            align: 'start',
          },
        },
        {
          element: '[data-tour="nav-reportes"]',
          popover: {
            title: '📊 Reportes',
            description: 'Resúmenes mensuales con datos de facturación, cobranzas y estado de cajas. Podés exportar un PDF de cierre mensual.',
            side: 'right',
            align: 'start',
          },
        },
        {
          element: '[data-tour="nav-configuracion"]',
          popover: {
            title: '⚙️ Configuración',
            description: 'Configurá el número de WhatsApp del administrador para las alertas de stock y otras opciones del sistema (solo ADMIN).',
            side: 'right',
            align: 'start',
          },
        },
        {
          element: '[data-tour="nav-manual"]',
          popover: {
            title: '📖 Manual de usuario',
            description: 'Esta sección contiene la documentación completa del sistema. ¡Llegaste al final del tour! Explorá el manual para más detalles.',
            side: 'right',
            align: 'start',
          },
        },
      ],
    });

    driverObj.drive();
  }
}
