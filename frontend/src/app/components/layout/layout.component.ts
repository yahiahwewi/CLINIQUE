import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService, AuthResponse } from '../../services/auth.service';
import { InboxService } from '../../services/inbox.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { BrandMarkComponent } from './brand-mark.component';
import { NotificationBellComponent } from './notification-bell.component';
import { AnnouncementBannerComponent } from './announcement-banner.component';

interface NavItem {
  label: string;
  route: string;
  icon: string;
  roles?: string[];
}

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, BrandMarkComponent, NotificationBellComponent, AnnouncementBannerComponent],
  templateUrl: './layout.component.html'
})
export class LayoutComponent implements OnInit, OnDestroy {
  currentUser: AuthResponse | null = null;
  collapsed = signal(false);
  unread = signal(0);
  private destroy$ = new Subject<void>();

  readonly navItems: NavItem[] = [
    { label: 'Overview',       route: '/dashboard/overview',                icon: 'home' },
    { label: 'Appointments',   route: '/dashboard/appointments',            icon: 'calendar' },
    { label: 'My profile',     route: '/dashboard/profile/patient',         icon: 'id',        roles: ['ROLE_USER'] },
    { label: 'My history',     route: '/dashboard/history',                 icon: 'book',      roles: ['ROLE_USER'] },
    { label: 'My profile',     route: '/dashboard/profile/doctor',          icon: 'id',        roles: ['ROLE_DOCTOR'] },
    { label: 'Availability',   route: '/dashboard/availability',            icon: 'clock',     roles: ['ROLE_DOCTOR'] },
    { label: 'Time off',       route: '/dashboard/time-off',                icon: 'palm',      roles: ['ROLE_DOCTOR'] },
    { label: 'Referrals',      route: '/dashboard/referrals',               icon: 'arrow',     roles: ['ROLE_DOCTOR'] },
    { label: 'Approvals',      route: '/dashboard/admin/approvals',         icon: 'check',     roles: ['ROLE_ADMIN'] },
    { label: 'Time-off',       route: '/dashboard/admin/time-off',          icon: 'palm',      roles: ['ROLE_ADMIN'] },
    { label: 'Accounts',       route: '/dashboard/admin/users',             icon: 'users',     roles: ['ROLE_ADMIN'] },
    { label: 'Departments',    route: '/dashboard/admin/departments',       icon: 'grid',      roles: ['ROLE_ADMIN'] },
    { label: 'Holidays',       route: '/dashboard/admin/holidays',          icon: 'calendar',  roles: ['ROLE_ADMIN'] },
    { label: 'Announcements',  route: '/dashboard/admin/announcements',     icon: 'megaphone', roles: ['ROLE_ADMIN'] },
    { label: 'Audit log',      route: '/dashboard/admin/audit',             icon: 'shield',    roles: ['ROLE_ADMIN'] }
  ];

  constructor(
    private authService: AuthService,
    private inboxService: InboxService,
    public router: Router
  ) {}

  ngOnInit(): void {
    this.authService.currentUser$.pipe(takeUntil(this.destroy$)).subscribe(user => {
      this.currentUser = user;
    });
    this.inboxService.startPolling();
    this.inboxService.unreadCount$.pipe(takeUntil(this.destroy$)).subscribe(count => this.unread.set(count));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.inboxService.stopPolling();
  }

  toggleCollapsed(): void {
    this.collapsed.update(v => !v);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  visibleNav(): NavItem[] {
    return this.navItems.filter(item =>
      !item.roles || item.roles.some(r => this.currentUser?.roles.includes(r))
    );
  }

  primaryRole(): string {
    if (!this.currentUser?.roles?.length) return 'Member';
    const r = this.currentUser.roles[0];
    return r.replace('ROLE_', '').toLowerCase();
  }

  initials(): string {
    if (!this.currentUser) return '·';
    return ((this.currentUser.firstName?.[0] ?? '') + (this.currentUser.lastName?.[0] ?? '')).toUpperCase() || '·';
  }
}
