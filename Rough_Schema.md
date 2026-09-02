**Users**

| *user_id* | email | phone   | name   | gender     | college name | yearOfStudy |
| --------- | ----- | ------- | ------ | ---------- | ------------ | ----------- |
| uuid, PK  | email | str(10) | string | varchar(1) | str          | 1/2/3/4/... |

**Events**

| *event_id* | date | name | reg_count | attend_count | event_type                               | amount  |
| ---------- | ---- | ---- | --------- | ------------ | ---------------------------------------- | ------- |
| uuid, PK   | date | str  | int       | int          | `TECH/NONTECH/WORKSHOP/HACKATHON/RACING` | ??????? |

TicketType

| ticket_type | amount |
| ----------- | ------ |
| HACKATHON   | 1200   |
| TECHPASS    | 300    |
| NONTECHPASS | NULL   |
| RACING      | 500    |
| WORKSHOP    | 300    |

**TicketPayments**
Status: Pending Payment, Not Verified, Accepted/Rejected

| *ticket_id* | user_id   | ticket_type    | amount_paid | s3_url         | status                                       | email_sent       |
| ----------- | --------- | -------------- | ----------- | -------------- | -------------------------------------------- | ---------------- |
| uuid        | FK->Users | FK->TicketType | float       | url (nullable) | PendingPayment/NotVerified/Accepted/Rejected | NULL/queued/sent |

**TicketEvent**

| *ticket_id*      | event_id  | attendance | attendance_timestamp |
| ---------------- | --------- | ---------- | -------------------- |
| uuid, FK->pay_id | FK->Event | T/F        | NULL/timestamp       |
| AAAA             | I         |            |                      |
| AAAA             | II        |            |                      |

All detials sent (name,email,phno) -> ticket_id created, sent to frontend+email -> receipt uploaded to S3 -> S3_url, ticket_id returned again to backend
Email *ticket_id* to user jic, so they can use to upload receipt (could embed direct URL to receipt upload page)
Reject the upload if receipt already exists for *ticket_id* + status!="PendingPayment" (409)

**HackathonRegs**

| team_id | team_name | *ticket_id* | domain            | track            | ps_description |
| ------- | --------- | ----------- | ----------------- | ---------------- | -------------- |
| uuid    | str       | uuid        | Software/Hardware | NULL/(6 domains) | str            |
team lead only is registered as user

| team_id | lead | email | phno | yearOf |
| ------- | ---- | ----- | ---- | ------ |
| uuid    | T/F  |       |      |        |
| 1       | T    |       |      |        |
| 1       | F    |       |      |        |
| ...     | F    |       |      |        |

Polling service (redis to postgres): SELECT ... verified_status=Accepted FOR UPDATE SKIP LOCKED;
Lock is applied, now publish to redis (paginated)
Lock released when all payments are queued
XREADGROUP -> Pending evaluation list -> XACK (if ack not present, put back to queue (worker can consume)) - at least once delivery

**Attendance**

| attendance_entry_id | user_id         | event_id         | attendance_time |
| ------------------- | --------------- | ---------------- | --------------- |
| uuid/int/whatever   | uuid, FK->Users | uuid, FK->Events | timestamp       |

**Verification**

| id   | email | password | dept | name       |
| ---- | ----- | -------- | ---- | ---------- |
| uuid | str   | str      | uuid | Volunteer1 |
PaymentVerificationLog

| pay_id | verif_user_id | verif_time |
| ------ | ------------- | ---------- |
|        |               |            |

Hackathon
- 4 member team
- team lead pays at one go (1200)
- 50 team cap
	- 20 hardware
	- 30 software
- data:
	- team name
	- Hardware/Software
	- Software PS domain (6) - AgriTech, Fintech, Edutech, Healthcare, Sustainability(SDG), OpenInnovation
	- PS statement description
	- User() x 4: name, phone, email, college, yearOfStudy, gender
- CONSTRAINT: student cant be from differnet institutions (get this only once)

**Roles**

| 