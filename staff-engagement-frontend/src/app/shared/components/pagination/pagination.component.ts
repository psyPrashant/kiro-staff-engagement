import { Component, computed, input, output } from '@angular/core';

@Component({
  selector: 'app-pagination',
  standalone: true,
  template: `
    @if (totalPages() > 1) {
      <div class="pagination" data-testid="pagination">
        <button
          class="btn btn-secondary"
          [disabled]="page() <= 1"
          (click)="goTo(page() - 1)"
          aria-label="Previous page"
          data-testid="pagination-prev"
        >
          Prev
        </button>
        <span class="pagination-status" data-testid="pagination-status">
          Page {{ page() }} of {{ totalPages() }}
        </span>
        <button
          class="btn btn-secondary"
          [disabled]="page() >= totalPages()"
          (click)="goTo(page() + 1)"
          aria-label="Next page"
          data-testid="pagination-next"
        >
          Next
        </button>
      </div>
    }
  `,
  styles: [
    `
      .pagination {
        display: flex;
        align-items: center;
        justify-content: flex-end;
        gap: var(--space-3);
        margin-top: var(--space-3);
      }
      .pagination-status {
        font-size: var(--fs-sm);
        color: var(--color-muted);
      }
    `,
  ],
})
export class PaginationComponent {
  readonly page = input.required<number>();
  readonly totalPages = input.required<number>();
  readonly pageChange = output<number>();

  protected readonly safeTotal = computed(() => Math.max(1, this.totalPages()));

  goTo(target: number): void {
    const clamped = Math.min(Math.max(1, target), this.safeTotal());
    if (clamped !== this.page()) {
      this.pageChange.emit(clamped);
    }
  }
}
