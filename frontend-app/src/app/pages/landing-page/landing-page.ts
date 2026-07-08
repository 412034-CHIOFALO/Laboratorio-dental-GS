import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

interface ItemGaleria {
  tipo: 'foto' | 'video';
  /** Nombre del archivo dentro de public/instagram/. */
  archivo: string;
  alt: string;
  caption: string;
  layout?: 'tall' | 'wide';
}

/**
 * Galería de "Nuestros trabajos" (sección Instagram de la landing).
 * Los archivos van en frontend-app/public/instagram/ — ver el README ahí
 * mismo para el detalle de nombres y formatos esperados.
 */
const GALERIA_INSTAGRAM: ItemGaleria[] = [
  { tipo: 'foto', archivo: 'trabajo-1.jpg', alt: 'Técnico dental en el laboratorio', caption: 'Precisión en cada trabajo', layout: 'tall' },
  { tipo: 'foto', archivo: 'trabajo-2.jpg', alt: 'Aparato ortodóncico', caption: 'Ortodoncia removible' },
  { tipo: 'video', archivo: 'proceso-armado.mp4', alt: 'Proceso de armado de un aparato', caption: 'Así armamos cada aparato' },
  { tipo: 'foto', archivo: 'trabajo-3.jpg', alt: 'Modelos dentales de yeso en el banco de trabajo', caption: 'El pedido por el que esperan los odontólogos', layout: 'wide' },
  { tipo: 'video', archivo: 'empaquetado.mp4', alt: 'Empaquetado del trabajo terminado', caption: 'Empaquetado y control final' },
];

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './landing-page.html',
  styleUrls: ['./landing-page.css'],
})
export class LandingPage {
  readonly galeria = GALERIA_INSTAGRAM;
  mobileMenuOpen = false;

  toggleMenu() {
    this.mobileMenuOpen = !this.mobileMenuOpen;
  }

  closeMenu() {
    this.mobileMenuOpen = false;
  }
}
