let doctors=[];

async function loadDoctors(){

    const response=
    await fetch(
        "http://localhost:8080/api/doctors"
    );

    doctors=
    await response.json();

    displayDoctors(doctors);

}

function displayDoctors(list){

    let html="";

    list.forEach(d=>{

        html+=`

<div class="card">

<h2>
👨‍⚕️ ${d.user.fullName}
</h2>

<p>

<b>Specialization :</b>

${d.specialization}

</p>

<p>

<b>Experience :</b>

${d.experience} Years

</p>

<p>

⭐ ${d.rating}

</p>

<button onclick="book(${d.id})">

Book Appointment

</button>

</div>

`;

    });

    document.getElementById("doctorList")
    .innerHTML=html;

}

function filterDoctors(){

    const text=
    document.getElementById("search")
    .value
    .toLowerCase();

    const filtered=
    doctors.filter(d=>

        d.user.fullName
        .toLowerCase()
        .includes(text)

        ||

        d.specialization
        .toLowerCase()
        .includes(text)

    );

    displayDoctors(filtered);

}

function book(id){

    localStorage.setItem(
        "doctorId",
        id
    );

    window.location.href=
    "/pages/book-appointment.html";

}

loadDoctors();