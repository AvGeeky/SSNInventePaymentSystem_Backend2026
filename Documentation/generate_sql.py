import json
from uuid6 import uuid7
from datetime import datetime


INPUT_FILE = "events.json"
OUTPUT_FILE = "events_insert.sql"
EVENT_YEAR = 2026


def sql_string(value):
    """
    Safely convert a Python string to a PostgreSQL string literal.
    """
    if value is None:
        return "NULL"

    value = str(value).replace("'", "''")
    return f"'{value}'"


def parse_date(day):
    """
    Convert event day/date to a PostgreSQL-compatible date.

    Day 1     -> 2026-09-25
    Day 2     -> 2026-09-26
    Day 1 & 2 -> 2026-09-25
    Sep 25    -> 2026-09-25
    TBA       -> None
    """
    if not day:
        return None

    day = day.strip().lower()

    # Event day mappings
    if "day 1" in day and "day 2" in day:
        return "2026-09-25"

    if "day 1" in day:
        return "2026-09-25"

    if "day 2" in day:
        return "2026-09-26"

    # Explicit date such as "Sep 25"
    try:
        parsed = datetime.strptime(day.title(), "%b %d")
        return parsed.replace(year=EVENT_YEAR).strftime("%Y-%m-%d")
    except ValueError:
        return None

def get_event_type(event):
    """
    Convert the JSON category/domain into the database event_type.

    Allowed DB values:
        TECH
        NONTECH
        WORKSHOP
        HACKATHON
        RACING
    """

    category = (event.get("category") or "").lower()
    name = (event.get("name") or "").lower()
    domain = (event.get("domain") or "").lower()

    if "hackathon" in name or "hackathon" in domain:
        return "HACKATHON"

    if "racing" in name or "race" in domain:
        return "RACING"

    if "workshop" in category or "workshop" in domain:
        return "WORKSHOP"

    if category == "technical":
        return "TECH"

    if category == "non-technical":
        return "NONTECH"

    return "UNKNOWN"

def main():

    with open(INPUT_FILE, "r", encoding="utf-8") as f:
        events = json.load(f)

    sql_statements = []

    for event in events:

        event_id = uuid7()

        event_date = parse_date(event.get("day"))

        name = event.get("name")
        department = event.get("department")
        if department == "Mechanical":
            department = "Mechanical Engineering"
        if department == "Open to all":
            department = "Open to All Departments"

        event_type = get_event_type(event)

        date_value = (
            sql_string(event_date)
            if event_date
            else "NULL"
        )

        sql = f"""
INSERT INTO events
(
    event_id,
    date,
    name,
    dept_name,
    reg_count,
    attend_count,
    event_type,
    created_at,
    updated_at
)
VALUES
(
    '{event_id}',
    {date_value},
    {sql_string(name)},
    {sql_string(department)},
    0,
    0,
    {sql_string(event_type)},
    NOW(),
    NOW()
);
""".strip()

        sql_statements.append(sql)

    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:

        f.write("BEGIN;\n\n")

        for statement in sql_statements:
            f.write(statement)
            f.write("\n\n")

        f.write("COMMIT;\n")

    print(f"Generated {len(sql_statements)} INSERT statements.")
    print(f"Output: {OUTPUT_FILE}")


if __name__ == "__main__":
    main()