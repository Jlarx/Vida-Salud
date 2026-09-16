import axios from 'axios';
import { PublicClientApplication } from '@azure/msal-browser';
import { msalConfig, loginRequest } from './authConfig';

const msalInstance = new PublicClientApplication(msalConfig);
await msalInstance.initialize();

// Cuando configures tu AWS API Gateway en el laboratorio, reemplaza esta URL
// por el 'Invoke URL' que te entregue Amazon (Ejemplo: https://xyz123.execute-api.us-east-1.amazonaws.com/prod)
const API_BASE_URL = 'http://REEMPLAZAR_POR_TU_AWS_API_GATEWAY_URL';

const apiClient = axios.create({
    baseURL: API_BASE_URL
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
