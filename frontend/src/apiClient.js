import axios from 'axios';
import { msalInstance, loginRequest } from './authConfig';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'https://zzyt3obhvf.execute-api.us-east-1.amazonaws.com/prod';

const apiClient = axios.create({
    baseURL: API_BASE_URL
});

apiClient.interceptors.request.use(async (config) => {
    try {
        let activeAccount = msalInstance.getActiveAccount();
        if (!activeAccount) {
            const accounts = msalInstance.getAllAccounts();
            if (accounts.length > 0) {
                activeAccount = accounts[0];
                msalInstance.setActiveAccount(activeAccount);
            }
        }

        if (activeAccount) {
            const tokenResponse = await msalInstance.acquireTokenSilent({
                ...loginRequest,
                account: activeAccount
            });

            if (tokenResponse?.accessToken) {
                config.headers.Authorization = `Bearer ${tokenResponse.accessToken}`;
            }
        }
    } catch (error) {
        console.warn("Intento de token silencioso falló, reintentando con interacción si es necesario:", error);
        try {
            const tokenResponse = await msalInstance.acquireTokenPopup(loginRequest);
            if (tokenResponse?.accessToken) {
                config.headers.Authorization = `Bearer ${tokenResponse.accessToken}`;
            }
        } catch (popupError) {
            console.error("No se pudo obtener token de autenticación:", popupError);
        }
    }

    return config;
}, (error) => {
    return Promise.reject(error);
});

export default apiClient;
