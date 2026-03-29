import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { DataService, User, UserCreateRequest } from '../../../services/data.service';
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
  availableRoles = ['ROLE_USER', 'ROLE_DOCTOR', 'ROLE_NURSE', 'ROLE_ADMIN'];
  private destroy$ = new Subject<void>();

  constructor(
    private dataService: DataService,
    private formBuilder: FormBuilder
  ) {}

  ngOnInit(): void {
    this.initForms();
    this.loadUsers();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  initForms(): void {
    this.userForm = this.formBuilder.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: [''],
      roles: [['ROLE_USER'], Validators.required],
      enabled: [true]
    });

    this.passwordForm = this.formBuilder.group({
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required]
    }, { validators: this.passwordMatchValidator });
  }

  passwordMatchValidator(group: FormGroup) {
    return group.get('password')?.value === group.get('confirmPassword')?.value
      ? null
      : { mismatch: true };
  }

  loadUsers(): void {
    this.loading = true;
    this.error = '';

    this.dataService.adminGetAllUsers()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: users => {
          this.users = users;
          this.loading = false;
        },
        error: () => {
          this.error = 'Failed to load users';
          this.loading = false;
        }
      });
  }

  openCreateForm(): void {
    this.editingUser = null;
    this.userForm.reset({
      firstName: '',
      lastName: '',
      email: '',
      password: '',
      roles: ['ROLE_USER'],
      enabled: true
    });
    this.userForm.get('password')?.setValidators([Validators.required, Validators.minLength(6)]);
    this.userForm.get('password')?.updateValueAndValidity();
    this.userForm.get('email')?.enable();
    this.showForm = true;
  }

  openEditForm(user: User): void {
    this.editingUser = user;
    this.userForm.reset({
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      password: '',
      roles: user.roles.map(role => role.name),
      enabled: user.enabled
    });
    this.userForm.get('password')?.clearValidators();
    this.userForm.get('password')?.updateValueAndValidity();
    this.userForm.get('email')?.disable();
    this.showForm = true;
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
    this.userForm.reset({ roles: ['ROLE_USER'], enabled: true });
    this.passwordForm.reset();
  }

  onRoleChange(role: string, checked: boolean): void {
    const roles = [...(this.userForm.get('roles')?.value || [])];
    const nextRoles = checked
      ? Array.from(new Set([...roles, role]))
      : roles.filter((currentRole: string) => currentRole !== role);

    this.userForm.get('roles')?.setValue(nextRoles);
  }

  submitUser(): void {
    if (this.userForm.invalid) {
      this.userForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.error = '';
    this.success = '';

    const formValue = this.userForm.getRawValue();

    if (this.editingUser) {
      const payload = {
        firstName: formValue.firstName,
        lastName: formValue.lastName,
        enabled: formValue.enabled,
        roles: formValue.roles.map((role: string) => ({ name: role }))
      };

      this.dataService.adminUpdateUser(this.editingUser.id, payload)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (updatedUser) => {
            this.loading = false;
            this.success = 'User updated successfully';
            this.upsertUser(updatedUser);
            this.closeForm();
          },
          error: (error) => {
            this.loading = false;
            this.error = error?.error?.message || 'Failed to update user';
          }
        });
      return;
    }

    const createPayload: UserCreateRequest = {
      firstName: formValue.firstName,
      lastName: formValue.lastName,
      email: formValue.email,
      password: formValue.password,
      roles: formValue.roles,
      enabled: formValue.enabled
    };

    this.dataService.adminCreateUser(createPayload)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (createdUser) => {
          this.loading = false;
          this.success = 'User created successfully';
          this.upsertUser(createdUser);
          this.closeForm();
        },
        error: (error) => {
          this.loading = false;
          this.error = error?.error?.message || 'Failed to create user';
        }
      });
  }

  submitPassword(): void {
    if (this.passwordForm.invalid || !this.editingUser) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.error = '';

    this.dataService.adminChangePassword(this.editingUser.id, this.passwordForm.value.password)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.loading = false;
          this.success = 'Password updated successfully';
          this.closeForm();
        },
        error: (error) => {
          this.loading = false;
          this.error = error?.error?.message || 'Failed to update password';
        }
      });
  }

  toggleStatus(user: User): void {
    this.dataService.adminToggleUserStatus(user.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          user.enabled = !user.enabled;
          this.success = `User ${user.enabled ? 'enabled' : 'disabled'} successfully`;
        },
        error: (error) => {
          this.error = error?.error?.message || 'Failed to update user status';
        }
      });
  }

  deleteUser(user: User): void {
    if (!confirm(`Delete ${user.firstName} ${user.lastName}?`)) {
      return;
    }

    this.dataService.adminDeleteUser(user.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.success = 'User deleted successfully';
          this.users = this.users.filter(currentUser => currentUser.id !== user.id);
        },
        error: (error) => {
          this.error = error?.error?.message || 'Failed to delete user';
        }
      });
  }

  private upsertUser(user: User): void {
    const otherUsers = this.users.filter(currentUser => currentUser.id !== user.id);
    this.users = [...otherUsers, user].sort((left, right) =>
      `${left.firstName} ${left.lastName}`.localeCompare(`${right.firstName} ${right.lastName}`)
    );
  }
}
