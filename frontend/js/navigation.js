/**
 * Common Navigation and Session Guard for SecureBank
 */
const Navigation = {
  init(activePage) {
    // 1. Session Guard: Check if authenticated
    if (!ApiService.isAuthenticated()) {
      window.location.href = 'index.html?error=unauthorized';
      return;
    }

    // 2. Display demo banner if DEMO_MODE is active
    if (CONFIG.DEMO_MODE) {
      const banner = document.getElementById('demo-banner');
      if (banner) {
        banner.style.display = 'block';
        banner.textContent = 'DEMO MODE — Sample Data Active. Real banking operations disabled.';
      }
    }

    // 3. Populate username in header & sidebar
    const username = ApiService.getCurrentUser();
    const userElements = document.querySelectorAll('.js-username');
    userElements.forEach(el => { el.textContent = username; });

    const avatarInitial = document.querySelector('.js-avatar-initial');
    if (avatarInitial) {
      avatarInitial.textContent = username.charAt(0).toUpperCase();
    }

    // 4. Mark active navigation item
    const navLinks = document.querySelectorAll('.nav-link');
    navLinks.forEach(link => {
      if (link.getAttribute('data-page') === activePage) {
        link.classList.add('active');
      } else {
        link.classList.remove('active');
      }
    });

    // 5. Setup mobile sidebar toggle
    const toggleBtn = document.getElementById('menu-toggle');
    const sidebar = document.querySelector('.sidebar');
    if (toggleBtn && sidebar) {
      toggleBtn.addEventListener('click', () => {
        sidebar.classList.toggle('open');
      });
    }

    // 6. Setup logout button
    const logoutBtn = document.getElementById('btn-logout');
    if (logoutBtn) {
      logoutBtn.addEventListener('click', (e) => {
        e.preventDefault();
        ApiService.clearAuth();
        window.location.href = 'index.html?msg=logged_out';
      });
    }
  }
};
