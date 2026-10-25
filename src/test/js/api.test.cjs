const {test} = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
const source = fs.readFileSync('src/main/resources/static/js/api.js', 'utf8');
function setup(responses) {
    const calls = [];
    const context = vm.createContext({Headers, console,
        fetch: async (...args) => {calls.push(args); return responses.shift();},
        localStorage: {clear() {context.cleared = true;}},
        window: {location: {href: ''}}, alert: () => {context.alerted = true;}
    });
    vm.runInContext(source, context);
    return {context, calls};
}
const csrf = () => ({ok: true, json: async () => ({headerName: 'X-CSRF-TOKEN', token: 'test-token'})});
test('GET uses session credentials without requesting CSRF', async () => {
    const {context, calls} = setup([{status: 200}]);
    await context.apiFetch('/api/doctors');
    assert.equal(calls.length, 1);
    assert.equal(calls[0][1].credentials, 'same-origin');
});
test('writes fetch a current CSRF token and retain content type', async () => {
    const {context, calls} = setup([csrf(), {status: 200}]);
    await context.apiFetch('/api/appointments', {method: 'POST', headers: {'Content-Type': 'application/json'}, body: '{}'});
    assert.equal(calls[0][0], '/api/auth/csrf');
    assert.equal(calls[1][1].headers.get('X-CSRF-TOKEN'), 'test-token');
    assert.equal(calls[1][1].headers.get('Content-Type'), 'application/json');
});
test('logout contacts server before clearing local state', async () => {
    const {context, calls} = setup([csrf(), {ok: true, status: 204}]);
    await context.logout();
    assert.equal(calls[1][0], '/api/auth/logout');
    assert.equal(calls[1][1].method, 'POST');
    assert.equal(context.cleared, true);
    assert.equal(context.window.location.href, '/pages/login.html');
});
test('failed logout does not pretend the session ended', async () => {
    const {context} = setup([csrf(), {ok: false, status: 500}]);
    await context.logout();
    assert.equal(context.cleared, undefined);
    assert.equal(context.alerted, true);
});
test('expired session redirects, login rejection remains available to form', async () => {
    const {context} = setup([{status: 401}, csrf(), {status: 401}]);
    await assert.rejects(() => context.apiFetch('/api/doctors'));
    assert.equal(context.window.location.href, '/pages/login.html');
    assert.equal((await context.apiFetch('/api/auth/login', {method: 'POST'})).status, 401);
});
test('database strings cannot inject HTML into templates', () => {
    const {context} = setup([]);
    assert.equal(context.escapeHtml('<img src=x onerror="alert(1)">'), '&lt;img src=x onerror=&quot;alert(1)&quot;&gt;');
});
