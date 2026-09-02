### 1. Public Registration APIs

**`POST /api/v1/register`**

* **Purpose:** Registers a user for standard events (Techpass, workshops, paper presentations, nitro racing). Creates the `Users`, `TicketPayments`, and `TicketEvent` records.### 1. Public Registration APIs

* **Request Body:**
```json
{
  "email": "string",
  "phone": "string",
  "name": "string",
  "gender": "string(1)",
  "college_name": "string",
  "year_of_study": "integer",
  "ticket_type": "string",
  "amount_to_be_paid": "decimal",
  "event_ids": ["uuid", "uuid", "uuid", "uuid"]  
}

```


* **Response (201 Created):**
```json
{
  "ticket_id": "uuid",
  "message": "Save this ID. Provide it when uploading the payment receipt PDF."
}

```

---

**`POST /api/v1/register-hackathon`**

* **Purpose:** Handles team lead payment and team creation.
* **Request Body:**
```json
{
  "leader": {
    "email": "string",
    "phone": "string",
    "name": "string",
    "gender": "string(1)",
    "college_name": "string",
    "year_of_study": "integer"
  },
  "team_name": "string",
  "domain": "string",
  "track": "string",
  "ps_description": "string",
  "members": [
    {
      "name": "string",
      "email": "string",
      "phone": "string",
      "year_of_study": "integer"
    }
  ]
}

```
* **Response (201 Created):**
```json
{
  "ticket_id": "uuid",
  "message": "Save this ID. Provide it when uploading the payment receipt PDF."
}

```
---
**`PATCH /api/v1/receipt/{ticket_id}`**

* **Purpose:** Updates the `TicketPayments` record with the uploaded S3 URL.
* **Request Body:**
```json
{
  "s3_url": "string"
}

```
---


**`GET /api/v1/events`**

* **Purpose:** Fetches available events for frontend dropdowns/selection.
* **Response (200 OK):**
```json
[
  {
    "event_id": "uuid",
    "name": "string",
    "event_type": "string",
    "date": "timestamp"
  }
]

```
---


**`GET /api/v1/hackathon-stats`**

* **Purpose:** Returns the current number of registrations to enforce the 50-team cap (30 Software, 20 Hardware).
* **Response (200 OK):**
```json
{
  "total_registrations": 15,
  "software_count": 2,
  "hardware_count": 13
}

```



---

### 3. Internal APIs (Admin/Volunteer only)

**`POST protected/direct-register`**

* **Purpose:** Allows admins to manually insert a user and payment bypassing the standard S3 receipt flow (e.g., on-spot cash registrations). The payload matches `POST /api/v1/register` exactly, but the backend immediately sets `TicketPayments.status` to `Accepted`.
* **Request Body:**
```json
{
  "email": "string",
  "phone": "string",
  "name": "string",
  "gender": "string(1)",
  "college_name": "string",
  "year_of_study": "integer",
  "ticket_type": "string",
  "amount_to_be_paid": "decimal",
  "event_ids": ["uuid", "uuid", "uuid", "uuid"] 
}

```


* **Response (201 Created):**
```json
{
  "ticket_id": "uuid",
  "message": "Save this ID. Provide it when uploading the payment receipt PDF."
}

```



**`POST /api/v1/register-hackathon`**

* **Purpose:** Handles team lead payment and team creation.
* **Request Body:**
```json
{
  "leader": {
    "email": "string",
    "phone": "string",
    "name": "string",
    "gender": "string(1)",
    "college_name": "string",
    "year_of_study": "integer"
  },
  "team_name": "string",
  "domain": "string",
  "track": "string",
  "ps_description": "string",
  "members": [
    {
      "name": "string",
      "email": "string",
      "phone": "string",
      "year_of_study": "integer"
    }
  ]
}

```



---

### 2. Protected APIs (Authenticated/Frontend Logic)

**`PATCH /api/v1/receipt/{ticket_id}`**

* **Purpose:** Updates the `TicketPayments` record with the uploaded S3 URL.
* **Request Body:**
```json
{
  "s3_url": "string"
}

```



**`GET /api/v1/events`**

* **Purpose:** Fetches available events for frontend dropdowns/selection.
* **Response (200 OK):**
```json
[
  {
    "event_id": "uuid",
    "name": "string",
    "event_type": "string",
    "date": "timestamp"
  }
]

```



**`GET /api/v1/hackathon-stats`**

* **Purpose:** Returns the current number of registrations to enforce the 50-team cap (30 Software, 20 Hardware).
* **Response (200 OK):**
```json
{
  "total_registrations": 15,
  "software_count": 2,
  "hardware_count": 13
}

```



---

### 3. Internal APIs (Admin/Volunteer only)

**`POST /api/v1/direct-register`**

* **Purpose:** Allows admins to manually insert a user and payment bypassing the standard S3 receipt flow (e.g., on-spot cash registrations). The payload matches `POST /api/v1/register` exactly, but the backend immediately sets `TicketPayments.status` to `Accepted`.