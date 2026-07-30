document
    .getElementById("registerForm")
    .addEventListener("submit", async function (e) {

        e.preventDefault();

        const user = {
            fullName: document.getElementById("fullName").value,
            email: document.getElementById("email").value,
            phoneNumber: document.getElementById("phone").value,
            password: document.getElementById("password").value,
            role: document.getElementById("role").value
        };

        try {

            const response = await fetch(
                "http://localhost:8080/api/auth/register",
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify(user)
                }
            );

            const message = await response.text();

            if (response.ok) {
                alert(message);
                window.location.href = "/pages/login.html";
            } else {
                alert(message);
            }

        } catch (error) {
            alert("Server Error");
            console.log(error);
        }
    });