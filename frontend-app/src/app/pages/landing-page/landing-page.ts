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
  { tipo: 'video', archivo: 'reel-1.mp4', alt: 'Trabajo del laboratorio GS Ortodoncia', caption: 'Así trabajamos en el laboratorio', layout: 'tall' },
  { tipo: 'video', archivo: 'reel-2.mp4', alt: 'Trabajo del laboratorio GS Ortodoncia', caption: 'Precisión en cada detalle', layout: 'tall' },
  { tipo: 'video', archivo: 'reel-3.mp4', alt: 'Trabajo del laboratorio GS Ortodoncia', caption: 'Un vistazo a nuestro día a día', layout: 'tall' },
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
