import { routes } from './app.routes';

describe('app routes', () => {
  it('includes password reset pages', () => {
    const paths = routes.map(route => route.path);

    expect(paths).toContain('forgot-password');
    expect(paths).toContain('reset-password');
  });

  it('redirects dashboard root to overview', () => {
    const dashboardRoute = routes.find(route => route.path === 'dashboard');
    const childRedirect = dashboardRoute?.children?.find(route => route.path === '');

    expect(childRedirect?.redirectTo).toBe('overview');
  });
});
