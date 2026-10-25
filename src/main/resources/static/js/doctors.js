let doctors=[];

async function loadDoctors(){

    const response=
    await apiFetch(
        "/api/doctors"
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
👨‍⚕️ ${escapeHtml(d.name)}
</h2>

<p>

<b>Specialization :</b>

${escapeHtml(d.specialization)}

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

        d.name
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
