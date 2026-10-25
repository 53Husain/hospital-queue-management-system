async function loadQueueStatus() {

    const appointmentId = localStorage.getItem("appointmentId");

    if (!appointmentId) {

        alert("No appointment found.");

        return;
    }

    const positionResponse =
        await apiFetch(
            "/api/queue/position/" + appointmentId
        );

    const position =
        await positionResponse.json();

    const appointmentResponse =
        await apiFetch(
            "/api/appointments/" + appointmentId
        );

    const appointment =
        await appointmentResponse.json();

    document.getElementById("token").innerHTML =
        "Token : " + appointment.tokenNumber;

    document.getElementById("position").innerHTML =
        "Queue Position : " + position;

    document.getElementById("waiting").innerHTML =
        "Estimated Waiting : " + appointment.estimatedWaitingTime + " Minutes";

    document.getElementById("status").innerHTML =
        "Status : " + appointment.status;

}

loadQueueStatus();
