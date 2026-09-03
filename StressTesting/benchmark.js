import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';

// Configuration: Simulating a moderate ticket sale rush
export const options = {
    stages: [
        { duration: '10s', target: 10 }, // Ramp up to 20 concurrent volunteers/users
        { duration: '30s', target: 20 }, // Hold steady for 30 seconds
        { duration: '10s', target: 0 },  // Ramp down to 0
    ],
};

const EVENT_IDS = [
    "01a065e6-e1fe-7d40-8560-449efc587ab2",
    "01a065e6-e1ff-798b-b118-6460a9fc305f",
    "01a065e6-e1f8-7638-900b-c17d6e1503d6",
    "01a065e6-e1f9-7e6d-980b-ee8c7a9befed"
];

export default function () {
    // 1. Generate unique mock data to bypass database UNIQUE constraints
    const vuId = exec.vu.idInTest;
    const iter = exec.vu.iterationInInstance;
    const uniqueEmail = `sai+test_${vuId}_${iter}@buildapp.in`;
    const uniquePhone = `8220${Math.floor(100000 + Math.random() * 900000)}`;

    const registerPayload = JSON.stringify({
        email: uniqueEmail,
        phone: uniquePhone,
        name: `Saipranav LoadTest ${vuId}-${iter}`,
        gender: "M",
        college_name: "SSN College of Engineering",
        year_of_study: 4,
        ticket_type: "TECHPASS",
        amount_to_be_paid: 300.00,
        event_ids: EVENT_IDS
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    // 2. Step 1: Register User
    const registerRes = http.post('http://localhost:8080/api/v1/register', registerPayload, params);

    const registerSuccess = check(registerRes, {
        'register status is 200 or 201': (r) => r.status === 200 || r.status === 201,
        'has ticket_id': (r) => r.json('ticket_id') !== undefined,
    });

    // 3. Step 2: Approve Payment
    if (registerSuccess) {
        const ticketId = registerRes.json('ticket_id');

        const approvePayload = JSON.stringify({
            ticket_id: ticketId
        });



        const approveRes = http.post('http://localhost:8080/restricted/v1/approve-payment', approvePayload, params);

        check(approveRes, {
            'approval status is 200 or 201': (r) => r.status === 200 || r.status === 201,
        });
    } else {
        console.warn(`Registration failed with status ${registerRes.status}: ${registerRes.body}`);
    }


}