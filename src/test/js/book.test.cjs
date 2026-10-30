const {test} = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
const source = fs.readFileSync('src/main/resources/static/js/book.js', 'utf8');
const response = (data, ok = true) => ({ok, json: async () => data});
const doctors = [{id: 7, name: '<Doctor>', specialization: 'General'}];
const booked = {appointmentId: 20, tokenNumber: 3, estimatedWaitingTime: 30, status: 'BOOKED'};
function setup({stored = {patientId: '4', doctorId: '7'}, replies = [response(doctors), response(booked)]} = {}) {
    const elements = Object.fromEntries(['bookingForm', 'doctor', 'priority', 'bookButton', 'bookingMessage'].map(id => [id, {
        value: id === 'priority' ? 'NORMAL' : '', textContent: '', disabled: ['doctor', 'bookButton'].includes(id), children: [],
        addEventListener(type, listener) { this[type] = listener; },
        replaceChildren() { this.children = []; this.value = ''; },
        appendChild(child) { this.children.push(child); }
    }]));
    const calls = [];
    const context = vm.createContext({console, Set, Number,
        localStorage: {getItem: key => stored[key] ?? null},
        window: {location: {href: ''}},
        document: {getElementById: id => elements[id], createElement: () => ({value: '', textContent: ''})},
        apiFetch: async (...args) => {
            calls.push(args); const reply = replies.shift();
            if (reply instanceof Error) throw reply;
            return typeof reply === 'function' ? reply() : reply;
        }
    });
    vm.runInContext(source, context);
    return {context, elements, calls, ready: vm.runInContext('bookingReady', context)};
}
test('page provides the form controls and loads helper before booking script', () => {
    const html = fs.readFileSync('src/main/resources/static/pages/book-appointment.html', 'utf8');
    for (const id of ['bookingForm', 'doctor', 'priority', 'bookButton', 'bookingMessage']) assert.ok(html.includes(`id="${id}"`));
    assert.ok(html.indexOf('/js/api.js') < html.indexOf('/js/book.js'));
});
test('loads doctors safely and preserves the doctor selected on the doctors page', async () => {
    const {elements, ready, calls} = setup(); await ready;
    assert.equal(calls[0][0], '/api/doctors');
    assert.equal(elements.doctor.value, '7');
    assert.equal(elements.doctor.children[1].textContent, '<Doctor> — General');
    assert.equal(elements.bookButton.disabled, false);
});
test('submits the patient, doctor and priority and shows the real token', async () => {
    const {elements, ready, calls} = setup(); await ready;
    elements.priority.value = 'EMERGENCY';
    let prevented = false;
    await elements.bookingForm.submit({preventDefault() {prevented = true;}});
    assert.equal(prevented, true);
    assert.equal(calls[1][0], '/api/appointments');
    assert.equal(calls[1][1].method, 'POST');
    assert.deepEqual(JSON.parse(calls[1][1].body), {patientId: 4, doctorId: 7, priority: 'EMERGENCY'});
    assert.match(elements.bookingMessage.textContent, /Token: 3/);
    assert.match(elements.bookingMessage.textContent, /30 minutes/);
    assert.equal(elements.bookButton.disabled, true);
});
test('missing patient redirects before any API request', async () => {
    const {context, calls, ready} = setup({stored: {patientId: 'null'}}); await ready;
    assert.equal(context.window.location.href, '/pages/login.html'); assert.equal(calls.length, 0);
});
test('missing doctor or invalid priority never submits', async () => {
    const {context, elements, calls, ready} = setup({stored: {patientId: '4', doctorId: '99'}}); await ready;
    await context.bookAppointment(); assert.equal(calls.length, 1);
    elements.doctor.value = '7'; elements.priority.value = 'INVALID';
    await context.bookAppointment(); assert.equal(calls.length, 1);
});
test('no doctors and doctor-load failures keep the form disabled', async () => {
    for (const reply of [response([]), response({message: 'Unavailable'}, false), new Error('Network unavailable')]) {
        const {elements, ready} = setup({replies: [reply]}); await ready;
        assert.equal(elements.bookButton.disabled, true);
        assert.ok(elements.bookingMessage.textContent.length > 0);
    }
});
test('HTTP booking errors display the server message and allow retry', async () => {
    const {context, elements, ready} = setup({replies: [response(doctors), response({message: 'Not allowed'}, false)]}); await ready;
    await context.bookAppointment();
    assert.equal(elements.bookingMessage.textContent, 'Not allowed');
    assert.equal(elements.bookButton.disabled, false);
});
test('in-flight and completed bookings cannot be submitted twice', async () => {
    let resolve;
    const pending = new Promise(done => {resolve = done;});
    const {context, calls, ready} = setup({replies: [response(doctors), () => pending]}); await ready;
    const first = context.bookAppointment(); await context.bookAppointment();
    assert.equal(calls.length, 2); resolve(response(booked)); await first;
    await context.bookAppointment(); assert.equal(calls.length, 2);
});
test('expired session errors do not show booking success', async () => {
    const {context, elements, ready} = setup({replies: [response(doctors), new Error('Please sign in again')]}); await ready;
    await context.bookAppointment();
    assert.equal(elements.bookingMessage.textContent, 'Please sign in again');
    assert.notEqual(elements.bookButton.textContent, 'Appointment booked');
});
test('malformed successful response asks user to check appointments', async () => {
    const {context, elements, ready} = setup({replies: [response(doctors), response({})]}); await ready;
    await context.bookAppointment();
    assert.match(elements.bookingMessage.textContent, /Check My appointments/);
    assert.notEqual(elements.bookButton.textContent, 'Appointment booked');
});
