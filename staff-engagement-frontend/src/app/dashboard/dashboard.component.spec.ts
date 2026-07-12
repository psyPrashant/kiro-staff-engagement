import { TestBed, ComponentFixture } from '@angular/core/testing';
import { Component } from '@angular/core';
import { DashboardComponent } from './dashboard.component';

// Mock the InteractionMatrixComponent to avoid deep rendering in shallow tests
@Component({
  selector: 'app-interaction-matrix',
  standalone: true,
  template: '<div data-testid="interaction-matrix-mock"></div>',
})
class MockInteractionMatrixComponent {}

describe('DashboardComponent', () => {
  let fixture: ComponentFixture<DashboardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
    })
      .overrideComponent(DashboardComponent, {
        set: { imports: [MockInteractionMatrixComponent] },
      })
      .compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
  });

  it('should have data-testid="dashboard" on the container', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const dashboard = compiled.querySelector('[data-testid="dashboard"]');
    expect(dashboard).toBeTruthy();
  });

  it('should render the interaction matrix component', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const matrix = compiled.querySelector('app-interaction-matrix');
    expect(matrix).toBeTruthy();
  });

  it('should render a Dashboard heading', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const heading = compiled.querySelector('h1');
    expect(heading?.textContent).toContain('Dashboard');
  });
});
