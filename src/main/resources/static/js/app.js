// Antigravity Spring Boot Text File DB App Scripts

document.addEventListener('DOMContentLoaded', () => {
  // 1. Quick demo login fill
  const fillDemoBtn = document.getElementById('btn-fill-demo');
  if (fillDemoBtn) {
    fillDemoBtn.addEventListener('click', () => {
      const emailInput = document.getElementById('login-email');
      const passInput = document.getElementById('login-password');
      if (emailInput && passInput) {
        emailInput.value = 'demo@example.com';
        passInput.value = 'Password123!';
        
        // Highlight inputs briefly
        emailInput.style.borderColor = '#6366F1';
        passInput.style.borderColor = '#6366F1';
        setTimeout(() => {
          emailInput.style.borderColor = '';
          passInput.style.borderColor = '';
        }, 800);
      }
    });
  }

  // 2. Auto-fade alert banners after 5 seconds
  const alerts = document.querySelectorAll('.alert');
  alerts.forEach(alert => {
    setTimeout(() => {
      alert.style.transition = 'opacity 0.5s ease, transform 0.5s ease';
      alert.style.opacity = '0';
      alert.style.transform = 'translateY(-10px)';
      setTimeout(() => alert.remove(), 500);
    }, 5000);
  });

  // 3. Raw Database Inspector Tab switcher
  const tabUsers = document.getElementById('tab-btn-users');
  const tabRecords = document.getElementById('tab-btn-records');
  const codeViewer = document.getElementById('raw-file-code');
  const filePathDisplay = document.getElementById('inspector-file-path');
  const copyBtn = document.getElementById('btn-copy-raw');

  function loadRawFile(filename) {
    if (!codeViewer) return;
    codeViewer.textContent = 'Loading ' + filename + ' from disk...';
    
    fetch('/dashboard/api/raw-file?file=' + encodeURIComponent(filename))
      .then(res => res.json())
      .then(data => {
        if (data.content !== undefined) {
          codeViewer.textContent = data.content.trim() || '/* File is currently empty */';
          if (filePathDisplay) {
            filePathDisplay.textContent = data.path || filename;
          }
        } else {
          codeViewer.textContent = 'Could not load file content.';
        }
      })
      .catch(err => {
        codeViewer.textContent = 'Error loading file: ' + err.message;
      });
  }

  if (tabUsers && tabRecords) {
    tabUsers.addEventListener('click', () => {
      tabUsers.classList.add('active');
      tabRecords.classList.remove('active');
      loadRawFile('users.txt');
    });

    tabRecords.addEventListener('click', () => {
      tabRecords.classList.add('active');
      tabUsers.classList.remove('active');
      loadRawFile('user_records.txt');
    });

    // Load initial tab on page load
    loadRawFile('user_records.txt');
  }

  if (copyBtn && codeViewer) {
    copyBtn.addEventListener('click', () => {
      navigator.clipboard.writeText(codeViewer.textContent).then(() => {
        const originalText = copyBtn.textContent;
        copyBtn.textContent = 'Copied!';
        setTimeout(() => {
          copyBtn.textContent = originalText;
        }, 2000);
      });
    });
  }
});
