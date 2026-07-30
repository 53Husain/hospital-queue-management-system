const patientId = localStorage.getItem("patientId");

loadProfile();

function loadProfile() {

    fetch("/api/patient/profile/" + patientId)

        .then(res => res.json())

        .then(data => {

            document.getElementById("fullName").value =
                data.fullName;

            document.getElementById("email").value =
                data.email;

            document.getElementById("phoneNumber").value =
                data.phoneNumber;

            document.getElementById("age").value =
                data.age;

            document.getElementById("gender").value =
                data.gender;

            document.getElementById("bloodGroup").value =
                data.bloodGroup;

        });

}

function updateProfile() {

    const profile = {

        phoneNumber:
            document.getElementById("phoneNumber").value,

        age:
            parseInt(document.getElementById("age").value),

        gender:
            document.getElementById("gender").value,

        bloodGroup:
            document.getElementById("bloodGroup").value

    };

    fetch("/api/patient/profile/" + patientId, {

        method: "PUT",

        headers: {

            "Content-Type": "application/json"

        },

        body: JSON.stringify(profile)

    })

    .then(res => res.text())

    .then(message => {

        alert(message);

        loadProfile();

    })

    .catch(error => {

        console.error(error);

        alert("Failed to update profile.");

    });

}