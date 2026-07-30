document.getElementById("doctorName").innerHTML =
localStorage.getItem("name");

loadQueue();

function logout(){

    localStorage.clear();

    window.location.href="/pages/login.html";

}

function loadQueue() {

    const doctorId = localStorage.getItem("doctorId");

    fetch("/api/queue/" + doctorId)

        .then(res => res.json())

        .then(data => {

            let table = "";

            data.forEach(patient => {

                table += `

<tr>

<td>${patient.tokenNumber}</td>

<td>${patient.patientName}</td>

<td>${patient.priority}</td>

<td>${patient.status}</td>

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

    fetch("/api/queue/next/"+doctorId,{

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

    fetch("/api/queue/complete/" + appointmentId, {

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