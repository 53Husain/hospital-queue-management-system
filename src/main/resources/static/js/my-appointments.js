document.getElementById("username").innerHTML =
    localStorage.getItem("name");

const patientId = localStorage.getItem("patientId");

if (!patientId) {
    alert("Please login as a patient.");
    window.location.href = "/pages/login.html";
}

loadAppointments();

setInterval(loadAppointments, 5000);

function logout() {

    localStorage.clear();

    window.location.href = "/pages/login.html";

}

function loadAppointments() {

    const patientId = localStorage.getItem("patientId");

    console.log("Patient ID:", patientId);

    fetch("/api/appointments/patient/" + patientId)

        .then(async res => {

            console.log("Status:", res.status);

            const data = await res.json();

            console.log(data);

            if (!res.ok) {
                throw new Error(data.message || "Unable to load appointments");
            }

            return data;

        })

        .then(data => {

            let table = "";

            data.forEach(appointment => {

                table += `
                <tr>
                    <td>${appointment.appointmentId}</td>
                    <td>${appointment.doctorName}</td>
                    <td>${appointment.priority}</td>
                    <td>${appointment.tokenNumber}</td>
                    <td>${appointment.estimatedWaitingTime} mins</td>
                    <td>${appointment.status}</td>
                </tr>`;
            });

            document.getElementById("appointmentTable").innerHTML = table;

        })

        .catch(error => {

            console.error(error);

            alert(error.message);

        });

}