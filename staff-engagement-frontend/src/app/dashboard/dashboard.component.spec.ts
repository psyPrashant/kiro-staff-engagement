import { TestBed, ComponentFixture } from '@angular/core/testing';
import { DashboardComponent } from './dashboard.component';

describe('DashboardComponent', () => {
  let fixture: ComponentFixture<DashboardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
  });

  it('should render exactly 4 skeleton cards', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const skeletonCards = compiled.querySelectorAll('[data-testid="skeleton-card"]');
    expect(skeletonCards.length).toBe(4);
  });

  it('should apply skeleton-card CSS class to each card', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const skeletonCards = compiled.querySelectorAll('[data-testid="skeleton-card"]');
    skeletonCards.forEach((card) => {
      expect(card.classList.contains('skeleton-card')).toBe(true);
    });
  });

  it('should have data-testid="dashboard" on the container', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const dashboard = compiled.querySelector('[data-testid="dashboard"]');
    expect(dashboard).toBeTruthy();
  });
});
