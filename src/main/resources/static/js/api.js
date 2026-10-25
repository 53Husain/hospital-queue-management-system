// All API calls use the same origin and the HttpOnly session cookie.
async function apiFetch(path, options = {}) {
    const headers = new Headers(options.headers || {});
    const method = (options.method || "GET").toUpperCase();
    if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
        const response = await fetch("/api/auth/csrf", {credentials: "same-origin", cache: "no-store"});
        if (!response.ok) throw new Error("Unable to obtain CSRF token");
        const csrf = await response.json();
        headers.set(csrf.headerName, csrf.token);
    }
    const response = await fetch(path, {...options, headers, credentials: "same-origin"});
    if (response.status === 401 && path !== "/api/auth/login") {
        localStorage.clear();
        window.location.href = "/pages/login.html";
        throw new Error("Please sign in again");
    }
    return response;
}

async function logout() {
    try {
        const response = await apiFetch("/api/auth/logout", {method: "POST"});
        if (!response.ok) throw new Error("Logout failed");
        localStorage.clear();
        window.location.href = "/pages/login.html";
    } catch (error) {
        alert("Unable to sign out. Please retry.");
    }
}

// Escape database text before inserting it into the existing HTML templates.
function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>"']/g, character => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
    })[character]);
}
