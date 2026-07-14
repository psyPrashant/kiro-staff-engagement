import { computed, inject, Injectable, Signal, signal, WritableSignal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError, Observable, of, tap, timeout } from 'rxjs';
import { User } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  readonly currentUser: WritableSignal<User | null> = signal(null);
  readonly isAuthenticated: Signal<boolean> = computed(() => this.currentUser() !== null);

  login(email: string, password: string): Observable<User> {
    return this.http
      .post<User>('/api/auth/login', { email, password })
      .pipe(tap((user) => this.currentUser.set(user)));
  }

  rehydrate(): Observable<User | null> {
    return this.http.get<User>('/api/auth/me').pipe(
      tap((user) => this.currentUser.set(user)),
      catchError(() => {
        this.currentUser.set(null);
        return of(null);
      }),
    );
  }

  logout(): void {
    if (this.currentUser() === null) {
      this.router.navigateByUrl('/login');
      return;
    }

    this.http
      .post('/api/auth/logout', {})
      .pipe(timeout(10000))
      .subscribe({
        next: () => {
          this.currentUser.set(null);
          this.router.navigateByUrl('/login');
        },
        error: () => {
          this.currentUser.set(null);
          this.router.navigateByUrl('/login');
        },
      });
  }
}
