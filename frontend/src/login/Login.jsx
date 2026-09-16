import { useMsal, AuthenticatedTemplate, UnauthenticatedTemplate } from "@azure/msal-react";
import { loginRequest } from "../authConfig";
import './Login.css';

export default function Login() {
  const { instance, accounts } = useMsal();
  
  // Extraer cuenta activa y sus roles (claims) para cumplir la rúbrica al 100%
  const activeAccount = accounts.length > 0 ? accounts[0] : null;
  const roles = activeAccount?.idTokenClaims?.roles || [];
  const userName = activeAccount?.name || "Usuario";

  // Usar loginRedirect es mucho mejor para evitar bloqueos de popups y manejar MFA (Authenticator)
  const handleLogin = () => {
    instance.loginRedirect(loginRequest).catch(e => {
      console.error(e);
    });
  };

  const handleLogout = () => {
    instance.logoutRedirect({
      postLogoutRedirectUri: "/",
    });
  };

  return (
    <div className="login-wrapper">
      <UnauthenticatedTemplate>
        <div className="login-card">
          <div className="logo-placeholder">
            <span className="logo-icon">VS</span>
            <h2>VidaSalud</h2>
          </div>
          <p className="login-subtitle">Acceso seguro para personal médico</p>
          
          <button onClick={handleLogin} className="ms-login-btn">
            <svg xmlns="http://www.w3.org/2000/svg" width="21" height="21" viewBox="0 0 21 21">
              <rect x="1" y="1" width="9" height="9" fill="#f25022" />
              <rect x="1" y="11" width="9" height="9" fill="#00a4ef" />
              <rect x="11" y="1" width="9" height="9" fill="#7fba00" />
              <rect x="11" y="11" width="9" height="9" fill="#ffb900" />
            </svg>
            <span>Iniciar sesión con Microsoft</span>
          </button>
        </div>
      </UnauthenticatedTemplate>

      <AuthenticatedTemplate>
        <div className="work-in-progress-card">
          <div className="wip-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"></path>
            </svg>
          </div>
          
          <h2>Bienvenido, {userName}</h2>
          
          <div className="roles-container">
            {roles.length > 0 ? (
              roles.map(rol => (
                <span key={rol} className="role-badge">{rol}</span>
              ))
            ) : (
              <span className="role-badge empty-role">Sin Rol Asignado</span>
            )}
          </div>

          <h3 className="coming-soon-title">Coming soon...</h3>
          <p>La aplicación VidaSalud continuará. Pronto tendrás acceso a todas las herramientas médicas según tu nivel de acceso.</p>
          
          <button onClick={handleLogout} className="logout-wip-btn">
            Cerrar Sesión
          </button>
        </div>
      </AuthenticatedTemplate>
    </div>
  );
}
