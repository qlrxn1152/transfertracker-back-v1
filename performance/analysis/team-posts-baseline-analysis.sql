-- Read-only baseline analysis for GET /api/posts/teams/{teamId}?page={page}
-- Execute against veyro_performance after performance/seed/seed-team-posts.sql.

-- Confirm the deliberately different team-post distributions.
SELECT
    team.team_id,
    team.team_name,
    COUNT(post_team.post_team_id) AS post_count
FROM team
LEFT JOIN post_team ON post_team.team_id = team.team_id
WHERE team.api_football_id IN (990000001, 990000002, 990000003)
GROUP BY team.team_id, team.team_name
ORDER BY post_count DESC;

SHOW INDEX FROM post_team;
SHOW INDEX FROM post;

-- Substitute the selected high/low team IDs.  The data query mirrors the
-- repository's fetch join and the pagination order; the count query mirrors
-- Spring Data Page metadata generation.
EXPLAIN ANALYZE
SELECT post_team.post_team_id
FROM post_team
JOIN team ON team.team_id = post_team.team_id
JOIN post ON post.post_id = post_team.post_id
WHERE team.team_id = :high_team_id
ORDER BY post.content_created_at DESC, post.post_id DESC
LIMIT 30 OFFSET 0;

EXPLAIN ANALYZE
SELECT COUNT(*)
FROM post_team
WHERE post_team.team_id = :high_team_id;

EXPLAIN ANALYZE
SELECT post_team.post_team_id
FROM post_team
JOIN team ON team.team_id = post_team.team_id
JOIN post ON post.post_id = post_team.post_id
WHERE team.team_id = :low_team_id
ORDER BY post.content_created_at DESC, post.post_id DESC
LIMIT 30 OFFSET 0;

EXPLAIN ANALYZE
SELECT COUNT(*)
FROM post_team
WHERE post_team.team_id = :low_team_id;

-- Large-offset page for the high-volume team. Replace :high_offset with
-- floor((post_count - 1) / 30), so the requested page stays valid.
EXPLAIN ANALYZE
SELECT post_team.post_team_id
FROM post_team
JOIN team ON team.team_id = post_team.team_id
JOIN post ON post.post_id = post_team.post_id
WHERE team.team_id = :high_team_id
ORDER BY post.content_created_at DESC, post.post_id DESC
LIMIT 30 OFFSET :high_offset;
