import { Component, inject, OnInit, signal, WritableSignal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly errorMessage: WritableSignal<string | null> = signal(null);
  readonly isLoading: WritableSignal<boolean> = signal(false);

  readonly loginForm: FormGroup = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(128)]],
  });

  ngOnInit(): void {
    if (this.authService.isAuthenticated()) {
      this.router.navigateByUrl('/user');
      return;
    }

    this.loginForm.get('email')!.valueChanges.subscribe(() => {
      this.errorMessage.set(null);
    });

    this.loginForm.get('password')!.valueChanges.subscribe(() => {
      this.errorMessage.set(null);
    });
  }

  onSubmit(): void {
    if (this.loginForm.invalid || this.isLoading()) {
      return;
    }

    const { email, password } = this.loginForm.getRawValue();
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.authService.login(email, password).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.navigateAfterLogin();
      },
      error: (error: HttpErrorResponse) => {
        this.isLoading.set(false);
        this.errorMessage.set(this.mapError(error));
      },
    });
  }

  private navigateAfterLogin(): void {
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');

    if (returnUrl && this.isValidReturnUrl(returnUrl)) {
      this.router.navigateByUrl(returnUrl);
    } else {
      this.router.navigateByUrl('/user');
    }
  }

  private isValidReturnUrl(url: string): boolean {
    if (!url.startsWith('/')) {
      return false;
    }
    if (url.startsWith('//')) {
      return false;
    }
    if (url.length > 2048) {
      return false;
    }
    return true;
  }

  private mapError(error: HttpErrorResponse): string {
    switch (error.status) {
      case 401:
        return 'Invalid email or password';
      case 400:
        return 'Required fields are missing or malformed';
      default:
        return 'Login failed. Please try again later.';
    }
  }
}
