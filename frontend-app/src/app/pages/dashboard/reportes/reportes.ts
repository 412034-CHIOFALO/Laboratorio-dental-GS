import { Component } from '@angular/core';

interface BarData  { mes: string; valor: number; }
interface LineData  { mes: string; ingresos: number; deuda: number; }
interface DonutSeg  { label: string; pct: number; color: string; dash: number; offset: number; }

@Component({
  selector: 'app-reportes',
  standalone: true,
  imports: [],
  templateUrl: './reportes.html',
  styleUrls: ['./reportes.css'],
})
export class ReportesComponent {

  // ── KPIs ──────────────────────────────────────────────────
  kpis = [
    { label: 'Pedidos este mes',    value: '41',        sub: '+8 vs mes anterior', color: 'green',  icon: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2' },
    { label: 'Ingresos del mes',    value: '$1.320.000', sub: '$980K cobrado',      color: 'cyan',   icon: 'M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z' },
    { label: 'Deuda total clientes', value: '$785.000',  sub: '5 odontólogos',     color: 'amber',  icon: 'M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z' },
    { label: 'Entregas pendientes', value: '2',         sub: 'Listos para hoy',   color: 'orange', icon: 'M5 8h14M5 8a2 2 0 110-4h14a2 2 0 110 4M5 8l1 12a2 2 0 002 2h8a2 2 0 002-2l1-12' },
  ];

  // ── BAR CHART: pedidos por mes ────────────────────────────
  barData: BarData[] = [
    { mes: 'Dic', valor: 28 },
    { mes: 'Ene', valor: 32 },
    { mes: 'Feb', valor: 38 },
    { mes: 'Mar', valor: 45 },
    { mes: 'Abr', valor: 52 },
    { mes: 'May', valor: 41 },
  ];

  get barMax(): number { return Math.max(...this.barData.map(d => d.valor)); }
  barHeight(v: number): number { return Math.round((v / this.barMax) * 140); }
  barY(v: number): number { return 160 - this.barHeight(v); }
  barX(i: number): number { return 30 + i * 72; }

  // ── LINE CHART: ingresos vs deuda ─────────────────────────
  lineData: LineData[] = [
    { mes: 'Dic', ingresos: 1_200_000, deuda: 450_000 },
    { mes: 'Ene', ingresos:   980_000, deuda: 520_000 },
    { mes: 'Feb', ingresos: 1_450_000, deuda: 380_000 },
    { mes: 'Mar', ingresos: 1_100_000, deuda: 620_000 },
    { mes: 'Abr', ingresos: 1_680_000, deuda: 580_000 },
    { mes: 'May', ingresos: 1_320_000, deuda: 785_000 },
  ];

  private lineMax(): number { return Math.max(...this.lineData.flatMap(d => [d.ingresos, d.deuda])); }
  linePoints(key: 'ingresos' | 'deuda'): string {
    const max = this.lineMax();
    return this.lineData.map((d, i) => {
      const x = 30 + i * 80;
      const y = 160 - Math.round((d[key] / max) * 140);
      return `${x},${y}`;
    }).join(' ');
  }
  lineX(i: number): number { return 30 + i * 80; }
  lineY(val: number): number { return 160 - Math.round((val / this.lineMax()) * 140); }

  // ── DONUT CHART: trabajos por tipo ────────────────────────
  donutData: DonutSeg[] = (() => {
    const raw = [
      { label: 'Fija (coronas)', pct: 45, color: '#22c55e' },
      { label: 'Removible',      pct: 25, color: '#06b6d4' },
      { label: 'Ortodoncia',     pct: 20, color: '#a855f7' },
      { label: 'ATM',            pct: 7,  color: '#f59e0b' },
      { label: 'Otros',          pct: 3,  color: '#f43f5e' },
    ];
    const circ = 2 * Math.PI * 60; // r=60
    let offset = 0;
    return raw.map(r => {
      const dash = (r.pct / 100) * circ;
      const seg = { ...r, dash, offset: -offset };
      offset += dash;
      return seg;
    });
  })();

  // ── BAR CHART 2: pedidos por tipo de trabajo ──────────────
  tipoData = [
    { label: 'Corona M/P',    valor: 18, color: '#22c55e' },
    { label: 'Prótesis Acr.', valor: 10, color: '#06b6d4' },
    { label: 'Zirconio',      valor:  7, color: '#a855f7' },
    { label: 'Ortodoncia',    valor:  6, color: '#f59e0b' },
    { label: 'ATM',           valor:  5, color: '#f43f5e' },
    { label: 'Otros',         valor:  4, color: '#64748b' },
  ];
  get tipoMax(): number { return Math.max(...this.tipoData.map(d => d.valor)); }
  tipoWidth(v: number): number { return Math.round((v / this.tipoMax) * 100); }
}
