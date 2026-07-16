import { Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-avatar',
  standalone: true,
  template: `
    <span class="avatar {{ sizeClass() }}" [attr.aria-label]="name()">
      {{ initials() }}
    </span>
  `,
})
export class AvatarComponent {
  readonly name = input.required<string>();
  readonly size = input<'sm' | 'md' | 'lg'>('md');

  protected readonly sizeClass = computed(() => `avatar--${this.size()}`);

  protected readonly initials = computed(() => {
    const parts = this.name().trim().split(/\s+/);
    if (parts.length >= 2) {
      return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }
    return (parts[0]?.[0] ?? '').toUpperCase();
  });
}
