import { useState } from 'react';
import { useMsal, AuthenticatedTemplate, UnauthenticatedTemplate } from "@azure/msal-react";
import { loginRequest } from "../authConfig";
import './Login.css';
import '../dashboards/Dashboards.css';
import AdminDashboard from '../dashboards/AdminDashboard';
import PatientDashboard from '../dashboards/PatientDashboard';
import ReceptionDashboard from '../dashboards/ReceptionDashboard';
import AuditDashboard from '../dashboards/AuditDashboard';

export default function Login() {
  const { instance, accounts } = useMsal();
  const [activeMenu, setActiveMenu] = useState('');
  
  const activeAccount = accounts.length > 0 ? accounts[0] : null;
  const roles = activeAccount?.idTokenClaims?.roles || [];
  const userName = activeAccount?.name || "Usuario";

  // Determinar pestaña activa inicial por defecto
  if (!activeMenu && roles.length > 0) {
    if (roles.includes('Administrador')) setActiveMenu('Admin');
    else if (roles.includes('Recepcionista')) setActiveMenu('Recepcion');
    else if (roles.includes('Paciente')) setActiveMenu('Paciente');
    else if (roles.includes('Auditor')) setActiveMenu('Auditoria');
  }

  const handleLogin = () => {
    instance.loginRedirect(loginRequest).catch(e => console.error(e));
  };

  const handleLogout = () => {
    instance.logoutRedirect({ postLogoutRedirectUri: "/" });
  };

  return (
    <div className="login-wrapper" style={{ padding: 0, margin: 0, background: 'var(--bg-color)' }}>
      <UnauthenticatedTemplate>
        <div className="login-card" style={{ margin: 'auto', marginTop: '15vh' }}>
          <div className="logo-placeholder">
            <span className="logo-icon">VS</span>
            <h2>VidaSalud</h2>
          </div>
          <p className="login-subtitle">Acceso seguro con Microsoft Entra ID</p>
          <button onClick={handleLogin} className="ms-login-btn">
            <svg xmlns="http://www.w3.org/2000/svg" width="21" height="21" viewBox="0 0 21 21">
              <rect x="1" y="1" width="9" height="9" fill="#f25022" />
              <rect x="1" y="11" width="9" height="9" fill="#00a4ef" />
              <rect x="11" y="1" width="9" height="9" fill="#7fba00" />
              <rect x="11" y="11" width="9" height="9" fill="#ffb900" />
            </svg>
            <span>Iniciar sesión</span>
          </button>
        </div>
      </UnauthenticatedTemplate>

      <AuthenticatedTemplate>
        <div className="app-layout">
          {/* Sidebar */}
          <aside className="sidebar">
            <div className="sidebar-header">
              <div className="logo-circle">VS</div>
              <h2>VidaSalud</h2>
            </div>
            
            <div className="sidebar-menu">
              {roles.includes('Administrador') && (
                <div 
                  className={`menu-item ${activeMenu === 'Admin' ? 'active' : ''}`}
                  onClick={() => setActiveMenu('Admin')}
                >
                  Servicios Médicos
                </div>
              )}
              {roles.includes('Recepcionista') && (
                <div 
                  className={`menu-item ${activeMenu === 'Recepcion' ? 'active' : ''}`}
                  onClick={() => setActiveMenu('Recepcion')}
                >
                  Gestión de Citas
                </div>
              )}
              {roles.includes('Paciente') && (
                <div 
                  className={`menu-item ${activeMenu === 'Paciente' ? 'active' : ''}`}
                  onClick={() => setActiveMenu('Paciente')}
                >
                  Mis Citas
                </div>
              )}
              {roles.includes('Auditor') && (
                <div 
                  className={`menu-item ${activeMenu === 'Auditoria' ? 'active' : ''}`}
                  onClick={() => setActiveMenu('Auditoria')}
                >
                  Auditoría
                </div>
              )}
            </div>

            <div className="sidebar-footer">
              <button onClick={handleLogout} className="logout-btn">
                Cerrar Sesión
              </button>
            </div>
          </aside>

          {/* Main Content */}
          <main className="main-content">
            {/* Topbar */}
            <header className="top-header">
              <div className="user-welcome">
                <h1>Bienvenido, {userName}</h1>
                <div className="roles-container">
                  {roles.length > 0 ? (
                    roles.map(rol => <span key={rol} className="role-badge">{rol}</span>)
                  ) : (
                    <span className="role-badge" style={{ background: '#f8d7da', color: '#721c24' }}>Sin Acceso</span>
                  )}
                </div>
              </div>
            </header>

            {/* Dashboard Views */}
            <div className="dashboard-view">
              {activeMenu === 'Admin' && roles.includes('Administrador') && <AdminDashboard />}
              {activeMenu === 'Recepcion' && roles.includes('Recepcionista') && <ReceptionDashboard />}
              {activeMenu === 'Paciente' && roles.includes('Paciente') && <PatientDashboard />}
              {activeMenu === 'Auditoria' && roles.includes('Auditor') && <AuditDashboard />}
              
              {roles.length === 0 && (
                <div className="card-white">
                  <h3 style={{ color: '#721c24' }}>Acceso Denegado</h3>
                  <p>Tu cuenta no tiene ningún rol asignado en el sistema VidaSalud.</p>
                </div>
              )}
            </div>
          </main>
        </div>
      </AuthenticatedTemplate>
    </div>
  );
}
