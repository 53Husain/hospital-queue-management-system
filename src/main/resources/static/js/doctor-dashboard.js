document.getElementById("doctorName").textContent =
localStorage.getItem("name");

loadQueue();



function loadQueue() {

    const doctorId = localStorage.getItem("doctorId");

    apiFetch("/api/queue/" + doctorId)

        .then(res => res.json())

        .then(data => {

            let table = "";

            data.forEach(patient => {

                table += `

<tr>

<td>${patient.tokenNumber}</td>

<td>${escapeHtml(patient.patientName)}</td>

<td>${escapeHtml(patient.priority)}</td>

<td>${escapeHtml(patient.status)}</td>

<td>

<button
class="btn btn-success btn-sm"
onclick="completeAppointment(${patient.appointmentId})">

Complete

</button>

</td>

</tr>

`;

            });

            document.getElementById("queueTable").innerHTML = table;

        });

}

function callNextPatient(){

    const doctorId=localStorage.getItem("doctorId");

    apiFetch("/api/queue/next/"+doctorId,{

        method:"POST"

    })

    .then(res=>res.json())

    .then(data=>{

        alert(

            "Now Serving\n\n"+
            "Token : "+data.tokenNumber+
            "\nPatient : "+data.patientName

        );

        loadQueue();

    });

}

function completeAppointment(appointmentId) {

    apiFetch("/api/queue/complete/" + appointmentId, {

        method: "PUT"

    })

    .then(res => res.text())

    .then(message => {

        alert(message);

        loadQueue();

    })

    .catch(err => {

        console.error(err);

        alert("Unable to complete appointment");

    });

}

// Auto refresh every 5 seconds
setInterval(loadQueue, 5000);
