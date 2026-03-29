import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { DataService, User } from '../../../services/data.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.css']
})
export class UserManagementComponent implements OnInit, OnDestroy {
  users: User[] = [];
  loading = false;
  error = '';
  success = '';
  showForm = false;
  showPasswordForm = false;
  editingUser: User | null = null;
  userForm!: FormGroup;
  passwordForm!: FormGroup;

  availableRoles = ['ROLE_USER', 'ROLE_ADMIN'];
  private destroy$ = new Subject<void>();
  private successTimeout: any;

  constructor(
    private dataService: DataService,
    private formBuilder: FormBuilder
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadUsers();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    if (this.successTimeout) {
      clearTimeout(this.successTimeout);
    }
  }

  initForm(): void {
    this.userForm = this.formBuilder.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: [{ value: '', disabled: true }],
      roles: [[], Validators.required],
      enabled: [true]
    });

    this.passwordForm = this.formBuilder.group({
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required]
    }, { validator: this.passwordMatchValidator });
  }

  passwordMatchValidator(g: FormGroup) {
    return g.get('password')?.value === g.get('confirmPassword')?.value
      ? null : { 'mismatch': true };
  }

  loadUsers(): void {
    this.loading = true;
    this.error = '';
    this.dataService.adminGetAllUsers()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.users = data;
          this.loading = false;
        },
        error: (error) => {
          this.error = 'Failed to load users';
          this.loading = false;
        }
      });
  }

  openForm(user?: User): void {
    if (user) {
      this.editingUser = user;
      this.userForm.patchValue({
        firstName: user.firstName,
        lastName: user.lastName,
        email: user.email,
        roles: user.roles.map(r => r.name || r),
        enabled: user.enabled
      });
    } else {
      this.editingUser = null;
      this.userForm.reset({ enabled: true, roles: ['ROLE_USER'] });
    }
    this.showForm = true;
  }

  onRoleChange(role: string, checked: boolean): void {
    const roles = this.userForm.get('roles')?.value as string[];
    if (checked) {
      if (!roles.includes(role)) {
        this.userForm.get('roles')?.setValue([...roles, role]);
      }
    } else {
      this.userForm.get('roles')?.setValue(roles.filter(r => r !== role));
    }
  }

  openPasswordForm(user: User): void {
    this.editingUser = user;
    this.passwordForm.reset();
    this.showPasswordForm = true;
  }

  closeForm(): void {
    this.showForm = false;
    this.showPasswordForm = false;
    this.editingUser = null;
    this.userForm.reset();
    this.passwordForm.reset();
  }

  private showSuccess(message: string): void {
    this.success = message;
    if (this.successTimeout) {
      clearTimeout(this.successTimeout);
    }
    this.successTimeout = setTimeout(() => this.success = '', 3000);
  }

  onSubmit(): void {
    if (this.userForm.invalid || !this.editingUser) {
      return;
    }

    this.loading = true;
    this.error = '';

    const formValue = this.userForm.getRawValue();
    const updatedUser: Partial<User> = {
      firstName: formValue.firstName,
      lastName: formValue.lastName,
      enabled: formValue.enabled,
      roles: formValue.roles.map((roleName: string) => ({ name: roleName }))
    };

    this.dataService.adminUpdateUser(this.editingUser.id, updatedUser)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          // Optimistic update: Update local user in array instead of reloading
          const index = this.users.findIndex(u => u.id === this.editingUser?.id);
          if (index !== -1) {
            this.users[index] = { ...this.users[index], ...updatedUser };
          }
          this.loading = false;
          this.showSuccess('User updated successfully');
          this.closeForm();
        },
        error: () => {
          this.error = 'Failed to update user';
          this.loading = false;
        }
      });
  }

  onPasswordSubmit(): void {
    if (this.passwordForm.invalid || !this.editingUser) {
      return;
    }

    this.loading = true;
    this.error = '';

    this.dataService.adminChangePassword(this.editingUser.id, this.passwordForm.value.password)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.loading = false;
          this.showSuccess('Password changed successfully');
          this.closeForm();
        },
        error: () => {
          this.error = 'Failed to change password';
          this.loading = false;
        }
      });
  }

  toggleStatus(user: User): void {
    this.dataService.adminToggleUserStatus(user.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          // Optimistic update: Update locally without reloading
          user.enabled = !user.enabled;
          this.showSuccess(`User ${user.enabled ? 'enabled' : 'disabled'} successfully`);
        },
        error: () => {
          this.error = 'Failed to toggle user status';
        }
      });
  }

  deleteUser(id: number, name: string): void {
    if (confirm(`Are you sure you want to delete ${name}?`)) {
      this.dataService.adminDeleteUser(id)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            // Remove user from local array instead of reloading
            this.users = this.users.filter(u => u.id !== id);
            this.showSuccess('User deleted successfully');
          },
          error: () => {
            this.error = 'Failed to delete user';
          }
        });
    }
  }

  get f() {
    return this.userForm.controls;
  }
}
