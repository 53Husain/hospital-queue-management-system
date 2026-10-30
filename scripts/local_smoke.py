"""CI-only smoke check: launches a packaged app against a disposable Compose DB.
Run from the repository root after compose up --wait and mvnw package.
Creates synthetic records; never point it at an existing database.
"""
import http.cookiejar
import json
import os
from pathlib import Path
import subprocess
import threading
import time
import urllib.error
import urllib.request
import uuid
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

MODE = 'hospital'
PORT = 8082
ARTIFACT = 'hospital-queue-management-system'
BASE = f'http://localhost:{PORT}'

class Client:
    def __init__(self):
        self.http = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))

    def call(self, method, path, body=None, expected=200, csrf=False):
        headers = {}
        if csrf:
            token = self.call('GET', '/api/auth/csrf')
            headers[token['headerName']] = token['token']
        data = None if body is None else json.dumps(body).encode()
        if data is not None:
            headers['Content-Type'] = 'application/json'
        request = urllib.request.Request(BASE + path, data=data, headers=headers, method=method)
        try:
            response = self.http.open(request, timeout=10)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            raw = response.read().decode()
            assert response.code == expected, (path, response.code, raw[:500])
            if not raw:
                return None
            try:
                return json.loads(raw)
            except json.JSONDecodeError:
                return raw

provider_requests = []
class Provider(BaseHTTPRequestHandler):
    def do_POST(self):
        payload = json.loads(self.rfile.read(int(self.headers['Content-Length'])))
        provider_requests.append(payload)
        body = json.dumps({'choices': [{'message': {'content': 'SELECT name, salary FROM employees WHERE salary > 50000'}}]}).encode()
        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, *args):
        pass


def hospital():
    admin, patient, doctor = Client(), Client(), Client()
    password = 'ci-demo-password-2026'
    admin.call('POST', '/api/auth/login', {'email': 'ci-admin@example.com', 'password': password}, csrf=True)
    suffix = uuid.uuid4().hex[:12]
    staff = {'fullName': 'CI Doctor', 'email': f'doctor-{suffix}@example.com', 'password': password,
             'phoneNumber': '0000000000', 'role': 'DOCTOR'}
    admin.call('POST', '/api/admin/users', staff, csrf=True)
    person = dict(staff, fullName='CI Patient', email=f'patient-{suffix}@example.com', role='PATIENT')
    patient.call('POST', '/api/auth/register', person, csrf=True)
    identity = patient.call('POST', '/api/auth/login', {'email': person['email'], 'password': password}, csrf=True)
    doc = doctor.call('POST', '/api/auth/login', {'email': staff['email'], 'password': password}, csrf=True)
    assert any(d['id'] == doc['doctorId'] for d in patient.call('GET', '/api/doctors'))
    booking = patient.call('POST', '/api/appointments', {'patientId': identity['patientId'], 'doctorId': doc['doctorId'], 'priority': 'NORMAL'}, csrf=True)
    assert booking['status'] == 'BOOKED' and booking['tokenNumber'] > 0
    assert len(patient.call('GET', f"/api/appointments/patient/{identity['patientId']}")) == 1
    next_path = f"/api/queue/next/{doc['doctorId']}"
    assert doctor.call('POST', next_path, csrf=True)['status'] == 'IN_PROGRESS'
    doctor.call('POST', next_path, csrf=True, expected=409)
    complete = f"/api/queue/complete/{booking['appointmentId']}"
    doctor.call('PUT', complete, csrf=True)
    doctor.call('PUT', complete, csrf=True, expected=409)
    patient.call('POST', '/api/auth/logout', csrf=True, expected=204)
    patient.call('GET', '/api/doctors', expected=401)


def main():
    env = os.environ.copy()
    env['SERVER_PORT'] = str(PORT)
    mock = None
    if MODE == 'hospital':
        env.update(APP_BOOTSTRAPADMIN_ENABLED='true', APP_BOOTSTRAPADMIN_EMAIL='ci-admin@example.com',
                   APP_BOOTSTRAPADMIN_PASSWORD='ci-demo-password-2026')
    if MODE == 'querypilot':
        mock = ThreadingHTTPServer(('127.0.0.1', 0), Provider)
        threading.Thread(target=mock.serve_forever, daemon=True).start()
        env.update(DEEPSEEK_API_KEY='ci-mock-only', DEEPSEEK_API_URL=f'http://127.0.0.1:{mock.server_port}/chat/completions')
    Path('.smoke').mkdir(exist_ok=True)
    with Path('.smoke/application.log').open('w') as log:
        app = subprocess.Popen(['java', '-jar', f'target/{ARTIFACT}-0.0.1-SNAPSHOT.jar', '--spring.profiles.active=local'], env=env, stdout=log, stderr=subprocess.STDOUT)
        try:
            path = '/api/materials' if MODE == 'procureflow' else '/'
            deadline = time.monotonic() + 90
            while True:
                if app.poll() is not None:
                    raise RuntimeError('Application exited; see .smoke/application.log')
                try:
                    Client().call('GET', path)
                    break
                except (OSError, AssertionError):
                    if time.monotonic() >= deadline:
                        raise RuntimeError('Application did not become ready; see .smoke/application.log')
                    time.sleep(1)
            globals()[MODE]()
            print(f'{MODE}: Compose database, local profile and HTTP smoke checks passed')
        finally:
            app.terminate()
            try:
                app.wait(timeout=15)
            except subprocess.TimeoutExpired:
                app.kill()
                app.wait()
            if mock:
                mock.shutdown()
                mock.server_close()

if __name__ == '__main__':
    main()
