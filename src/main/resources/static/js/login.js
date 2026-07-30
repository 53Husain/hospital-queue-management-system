console.log("login.js loaded");

document.getElementById("loginForm")
.addEventListener("submit", async function(e){

    e.preventDefault();

    const loginData = {

        email: document.getElementById("email").value,
        password: document.getElementById("password").value

    };

    try {

        const response = await fetch(
            "http://localhost:8080/api/auth/login",
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(loginData)
            }
        );

        const data = await response.json();

        console.log("Response:", data);

        if(response.ok){

            localStorage.setItem("userId", data.userId);
            localStorage.setItem("patientId", data.patientId);
            localStorage.setItem("doctorId", data.doctorId);
            localStorage.setItem("role", data.role);
            localStorage.setItem("name", data.fullName);

            if(data.role === "PATIENT"){

                window.location.href="/pages/patient-dashboard.html";

            }
            else if(data.role === "DOCTOR"){

                window.location.href="/pages/doctor-dashboard.html";

            }
            else if(data.role === "ADMIN"){

                window.location.href="/pages/admin-dashboard.html";

            }
            else{

                alert("Unknown Role : " + data.role);

            }

        }
        else{

            alert(data.message);

        }

    }
    catch(error){

        console.error(error);
        alert("Server Error");

    }

});