document.getElementById("username").innerHTML =
localStorage.getItem("name");

loadDoctors();
loadAppointments();
function logout(){

    localStorage.clear();

    window.location.href="/pages/login.html";

}

function loadDoctors(){

    fetch("/api/doctors")

        .then(res=>res.json())

        .then(data=>{

            let table="";

            data.forEach(doctor=>{

                table+=`

<tr>

<td>${doctor.name}</td>

<td>${doctor.specialization}</td>

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

    fetch("/api/appointments", {
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

    fetch("/api/appointments/patient/" + patientId)

        .then(res => res.json())

        .then(data => {

            let table = "";

            data.forEach(app => {

                table += `

<tr>

<td>${app.tokenNumber}</td>

<td>${app.doctorName}</td>

<td>${app.priority}</td>

<td>${app.estimatedWaitingTime} mins</td>

<td>${app.status}</td>

</tr>

`;

            });

            document.getElementById("appointmentTable").innerHTML = table;

        });

}

setInterval(loadDoctors, 5000);
