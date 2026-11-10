import { useState, useEffect } from 'react';
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
  
  const activeAccount = instance.getActiveAccount() || (accounts.length > 0 ? accounts[0] : null);
  const rawRoles = activeAccount?.idTokenClaims?.roles || [];
  const roles = Array.isArray(rawRoles) ? rawRoles : [rawRoles];
  const userName = activeAccount?.name || activeAccount?.username || "Usuario";

  const hasRole = (...targetRoles) => {
    return targetRoles.some(target => 
      roles.some(r => String(r).trim().toLowerCase() === target.toLowerCase())
    );
  };

  // Sincronizar cuenta activa en MSAL
  useEffect(() => {
    if (activeAccount && !instance.getActiveAccount()) {
      instance.setActiveAccount(activeAccount);
    }
  }, [activeAccount, instance]);

  // Determinar pestaña activa inicial por defecto
  useEffect(() => {
    if (!activeMenu && roles.length > 0) {
      if (hasRole('Administrador')) setActiveMenu('Admin');
      else if (hasRole('Operador', 'Recepcionista')) setActiveMenu('Recepcion');
      else if (hasRole('Cliente', 'Paciente')) setActiveMenu('Paciente');
      else if (hasRole('Auditor')) setActiveMenu('Auditoria');
    }
  }, [roles, activeMenu]);

  const handleLogin = () => {
    instance.loginRedirect(loginRequest).catch(e => console.error(e));
  };

  const handleLogout = () => {
    instance.logoutRedirect({ postLogoutRedirectUri: window.location.origin });
  };

  const canAccessAdmin = hasRole('Administrador');
  const canAccessReception = hasRole('Operador', 'Recepcionista', 'Administrador');
  const canAccessPatient = hasRole('Cliente', 'Paciente', 'Administrador');
  const canAccessAudit = hasRole('Auditor', 'Administrador');

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
              {canAccessAdmin && (
                <div 
                  className={`menu-item ${activeMenu === 'Admin' ? 'active' : ''}`}
                  onClick={() => setActiveMenu('Admin')}
                >
                  Servicios Médicos
                </div>
              )}
              {canAccessReception && (
                <div 
                  className={`menu-item ${activeMenu === 'Recepcion' ? 'active' : ''}`}
                  onClick={() => setActiveMenu('Recepcion')}
                >
                  Gestión de Citas
                </div>
              )}
              {canAccessPatient && (
                <div 
                  className={`menu-item ${activeMenu === 'Paciente' ? 'active' : ''}`}
                  onClick={() => setActiveMenu('Paciente')}
                >
                  Mis Citas
                </div>
              )}
              {canAccessAudit && (
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
                    roles.map((rol, idx) => <span key={idx} className="role-badge">{rol}</span>)
                  ) : (
                    <span className="role-badge" style={{ background: '#f8d7da', color: '#721c24' }}>Sin Acceso</span>
                  )}
                </div>
              </div>
            </header>

            {/* Dashboard Views */}
            <div className="dashboard-view">
              {activeMenu === 'Admin' && canAccessAdmin && <AdminDashboard />}
              {activeMenu === 'Recepcion' && canAccessReception && <ReceptionDashboard />}
              {activeMenu === 'Paciente' && canAccessPatient && <PatientDashboard />}
              {activeMenu === 'Auditoria' && canAccessAudit && <AuditDashboard />}
              
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
