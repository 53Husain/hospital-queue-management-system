document.getElementById("doctorName").textContent = localStorage.getItem("name");
let queueWritePending = false;

async function queueError(response, fallback) {
    try { const data = await response.json(); return data.message || fallback; }
    catch (_) { return fallback; }
}

async function loadQueue() {
    try {
        const doctorId = localStorage.getItem("doctorId");
        const response = await apiFetch("/api/queue/" + doctorId);
        if (!response.ok) throw new Error(await queueError(response, "Unable to load queue"));
        const data = await response.json();
        document.getElementById("queueTable").innerHTML = data.map(patient => `
<tr>
<td>${patient.tokenNumber}</td>
<td>${escapeHtml(patient.patientName)}</td>
<td>${escapeHtml(patient.priority)}</td>
<td>${escapeHtml(patient.status)}</td>
<td><button class="btn btn-success btn-sm"
    onclick="completeAppointment(${patient.appointmentId})"
    ${patient.status === "IN_PROGRESS" ? "" : "disabled"}>Complete</button></td>
</tr>`).join("");
    } catch (error) {
        document.getElementById("queueTable").textContent = error.message || "Unable to load queue";
    }
}

async function callNextPatient() {
    if (queueWritePending) return;
    queueWritePending = true;
    try {
        const response = await apiFetch("/api/queue/next/" + localStorage.getItem("doctorId"), {method: "POST"});
        if (!response.ok) throw new Error(await queueError(response, "Unable to call next patient"));
        const data = await response.json();
        alert("Now Serving\n\nToken: " + data.tokenNumber + "\nPatient: " + data.patientName);
    } catch (error) {
        alert(error.message || "Unable to call next patient");
    } finally {
        queueWritePending = false;
        await loadQueue();
    }
}

async function completeAppointment(appointmentId) {
    if (queueWritePending) return;
    queueWritePending = true;
    try {
        const response = await apiFetch("/api/queue/complete/" + appointmentId, {method: "PUT"});
        if (!response.ok) throw new Error(await queueError(response, "Unable to complete appointment"));
        alert(await response.text());
    } catch (error) {
        alert(error.message || "Unable to complete appointment");
    } finally {
        queueWritePending = false;
        await loadQueue();
    }
}

const queueReady = loadQueue();
setInterval(loadQueue, 5000);
