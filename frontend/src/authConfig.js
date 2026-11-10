import { PublicClientApplication } from "@azure/msal-browser";

export const msalConfig = {
    auth: {
        clientId: "09d40f2a-29ea-43c0-8d74-b89ba284c778",
        authority: "https://login.microsoftonline.com/001834fd-b68a-4e6a-a718-6d859787b926", // Tenant ID
        redirectUri: typeof window !== "undefined" ? window.location.origin : "/",
        postLogoutRedirectUri: typeof window !== "undefined" ? window.location.origin : "/"
    },
    cache: {
        cacheLocation: "sessionStorage",
        storeAuthStateInCookie: false,
    }
};

// Scopes para el login y consumo de APIs protegidas
export const loginRequest = {
    scopes: ["api://09d40f2a-29ea-43c0-8d74-b89ba284c778/.default"],
    prompt: "select_account"
};

export const msalInstance = new PublicClientApplication(msalConfig);
