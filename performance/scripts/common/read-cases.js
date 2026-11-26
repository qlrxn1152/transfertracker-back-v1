import { encodedQuery, page, readPositiveInteger, readText } from './env.js';
import { createReadCase, isObject, listChecks } from './read-case.js';

const postPage = () => page();
const teamFixture = () => ({ teamId: readPositiveInteger('TEAM_ID'), page: postPage() });
const playerFixture = () => ({ playerId: readPositiveInteger('PLAYER_ID'), page: postPage() });
const transferFixture = () => ({ transferId: readPositiveInteger('TRANSFER_ID') });
const postFixture = () => ({ postId: readPositiveInteger('POST_ID') });
const journalistFixture = () => ({ journalist: readText('JOURNALIST', 'FABRIZIO_ROMANO'), page: postPage() });
const leagueFixture = () => ({ leagueCode: readText('LEAGUE_CODE', 'EPL') });
const keywordFixture = () => ({ page: postPage(), keyWord: readText('KEYWORD', 'VEYRO Performance') });
const authFixture = () => ({ page: postPage() });

const definitions = {
    'posts-list': {
        id: 'posts-list',
        fixture: () => ({ page: postPage() }),
        path: ({ page }) => `/api/posts?${encodedQuery({ page })}`,
        checks: (payload) => listChecks(payload, 'posts', { pagination: true }),
    },
    'post-journalist': {
        id: 'post-journalist',
        fixture: journalistFixture,
        path: ({ journalist, page }) => `/api/posts/journalists/${encodeURIComponent(journalist)}?${encodedQuery({ page })}`,
        checks: (payload, { journalist }) => listChecks(payload, 'posts', {
            pagination: true,
            each: (post) => post.journalist === journalist,
        }),
    },
    'post-player': {
        id: 'post-player',
        fixture: playerFixture,
        path: ({ playerId, page }) => `/api/posts/players/${playerId}?${encodedQuery({ page })}`,
        checks: (payload, { playerId }) => listChecks(payload, 'posts', {
            pagination: true,
            each: (post) => Number(post.playerId) === playerId,
        }),
    },
    'post-detail': {
        id: 'post-detail',
        fixture: postFixture,
        path: ({ postId }) => `/api/posts/${postId}`,
        checks: (payload, { postId }) => ({
            'response postId matches request': () => Number(payload?.postId) === postId,
            'response has post content': () => typeof payload?.content === 'string',
        }),
    },
    'post-sources': {
        id: 'post-sources',
        path: () => '/api/posts/sources',
        checks: (payload) => listChecks(payload, 'sources'),
    },
    'players-list': {
        id: 'players-list',
        fixture: () => ({ page: postPage() }),
        path: ({ page }) => `/api/players?${encodedQuery({ page })}`,
        checks: (payload) => listChecks(payload, 'players', { pagination: true }),
    },
    'players-keyword': {
        id: 'players-keyword',
        fixture: keywordFixture,
        path: ({ page, keyWord }) => `/api/players?${encodedQuery({ page, keyWord })}`,
        checks: (payload, { keyWord }) => listChecks(payload, 'players', {
            pagination: true,
            each: (player) => String(player.playerName || '').toLowerCase().includes(keyWord.toLowerCase()),
        }),
    },
    'players-league': {
        id: 'players-league',
        fixture: () => ({ page: postPage(), ...leagueFixture() }),
        path: ({ page, leagueCode }) => `/api/players?${encodedQuery({ page, leagueCode })}`,
        checks: (payload) => listChecks(payload, 'players', { pagination: true }),
    },
    'players-team': {
        id: 'players-team',
        fixture: teamFixture,
        path: ({ page, teamId }) => `/api/players?${encodedQuery({ page, teamId })}`,
        checks: (payload) => listChecks(payload, 'players', { pagination: true }),
    },
    'player-detail': {
        id: 'player-detail',
        fixture: playerFixture,
        path: ({ playerId }) => `/api/players/${playerId}`,
        checks: (payload, { playerId }) => ({
            'response playerId matches request': () => Number(payload?.playerId) === playerId,
            'response has playerName': () => typeof payload?.playerName === 'string' && payload.playerName.length > 0,
        }),
    },
    'teams-list': {
        id: 'teams-list',
        path: () => '/api/teams',
        checks: (payload) => listChecks(payload, 'teams'),
    },
    'teams-league': {
        id: 'teams-league',
        fixture: leagueFixture,
        path: ({ leagueCode }) => `/api/teams/league?${encodedQuery({ leagueCode })}`,
        checks: (payload) => listChecks(payload, 'teams'),
    },
    'team-detail': {
        id: 'team-detail',
        fixture: teamFixture,
        path: ({ teamId }) => `/api/teams/${teamId}`,
        checks: (payload, { teamId }) => ({
            'response teamId matches request': () => Number(payload?.teamId) === teamId,
            'response has teamName': () => typeof payload?.teamName === 'string' && payload.teamName.length > 0,
        }),
    },
    'team-info': {
        id: 'team-info',
        fixture: teamFixture,
        path: ({ teamId, page }) => `/api/teams/test/${teamId}?${encodedQuery({ page })}`,
        checks: (payload, { teamId }) => ({
            'response team matches request': () => Number(payload?.teams?.teamId) === teamId,
            'response has team players collection': () => Array.isArray(payload?.players?.teamPlayers),
            'response has team posts collection': () => Array.isArray(payload?.posts?.posts),
        }),
    },
    'team-players': {
        id: 'team-players',
        fixture: teamFixture,
        path: ({ teamId }) => `/api/team-players/${teamId}`,
        checks: (payload) => listChecks(payload, 'teamPlayers'),
    },
    'transfers-list': {
        id: 'transfers-list',
        fixture: () => ({ page: postPage() }),
        path: ({ page }) => `/api/transfers?${encodedQuery({ page })}`,
        checks: (payload) => listChecks(payload, 'transfers', { pagination: true }),
    },
    'transfers-keyword': {
        id: 'transfers-keyword',
        fixture: keywordFixture,
        path: ({ page, keyWord }) => `/api/transfers?${encodedQuery({ page, keyWord })}`,
        checks: (payload, { keyWord }) => listChecks(payload, 'transfers', {
            pagination: true,
            each: (transfer) => String(transfer.playerName || '').toLowerCase().includes(keyWord.toLowerCase()),
        }),
    },
    'transfers-league': {
        id: 'transfers-league',
        fixture: () => ({ page: postPage(), ...leagueFixture() }),
        path: ({ page, leagueCode }) => `/api/transfers?${encodedQuery({ page, leagueCode })}`,
        checks: (payload) => listChecks(payload, 'transfers', { pagination: true }),
    },
    'transfers-team-filter': {
        id: 'transfers-team-filter',
        fixture: teamFixture,
        path: ({ page, teamId }) => `/api/transfers?${encodedQuery({ page, teamId })}`,
        checks: (payload, { teamId }) => listChecks(payload, 'transfers', {
            pagination: true,
            each: (transfer) => Number(transfer.inTeamId) === teamId || Number(transfer.outTeamId) === teamId,
        }),
    },
    'team-transfers': {
        id: 'team-transfers',
        fixture: teamFixture,
        path: ({ page, teamId }) => `/api/transfers/team/${teamId}?${encodedQuery({ page })}`,
        checks: (payload, { teamId }) => listChecks(payload, 'transfers', {
            pagination: true,
            each: (transfer) => Number(transfer.inTeamId) === teamId || Number(transfer.outTeamId) === teamId,
        }),
    },
    'player-transfers': {
        id: 'player-transfers',
        fixture: playerFixture,
        path: ({ playerId }) => `/api/players/${playerId}/transfers`,
        checks: (payload) => listChecks(payload, 'playerTransfers'),
    },
    'transfer-detail': {
        id: 'transfer-detail',
        fixture: transferFixture,
        path: ({ transferId }) => `/api/transfers/${transferId}`,
        checks: (payload, { transferId }) => ({
            'response transfer matches request': () => Number(payload?.transfer?.transferId) === transferId,
        }),
    },
    'member-me': {
        id: 'member-me',
        requiresAuth: true,
        expectJsonObject: false,
        path: () => '/api/members/me',
        checks: (payload) => ({ 'response memberId is positive': () => Number.isInteger(payload) && payload > 0 }),
    },
    'member-favorite-teams': {
        id: 'member-favorite-teams',
        requiresAuth: true,
        fixture: authFixture,
        path: ({ page }) => `/api/members/favorite-teams?${encodedQuery({ page })}`,
        checks: (payload) => ({
            'response has favorite team array': () => Array.isArray(payload?.teamDetails),
            'response has favorite team post array': () => Array.isArray(payload?.postDetails),
        }),
    },
    'member-favorite-players': {
        id: 'member-favorite-players',
        requiresAuth: true,
        path: () => '/api/members/me/players/following',
        checks: (payload) => listChecks(payload, 'memberFollowPlayers', { nonEmpty: false }),
    },
    'member-favorite-player-detail': {
        id: 'member-favorite-player-detail',
        requiresAuth: true,
        fixture: playerFixture,
        path: ({ playerId }) => `/api/members/me/players/${playerId}/details`,
        checks: (payload, { playerId }) => ({
            'response has player detail': () => Number(payload?.playerItemResponseDto?.playerId) === playerId,
            'response has player transfers': () => Array.isArray(payload?.playerTransfersResponseDto?.playerTransfers),
        }),
    },
    'member-followed-journalists': {
        id: 'member-followed-journalists',
        requiresAuth: true,
        path: () => '/api/members/me/journalists/following',
        checks: (payload) => listChecks(payload, 'memberFollowJournalists', { nonEmpty: false }),
    },
    'member-journalist-posts': {
        id: 'member-journalist-posts',
        requiresAuth: true,
        fixture: journalistFixture,
        path: ({ journalist, page }) => `/api/journalists/${encodeURIComponent(journalist)}/posts?${encodedQuery({ page })}`,
        checks: (payload, { journalist }) => listChecks(payload, 'posts', {
            pagination: true,
            each: (post) => post.journalist === journalist,
        }),
    },
    'member-journalists-posts': {
        id: 'member-journalists-posts',
        requiresAuth: true,
        fixture: authFixture,
        path: ({ page }) => `/api/members/me/journalists/posts?${encodedQuery({ page })}`,
        checks: (payload) => listChecks(payload, 'allJournalistsPosts', { nonEmpty: false }),
    },
};

export function getReadCase(id) {
    const definition = definitions[id];
    return definition ? createReadCase(definition) : undefined;
}

export function supportedReadCaseIds() {
    return Object.keys(definitions);
}
