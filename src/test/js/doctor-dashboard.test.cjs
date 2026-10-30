const {test} = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
const source = fs.readFileSync('src/main/resources/static/js/doctor-dashboard.js', 'utf8');
const response = (data, ok = true) => ({ok, json: async () => data, text: async () => String(data)});
function setup(replies) {
    const alerts = [], calls = [];
    const elements = {doctorName: {}, queueTable: {}};
    const context = vm.createContext({
        localStorage: {getItem: key => key === 'doctorId' ? '7' : 'Doctor'},
        document: {getElementById: id => elements[id]},
        apiFetch: async (...args) => { calls.push(args); return replies.shift(); },
        escapeHtml: value => String(value), alert: message => alerts.push(message), setInterval() {}
    });
    vm.runInContext(source, context);
    return {context, alerts, calls, elements, ready: vm.runInContext('queueReady', context)};
}
test('next conflict displays server error instead of pretending to serve a patient', async () => {
    const {context, alerts, ready} = setup([response([]), response({message: 'Complete current appointment'}, false), response([])]);
    await ready; await context.callNextPatient();
    assert.deepEqual(alerts, ['Complete current appointment']);
});
test('completion conflict is not treated as successful completion', async () => {
    const {context, alerts, ready} = setup([response([]), response({message: 'Only in-progress appointments can complete'}, false), response([])]);
    await ready; await context.completeAppointment(1);
    assert.deepEqual(alerts, ['Only in-progress appointments can complete']);
});
test('only the active consultation has an enabled Complete button', async () => {
    const {elements, ready} = setup([response([
        {appointmentId: 1, tokenNumber: 1, status: 'BOOKED'},
        {appointmentId: 2, tokenNumber: 2, status: 'IN_PROGRESS'}
    ])]); await ready;
    assert.match(elements.queueTable.innerHTML, /completeAppointment\(1\)"\s+disabled/);
    assert.doesNotMatch(elements.queueTable.innerHTML, /completeAppointment\(2\)"\s+disabled/);
});
