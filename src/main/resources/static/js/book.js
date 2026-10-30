const bookingForm = document.getElementById("bookingForm");
const doctorSelect = document.getElementById("doctor");
const prioritySelect = document.getElementById("priority");
const bookButton = document.getElementById("bookButton");
const bookingMessage = document.getElementById("bookingMessage");
let bookingPending = false;
let bookingCompleted = false;
let availableDoctorIds = new Set();

function positiveId(value) {
    const id = Number(value);
    return Number.isSafeInteger(id) && id > 0 ? id : null;
}

async function responseData(response) {
    try { return await response.json(); }
    catch (_) { return {}; }
}

async function loadBookingDoctors() {
    if (!positiveId(localStorage.getItem("patientId"))) {
        window.location.href = "/pages/login.html";
        return;
    }
    try {
        const response = await apiFetch("/api/doctors");
        const data = await responseData(response);
        if (!response.ok || !Array.isArray(data)) {
            throw new Error(data.message || "Unable to load doctors. Please reload this page.");
        }
        doctorSelect.replaceChildren();
        const placeholder = document.createElement("option");
        placeholder.value = "";
        placeholder.textContent = "Select a doctor";
        doctorSelect.appendChild(placeholder);
        for (const doctor of data) {
            const id = positiveId(doctor.id);
            if (!id) continue;
            const option = document.createElement("option");
            option.value = String(id);
            option.textContent = doctor.name + (doctor.specialization ? " — " + doctor.specialization : "");
            doctorSelect.appendChild(option);
            availableDoctorIds.add(id);
        }
        const selected = positiveId(localStorage.getItem("doctorId"));
        if (availableDoctorIds.has(selected)) doctorSelect.value = String(selected);
        doctorSelect.disabled = availableDoctorIds.size === 0;
        bookButton.disabled = availableDoctorIds.size === 0;
        if (!availableDoctorIds.size) bookingMessage.textContent = "No doctors are available for booking yet.";
    } catch (error) {
        bookingMessage.textContent = error.message || "Unable to load doctors. Please reload this page.";
    }
}

async function bookAppointment(event) {
    if (event) event.preventDefault();
    if (bookingPending || bookingCompleted) return;
    const patientId = positiveId(localStorage.getItem("patientId"));
    const doctorId = positiveId(doctorSelect.value);
    const priority = prioritySelect.value;
    if (!patientId) {
        window.location.href = "/pages/login.html";
        return;
    }
    if (!availableDoctorIds.has(doctorId) || !["NORMAL", "EMERGENCY"].includes(priority)) {
        bookingMessage.textContent = "Please select a doctor and a valid priority.";
        return;
    }
    bookingPending = true;
    bookButton.disabled = true;
    bookingMessage.textContent = "Booking your appointment…";
    try {
        const response = await apiFetch("/api/appointments", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({patientId, doctorId, priority})
        });
        const data = await responseData(response);
        if (!response.ok) throw new Error(data.message || "Booking failed. Please try again.");
        if (!positiveId(data.appointmentId) || !positiveId(data.tokenNumber) || data.status !== "BOOKED") {
            throw new Error("Unable to confirm the booking. Check My appointments before trying again.");
        }
        bookingCompleted = true;
        doctorSelect.disabled = true;
        prioritySelect.disabled = true;
        bookButton.textContent = "Appointment booked";
        bookingMessage.textContent = "Appointment booked. Token: " + data.tokenNumber +
            ". Estimated wait: " + data.estimatedWaitingTime + " minutes. Status: " + data.status + ".";
    } catch (error) {
        bookingMessage.textContent = error.message || "Unable to confirm the booking. Check My appointments before retrying.";
    } finally {
        bookingPending = false;
        bookButton.disabled = bookingCompleted;
    }
}

bookingForm.addEventListener("submit", bookAppointment);
const bookingReady = loadBookingDoctors();
