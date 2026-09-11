-- Adding workshops

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
        '01a08ef5-f44c-7553-985f-73f45ba8762e',
        '2026-09-26',
        'RAGnosis',
        'IT',
        0,
        0,
        'WORKSHOP',
        NOW(),
        NOW()
    )
ON CONFLICT (event_id) DO NOTHING;

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
        '01a08f1f-677e-753b-9ed2-e1b86446c351',
        '2026-09-25',
        'ROS Arena',
        'EEE',
        0,
        0,
        'WORKSHOP',
        NOW(),
        NOW()
    )
ON CONFLICT (event_id) DO NOTHING;
