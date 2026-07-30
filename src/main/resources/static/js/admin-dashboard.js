loadDashboard();

function logout() {

    localStorage.clear();

    window.location.href = "/pages/login.html";

}

function loadDashboard() {

    fetch("/api/admin/dashboard")

        .then(res => res.json())

        .then(data => {

            document.getElementById("patients").innerHTML =
                data.totalPatients;

            document.getElementById("doctors").innerHTML =
                data.totalDoctors;

            document.getElementById("appointments").innerHTML =
                data.totalAppointments;

            createPieChart(data);

            createBarChart(data);

        });

}

function createPieChart(data) {

    new Chart(document.getElementById("pieChart"), {

        type: "pie",

        data: {

            labels: [

                "Completed",
                "Booked",
                "Emergency"

            ],

            datasets: [

                {

                    data: [

                        data.completedAppointments,
                        data.bookedAppointments,
                        data.emergencyAppointments

                    ]

                }

            ]

        }

    });

}

function createBarChart(data) {

    new Chart(document.getElementById("barChart"), {

        type: "bar",

        data: {

            labels: [

                "Patients",
                "Doctors",
                "Appointments"

            ],

            datasets: [

                {

                    label: "Hospital Data",

                    data: [

                        data.totalPatients,
                        data.totalDoctors,
                        data.totalAppointments

                    ]

                }

            ]

        }

    });

}