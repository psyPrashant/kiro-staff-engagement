import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { TaskCreateDialogComponent } from './task-create-dialog.component';

describe('TaskCreateDialogComponent', () => {
  let fixture: ComponentFixture<TaskCreateDialogComponent>;
  let component: TaskCreateDialogComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TaskCreateDialogComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(TaskCreateDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  function openDialog(): void {
    component.open = true;
    fixture.detectChanges();
  }

  it('should not render dialog when open is false (default)', () => {
    const overlay = fixture.nativeElement.querySelector('.dialog-overlay');
    expect(overlay).toBeNull();
  });

  it('should render dialog with correct ARIA attributes when open', () => {
    openDialog();

    const panel = fixture.nativeElement.querySelector('[role="dialog"]');
    expect(panel).not.toBeNull();
    expect(panel.getAttribute('aria-modal')).toBe('true');
    expect(panel.getAttribute('aria-labelledby')).toBe('dialog-title');

    const title = fixture.nativeElement.querySelector('#dialog-title');
    expect(title).not.toBeNull();
    expect(title.textContent).toContain('Create Task');
  });

  it('should emit closed on Escape key', () => {
    openDialog();

    let closedEmitted = false;
    component.closed.subscribe(() => (closedEmitted = true));

    const event = new KeyboardEvent('keydown', { key: 'Escape', bubbles: true });
    fixture.nativeElement.dispatchEvent(event);

    expect(closedEmitted).toBe(true);
  });

  it('should emit closed on backdrop click', () => {
    openDialog();

    let closedEmitted = false;
    component.closed.subscribe(() => (closedEmitted = true));

    const overlay = fixture.nativeElement.querySelector('.dialog-overlay');
    expect(overlay).not.toBeNull();
    overlay.click();

    expect(closedEmitted).toBe(true);
  });

  it('should not emit closed when clicking inside dialog panel', () => {
    openDialog();

    let closedEmitted = false;
    component.closed.subscribe(() => (closedEmitted = true));

    const panel = fixture.nativeElement.querySelector('.dialog-panel');
    expect(panel).not.toBeNull();
    panel.click();

    expect(closedEmitted).toBe(false);
  });

  it('should render TaskFormComponent when open', () => {
    openDialog();

    const form = fixture.nativeElement.querySelector('.task-form');
    expect(form).not.toBeNull();
  });

  it('should render overlay backdrop when open', () => {
    openDialog();

    const overlay = fixture.nativeElement.querySelector('.dialog-overlay');
    expect(overlay).not.toBeNull();
  });

  it('should have dialog title with correct id for aria-labelledby', () => {
    openDialog();

    const title = fixture.nativeElement.querySelector('#dialog-title');
    expect(title).not.toBeNull();
    expect(title.tagName.toLowerCase()).toBe('h2');
  });
});
