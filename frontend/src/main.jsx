import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App.jsx';
import './index.css';
import { MsalProvider } from "@azure/msal-react";
import { msalInstance } from "./authConfig";

msalInstance.initialize().then(() => {
  msalInstance.handleRedirectPromise()
    .then((response) => {
      if (response && response.account) {
        msalInstance.setActiveAccount(response.account);
      } else {
        const accounts = msalInstance.getAllAccounts();
        if (accounts.length > 0 && !msalInstance.getActiveAccount()) {
          msalInstance.setActiveAccount(accounts[0]);
        }
      }
      renderApp();
    })
    .catch((err) => {
      console.error("MSAL Redirect Error:", err);
      renderApp();
    });

  function renderApp() {
    ReactDOM.createRoot(document.getElementById('root')).render(
      <React.StrictMode>
        <MsalProvider instance={msalInstance}>
          <App />
        </MsalProvider>
      </React.StrictMode>
    );
  }
});
