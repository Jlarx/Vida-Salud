import axios from 'axios';
import { PublicClientApplication } from '@azure/msal-browser';
import { msalConfig, loginRequest } from './authConfig';

const msalInstance = new PublicClientApplication(msalConfig);
await msalInstance.initialize();

const API_BASE_URL = 'https://zzyt3obhvf.execute-api.us-east-1.amazonaws.com/prod';

const apiClient = axios.create({
    baseURL: API_BASE_URL
});

apiClient.interceptors.request.use(async (config) => {
    try {
        const activeAccount = msalInstance.getActiveAccount() || msalInstance.getAllAccounts()[0];

        if (activeAccount) {
            const tokenResponse = await msalInstance.acquireTokenSilent({
                scopes: ["api://09d40f2a-29ea-43c0-8d74-b89ba284c778/.default"],
                account: activeAccount
            });

            // Adjuntar Bearer Token
            config.headers.Authorization = `Bearer ${tokenResponse.accessToken}`;
        }
    } catch (error) {
        console.error("Error al obtener token silencioso:", error);
        alert("Error de MSAL al pedir token: " + error.message);
        // Si el token expira o falla silenciosamente, el usuario debería volver a hacer login
    }

    return config;
}, (error) => {
    return Promise.reject(error);
});

export default apiClient;
