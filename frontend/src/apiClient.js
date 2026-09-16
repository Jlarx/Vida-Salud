import axios from 'axios';
import { PublicClientApplication } from '@azure/msal-browser';
import { msalConfig, loginRequest } from './authConfig';

const msalInstance = new PublicClientApplication(msalConfig);
await msalInstance.initialize();

const apiClient = axios.create({
    baseURL: 'http://localhost:8080/api' // Cambiar según la API Gateway luego
});

apiClient.interceptors.request.use(async (config) => {
    try {
        const activeAccount = msalInstance.getActiveAccount() || msalInstance.getAllAccounts()[0];
        
        if (activeAccount) {
            const tokenResponse = await msalInstance.acquireTokenSilent({
                ...loginRequest,
                account: activeAccount
            });
            
            // Adjuntar Bearer Token
            config.headers.Authorization = `Bearer ${tokenResponse.accessToken}`;
        }
    } catch (error) {
        console.error("Error al obtener token silencioso:", error);
        // Si el token expira o falla silenciosamente, el usuario debería volver a hacer login
    }
    
    return config;
}, (error) => {
    return Promise.reject(error);
});

export default apiClient;
