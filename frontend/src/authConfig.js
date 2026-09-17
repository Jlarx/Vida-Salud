export const msalConfig = {
    auth: {
        clientId: "09d40f2a-29ea-43c0-8d74-b89ba284c778",
        authority: "https://login.microsoftonline.com/001834fd-b68a-4e6a-a718-6d859787b926", // Tenant ID
        redirectUri: "/", // Points to window.location.origin
        postLogoutRedirectUri: "/"
    },
    cache: {
        cacheLocation: "sessionStorage", // This configures where your cache will be stored
        storeAuthStateInCookie: false, // Set this to "true" if you are having issues on IE11 or Edge
    }
};

// Scopes required for login
export const loginRequest = {
    scopes: ["api://09d40f2a-29ea-43c0-8d74-b89ba284c778/.default"],
    prompt: "select_account"
};
