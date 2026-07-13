import { Component, input, output } from '@angular/core';
import { SortOption } from '../models/engagement.model';

@Component({
  selector: 'app-sort-control',
  standalone: true,
  templateUrl: './sort-control.component.html',
  styleUrl: './sort-control.component.css',
})
export class SortControlComponent {
  readonly activeSort = input.required<SortOption>();
  readonly sortChange = output<SortOption>();

  selectSort(option: SortOption): void {
    this.sortChange.emit(option);
  }
}
