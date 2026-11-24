SET SESSION cte_max_recursion_depth = 1000000;

INSERT INTO veyro_performance.post (
    journalist_name,
    content,
    external_post_id,
    content_created_at,
    is_transfer_related
)
WITH RECURSIVE numbers AS (
    SELECT 100001 AS n

    UNION ALL

    SELECT n + 1
    FROM numbers
    WHERE n < 1000000
)
SELECT
    CASE
        WHEN MOD(n * 37, 100) < 50 THEN 'FABRIZIO_ROMANO'
        WHEN MOD(n * 37, 100) < 80 THEN 'DAVID_ORNSTEIN'
        WHEN MOD(n * 37, 100) < 85 THEN 'MATTEO_MORETTO'
        WHEN MOD(n * 37, 100) < 90 THEN 'FLORIAN_PLETTENBERG'
        WHEN MOD(n * 37, 100) < 95 THEN 'SANTI_AOUNA'
        ELSE 'DI_MARZIO'
        END,

    LEFT(
    CONCAT(
    'Performance test post ', n, '. ',
    REPEAT(
    'Football news and transfer updates. ',
    3 + MOD(n, 12)
    )
    ),
    500
    ),

    CONCAT('PERF_POST_', n),

    DATE_SUB(
    '2026-11-24 00:00:00',
    INTERVAL n MINUTE
    ),

    CASE
    WHEN MOD(n * 37, 101) < 71 THEN 1
    ELSE 0
END

FROM numbers;