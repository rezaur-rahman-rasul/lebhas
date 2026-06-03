import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import { AuthApiService } from '@app/core/auth/auth-api.service';
import { providePlatformIcons } from '@app/core/icons/platform-icons.provider';
import { RegisterFormComponent } from './components/register-form/register-form';

describe('Register flow', () => {
  it('requires confirmPassword in the register form', () => {
    TestBed.configureTestingModule({
      imports: [RegisterFormComponent],
      providers: [providePlatformIcons()],
    });
    const fixture = TestBed.createComponent(RegisterFormComponent);
    const component = fixture.componentInstance as unknown as {
      form: any;
      confirmPasswordError: () => string;
      attemptedSubmit: { set(value: boolean): void };
    };

    component.form.controls.confirmPassword.markAsTouched();
    component.attemptedSubmit.set(true);

    expect(component.form.controls.confirmPassword.hasError('required')).toBe(true);
    expect(component.confirmPasswordError()).toBe('Confirm your password.');
  });

  it('disables register when confirmPassword is empty', () => {
    TestBed.configureTestingModule({
      imports: [RegisterFormComponent],
      providers: [providePlatformIcons()],
    });
    const fixture = TestBed.createComponent(RegisterFormComponent);
    const component = fixture.componentInstance as unknown as { form: any };

    component.form.patchValue({
      firstName: 'Hridoy',
      lastName: 'Bhuiyan',
      email: 'hridoy.bhuiyan12@example.com',
      phone: '+1234567890',
      password: 'StrongP@ssw0rd!',
      confirmPassword: '',
    });
    fixture.detectChanges();

    const button = fixture.nativeElement.querySelector('[data-testid="register-submit"]') as HTMLButtonElement;
    expect(component.form.invalid).toBe(true);
    expect(button.disabled).toBe(true);
  });

  it('disables register and shows a friendly mismatch error', () => {
    TestBed.configureTestingModule({
      imports: [RegisterFormComponent],
      providers: [providePlatformIcons()],
    });
    const fixture = TestBed.createComponent(RegisterFormComponent);
    const component = fixture.componentInstance as unknown as {
      form: any;
      confirmPasswordError: () => string;
      attemptedSubmit: { set(value: boolean): void };
    };

    component.form.patchValue({
      firstName: 'Hridoy',
      lastName: 'Bhuiyan',
      email: 'hridoy.bhuiyan12@example.com',
      phone: '+1234567890',
      password: 'StrongP@ssw0rd!',
      confirmPassword: 'DifferentP@ssw0rd!',
    });
    component.form.controls.confirmPassword.markAsTouched();
    component.attemptedSubmit.set(true);
    fixture.detectChanges();

    const button = fixture.nativeElement.querySelector('[data-testid="register-submit"]') as HTMLButtonElement;
    expect(component.form.hasError('mismatch')).toBe(true);
    expect(component.confirmPasswordError()).toBe('Password and confirm password do not match.');
    expect(button.disabled).toBe(true);
  });

  it('emits successful register payloads with confirmPassword', () => {
    TestBed.configureTestingModule({
      imports: [RegisterFormComponent],
      providers: [providePlatformIcons()],
    });
    const fixture = TestBed.createComponent(RegisterFormComponent);
    const component = fixture.componentInstance as unknown as {
      form: any;
      submitted: { subscribe(callback: (value: unknown) => void): void };
      submit: () => void;
    };
    let emitted: unknown;
    component.submitted.subscribe((value) => {
      emitted = value;
    });

    component.form.patchValue({
      firstName: ' Hridoy ',
      lastName: ' Bhuiyan ',
      email: 'hridoy.bhuiyan12@example.com',
      phone: '+1234567890',
      workspaceName: '',
      password: 'StrongP@ssw0rd!',
      confirmPassword: 'StrongP@ssw0rd!',
    });
    component.submit();

    expect(emitted).toEqual({
      firstName: 'Hridoy',
      lastName: 'Bhuiyan',
      email: 'hridoy.bhuiyan12@example.com',
      phone: '+1234567890',
      workspaceName: null,
      password: 'StrongP@ssw0rd!',
      confirmPassword: 'StrongP@ssw0rd!',
    });
  });

  it('register service sends confirmPassword', async () => {
    const api = {
      post: vi.fn().mockReturnValue(
        of({
          success: true,
          message: 'Request completed successfully',
          errors: [],
          timestamp: new Date().toISOString(),
          data: {
            accessToken: 'access',
            accessTokenExpiresAt: new Date().toISOString(),
            refreshToken: 'refresh',
            refreshTokenExpiresAt: new Date().toISOString(),
            user: {
              id: 'user-1',
              firstName: 'Hridoy',
              lastName: 'Bhuiyan',
              email: 'hridoy.bhuiyan12@example.com',
              phone: '+1234567890',
              role: 'ADMIN',
              status: 'ACTIVE',
              emailVerified: false,
              lastLoginAt: null,
              createdAt: new Date().toISOString(),
              updatedAt: new Date().toISOString(),
              permissions: [],
              workspaceId: 'workspace-1',
            },
          },
        }),
      ),
    };

    TestBed.configureTestingModule({
      providers: [AuthApiService, { provide: ApiService, useValue: api }],
    });

    await TestBed.inject(AuthApiService).register({
      firstName: 'Hridoy',
      lastName: 'Bhuiyan',
      email: 'hridoy.bhuiyan12@example.com',
      phone: '+1234567890',
      password: 'StrongP@ssw0rd!',
      confirmPassword: 'StrongP@ssw0rd!',
    });

    expect(api.post).toHaveBeenCalledWith(
      '/api/v1/auth/register',
      {
        firstName: 'Hridoy',
        lastName: 'Bhuiyan',
        email: 'hridoy.bhuiyan12@example.com',
        phone: '+1234567890',
        password: 'StrongP@ssw0rd!',
        confirmPassword: 'StrongP@ssw0rd!',
      },
      expect.any(Object),
    );
  });
});
