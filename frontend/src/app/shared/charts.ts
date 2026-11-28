import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  ElementRef,
  inject,
  input,
  signal
} from '@angular/core';

import { compact } from './format';

export interface ChartDatum {
  label: string;
  sublabel?: string;
  value: number;
  display: string;
  details?: Array<{ label: string; value: string }>;
}

function observeWidth(element: ElementRef<HTMLElement>, destroyRef: DestroyRef) {
  const width = signal(element.nativeElement.clientWidth || 600);
  const observer = new ResizeObserver((entries) => {
    const next = Math.round(entries[0].contentRect.width);
    if (next > 0 && next !== width()) {
      width.set(next);
    }
  });
  observer.observe(element.nativeElement);
  destroyRef.onDestroy(() => observer.disconnect());
  return width;
}

const TABLE_VIEW = `
  <details class="table-view">
    <summary>View as table</summary>
    <div class="table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th>{{ labelHeader() }}</th>
            <th class="num">{{ valueHeader() }}</th>
            @for (column of detailColumns(); track column) {
              <th class="num">{{ column }}</th>
            }
          </tr>
        </thead>
        <tbody>
          @for (row of data(); track row.label) {
            <tr>
              <td>{{ row.label }}@if (row.sublabel) {<span class="sub">{{ row.sublabel }}</span>}</td>
              <td class="num">{{ row.display }}</td>
              @for (detail of row.details ?? []; track detail.label) {
                <td class="num">{{ detail.value }}</td>
              }
            </tr>
          }
        </tbody>
      </table>
    </div>
  </details>`;

@Component({
  selector: 'app-bar-list',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="bars" role="list" [attr.aria-label]="ariaLabel()">
      @for (row of data(); track row.label; let index = $index) {
        <div
          class="bar-row"
          role="listitem"
          tabindex="0"
          [attr.aria-label]="row.label + ': ' + row.display"
          (pointerenter)="hovered.set(index)"
          (pointerleave)="hovered.set(null)"
          (focus)="hovered.set(index)"
          (blur)="hovered.set(null)">
          <div class="bar-label">
            <span>{{ row.label }}</span>
            @if (row.sublabel) {
              <small>{{ row.sublabel }}</small>
            }
          </div>
          <div class="bar-track">
            <div class="bar" [class.lift]="hovered() === index" [style.width.%]="widthOf(row.value)"></div>
            <span class="bar-value">{{ row.display }}</span>
          </div>
          @if (hovered() === index && row.details?.length) {
            <div class="viz-tip" role="tooltip">
              <strong>{{ row.display }}</strong>
              <span class="tip-name">{{ row.label }}</span>
              @for (detail of row.details; track detail.label) {
                <span class="tip-row"><b>{{ detail.value }}</b> {{ detail.label }}</span>
              }
            </div>
          }
        </div>
      } @empty {
        <p class="empty">No sales in this period.</p>
      }
    </div>
    ${TABLE_VIEW}
  `,
  styleUrl: './charts.css'
})
export class BarListComponent {
  readonly data = input.required<ChartDatum[]>();
  readonly ariaLabel = input('Bar chart');
  readonly labelHeader = input('Name');
  readonly valueHeader = input('Value');

  readonly hovered = signal<number | null>(null);
  readonly max = computed(() => Math.max(0, ...this.data().map((row) => row.value)));
  readonly detailColumns = computed(() => (this.data()[0]?.details ?? []).map((detail) => detail.label));

  widthOf(value: number): number {
    const max = this.max();
    return max > 0 ? Math.max(0, (value / max) * 100) : 0;
  }
}

@Component({
  selector: 'app-column-chart',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="column-chart">
      @if (data().length === 0) {
        <p class="empty">No sales in this period.</p>
      } @else {
        <svg [attr.width]="width()" [attr.height]="height" role="img" [attr.aria-label]="ariaLabel()">
          @for (tick of ticks(); track tick.value) {
            <line class="grid" [attr.x1]="left" [attr.x2]="width() - right" [attr.y1]="tick.y" [attr.y2]="tick.y" />
            <text class="tick" [attr.x]="left - 8" [attr.y]="tick.y + 4" text-anchor="end">{{ tick.text }}</text>
          }
          <line class="axis" [attr.x1]="left" [attr.x2]="width() - right" [attr.y1]="baseline" [attr.y2]="baseline" />
          @for (column of columns(); track column.label; let index = $index; let last = $last) {
            <g
              class="col"
              tabindex="0"
              [attr.aria-label]="column.label + ': ' + column.display"
              (pointerenter)="hovered.set(index)"
              (pointerleave)="hovered.set(null)"
              (focus)="hovered.set(index)"
              (blur)="hovered.set(null)">
              <rect class="hit" [attr.x]="column.bandX" [attr.y]="top" [attr.width]="column.bandWidth" [attr.height]="baseline - top" />
              <path class="mark" [class.lift]="hovered() === index" [attr.d]="column.path" />
              <text class="tick" [attr.x]="column.center" [attr.y]="baseline + 18" text-anchor="middle">{{ column.short }}</text>
              @if (last) {
                <text class="cap" [attr.x]="column.center" [attr.y]="column.y - 8" text-anchor="middle">{{ column.display }}</text>
              }
            </g>
          }
        </svg>
        @if (hoveredColumn(); as column) {
          <div class="viz-tip" role="tooltip" [style.left.px]="tipLeft(column.center)" [style.top.px]="8">
            <strong>{{ column.display }}</strong>
            <span class="tip-name">{{ column.label }}</span>
            @for (detail of column.details ?? []; track detail.label) {
              <span class="tip-row"><b>{{ detail.value }}</b> {{ detail.label }}</span>
            }
          </div>
        }
      }
    </div>
    ${TABLE_VIEW}
  `,
  styleUrl: './charts.css'
})
export class ColumnChartComponent {
  readonly data = input.required<(ChartDatum & { short?: string })[]>();
  readonly ariaLabel = input('Column chart');
  readonly labelHeader = input('Period');
  readonly valueHeader = input('Value');
  readonly tickFormat = input<(value: number, axisMax: number) => string>(compact);

  readonly height = 240;
  readonly top = 28;
  readonly baseline = 210;
  readonly left = 56;
  readonly right = 8;

  readonly hovered = signal<number | null>(null);
  readonly width = observeWidth(inject(ElementRef), inject(DestroyRef));
  readonly detailColumns = computed(() => (this.data()[0]?.details ?? []).map((detail) => detail.label));

  readonly scale = computed(() => {
    const max = Math.max(0, ...this.data().map((row) => row.value));
    if (max === 0) {
      return { max: 1, step: 1 };
    }
    const rough = max / 4;
    const power = 10 ** Math.floor(Math.log10(rough));
    const step = [1, 2, 2.5, 5, 10].map((factor) => factor * power).find((candidate) => candidate >= rough) ?? rough;
    return { max: Math.ceil(max / step) * step, step };
  });

  readonly ticks = computed(() => {
    const { max, step } = this.scale();
    const format = this.tickFormat();
    const ticks = [];
    for (let value = 0; value <= max + step / 2; value += step) {
      ticks.push({ value, y: this.yOf(value), text: format(value, max) });
    }
    return ticks;
  });

  readonly columns = computed(() => {
    const rows = this.data();
    const plotWidth = Math.max(40, this.width() - this.left - this.right);
    const band = plotWidth / Math.max(rows.length, 1);
    const barWidth = Math.min(24, band * 0.6);
    return rows.map((row, index) => {
      const bandX = this.left + index * band;
      const center = bandX + band / 2;
      const y = this.yOf(row.value);
      const x = center - barWidth / 2;
      const radius = Math.min(4, barWidth / 2, Math.max(0, this.baseline - y));
      const path =
        `M${x},${this.baseline} V${y + radius} Q${x},${y} ${x + radius},${y}` +
        ` H${x + barWidth - radius} Q${x + barWidth},${y} ${x + barWidth},${y + radius}` +
        ` V${this.baseline} Z`;
      return { ...row, short: row.short ?? row.label, bandX, bandWidth: band, center, y, path };
    });
  });

  readonly hoveredColumn = computed(() => {
    const index = this.hovered();
    return index === null ? null : this.columns()[index];
  });

  tipLeft(center: number): number {
    return Math.min(Math.max(center - 90, 0), Math.max(0, this.width() - 180));
  }

  private yOf(value: number): number {
    return this.baseline - (value / this.scale().max) * (this.baseline - this.top);
  }
}

@Component({
  selector: 'app-segment-bar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="segment-bar" role="list" [attr.aria-label]="ariaLabel()">
      @for (part of parts(); track part.label; let index = $index) {
        <div
          class="segment"
          role="listitem"
          tabindex="0"
          [class]="'segment slot-' + part.slot"
          [class.lift]="hovered() === index"
          [style.flex-grow]="part.value"
          [attr.aria-label]="part.label + ': ' + part.display + ', ' + part.share"
          (pointerenter)="hovered.set(index)"
          (pointerleave)="hovered.set(null)"
          (focus)="hovered.set(index)"
          (blur)="hovered.set(null)">
          @if (part.fits) {
            <span>{{ part.share }}</span>
          }
        </div>
      }
    </div>
    @if (hoveredPart(); as part) {
      <div class="viz-tip static" role="tooltip">
        <strong>{{ part.display }}</strong>
        <span class="tip-name">{{ part.label }} · {{ part.share }}</span>
        @for (detail of part.details ?? []; track detail.label) {
          <span class="tip-row"><b>{{ detail.value }}</b> {{ detail.label }}</span>
        }
      </div>
    }
    <ul class="legend">
      @for (part of parts(); track part.label) {
        <li><i [class]="'swatch slot-' + part.slot"></i>{{ part.label }}</li>
      }
    </ul>
    ${TABLE_VIEW}
  `,
  styleUrl: './charts.css'
})
export class SegmentBarComponent {
  readonly data = input.required<ChartDatum[]>();
  readonly ariaLabel = input('Share of total');
  readonly labelHeader = input('Part');
  readonly valueHeader = input('Value');

  readonly hovered = signal<number | null>(null);
  readonly width = observeWidth(inject(ElementRef), inject(DestroyRef));
  readonly detailColumns = computed(() => (this.data()[0]?.details ?? []).map((detail) => detail.label));

  readonly parts = computed(() => {
    const rows = this.data().filter((row) => row.value > 0);
    const total = rows.reduce((sum, row) => sum + row.value, 0) || 1;
    const named = rows.slice(0, rows.length > 4 ? 3 : 4);
    const rest = rows.slice(named.length);
    const parts = named.map((row, index) => ({ ...row, slot: String(index + 1) }));
    if (rest.length > 0) {
      const value = rest.reduce((sum, row) => sum + row.value, 0);
      parts.push({ label: 'Other', value, display: rest.map((row) => row.display).join(' + '), slot: 'other' });
    }
    return parts.map((part) => {
      const shareValue = (part.value / total) * 100;
      const share = `${Math.round(shareValue)}%`;
      const pixels = (shareValue / 100) * this.width();
      return { ...part, share, fits: pixels >= share.length * 8 + 16 };
    });
  });

  readonly hoveredPart = computed(() => {
    const index = this.hovered();
    return index === null ? null : this.parts()[index];
  });
}
