document.getElementById("username").textContent =
localStorage.getItem("name");

loadDoctors();
loadAppointments();


function loadDoctors(){

    apiFetch("/api/doctors")

        .then(res=>res.json())

        .then(data=>{

            let table="";

            data.forEach(doctor=>{

                table+=`

<tr>

<td>${escapeHtml(doctor.name)}</td>

<td>${escapeHtml(doctor.specialization)}</td>

<td>${doctor.experience} Years</td>

<td>${doctor.rating}</td>

<td>

<button
class="btn btn-primary"
onclick="bookAppointment(${doctor.id})">

Book

</button>

</td>

</tr>

`;

            });

            document.getElementById("doctorTable").innerHTML=table;

        });

}
function bookAppointment(doctorId) {

    const patientId = localStorage.getItem("patientId");

    if (!patientId) {
        alert("Patient not found. Please login again.");
        return;
    }

    const priority = prompt(
        "Enter Priority (NORMAL or EMERGENCY)",
        "NORMAL"
    );

    if (priority === null) {
        return;
    }

    apiFetch("/api/appointments", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            patientId: Number(patientId),
            doctorId: Number(doctorId),
            priority: priority.toUpperCase()
        })
    })
    .then(async response => {

        const data = await response.json();

        if (!response.ok) {
            alert(data.message || "Booking Failed");
            return;
        }

        alert(
            "Appointment Booked Successfully\n\n" +
            "Token Number: " + data.tokenNumber +
            "\nEstimated Waiting Time: " + data.estimatedWaitingTime + " mins" +
            "\nStatus: " + data.status
        );

    })
    .catch(error => {
        console.error(error);
        alert("Server Error");
    });
}

function loadAppointments() {

    const patientId = localStorage.getItem("patientId");

    apiFetch("/api/appointments/patient/" + patientId)

        .then(res => res.json())

        .then(data => {

            let table = "";

            data.forEach(app => {

                table += `

<tr>

<td>${app.tokenNumber}</td>

<td>${escapeHtml(app.doctorName)}</td>

<td>${escapeHtml(app.priority)}</td>

<td>${app.estimatedWaitingTime} mins</td>

<td>${escapeHtml(app.status)}</td>

</tr>

`;

            });

            document.getElementById("appointmentTable").innerHTML = table;

        });

}

setInterval(loadDoctors, 5000);

