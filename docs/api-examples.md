# Hospital API walkthrough

Import [the patient collection](api.postman_collection.json) and keep Postman's
cookie jar enabled. Use one consistent hostname (localhost); cookies for
127.0.0.1 and localhost are different. Start with [local setup](local-setup.md).

A new database has no doctors. Follow the README's first-admin bootstrap and
`POST /api/admin/users` instructions to create one. Use a separate cookie jar or
log out before switching to the patient flow.

1. Set patientEmail to a new demo address and patientPassword to a local test
   password of at least 12 characters (at most 72 UTF-8 bytes).
2. Run Get CSRF, Register patient, and Login patient in order. For an existing
   patient, skip registration. Login captures patientId.
3. Run Refresh CSRF after login: login rotates both the session and CSRF token.
4. List doctors; copy an actual returned `id` to the doctorId collection variable.
   If the list is empty, provision a doctor before continuing.
5. Book selected doctor once, then My appointments. Booking returns the actual
   appointmentId, tokenNumber, estimatedWaitingTime and status.
6. Logout contacts the server and invalidates the session.

The collection stores CSRF values locally and sends them with writes. Never
share an exported collection containing your real password or active tokens.
The CI smoke check uses synthetic patient/doctor accounts to exercise session,
CSRF, booking, call-next, completion and a repeated-transition 409 against MySQL.
It is not a rendered-browser test. Repeated booking requests create new bookings;
client examples do not make the server idempotent.
