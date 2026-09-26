CREATE TABLE IF NOT EXISTS players (
    uuid VARCHAR(46) NOT NULL PRIMARY KEY,
    last_known_name VARCHAR(18),
    login_msg VARCHAR(2000),
    prefix VARCHAR(2000),
    staffChat BOOLEAN,
    commandspy BOOLEAN
);

CREATE TABLE IF NOT EXISTS punishments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    punished_uuid VARCHAR(46) NOT NULL,
    punisher_uuid VARCHAR(46),
    source VARCHAR(30) NOT NULL CHECK (source IN ('PLAYER', 'CONSOLE', 'WEB')),
    punisher_reference VARCHAR(200),
    ip VARCHAR(2000),
    ip_match_key VARCHAR(64) NOT NULL,
    type VARCHAR(30) NOT NULL CHECK (type IN ('MUTE', 'FREEZE', 'BAN', 'TEMPBAN', 'KICK', 'SMITE')),
    reason VARCHAR(2000) NOT NULL,
    active BOOLEAN NOT NULL,
    issueDate BIGINT NOT NULL,
    endDate BIGINT,
    CHECK ((type IN ('MUTE', 'FREEZE', 'BAN', 'TEMPBAN') AND endDate IS NOT NULL)
        OR (type IN ('KICK', 'SMITE') AND endDate IS NULL)),
    CHECK (type NOT IN ('KICK', 'SMITE') OR active = FALSE),
    CHECK (type <> 'BAN' OR endDate = issueDate + 86400000),
    CHECK (type <> 'MUTE' OR endDate <= issueDate + 604800000)
);

CREATE TABLE IF NOT EXISTS notes (
    row_id INTEGER PRIMARY KEY AUTOINCREMENT,
    id INT NOT NULL,
    uuid VARCHAR(46) NOT NULL,
    written_by_uuid VARCHAR(46),
    note VARCHAR(2000),
    timestamp BIGINT
);

CREATE TABLE IF NOT EXISTS player_ips (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid VARCHAR(46) NOT NULL,
    ip VARCHAR(64) NOT NULL
);

CREATE TABLE IF NOT EXISTS player_module_data (
    player_uuid VARCHAR(46) NOT NULL,
    module VARCHAR(100) NOT NULL,
    data_key VARCHAR(64) NOT NULL,
    value_json TEXT NOT NULL,
    updated_at INTEGER NOT NULL,
    PRIMARY KEY (player_uuid, module, data_key)
);

CREATE INDEX IF NOT EXISTS idx_players_last_known_name ON players(last_known_name);
CREATE INDEX IF NOT EXISTS idx_punishments_punished ON punishments(punished_uuid);
CREATE INDEX IF NOT EXISTS idx_punishments_ip ON punishments(ip);
CREATE INDEX IF NOT EXISTS idx_punishments_ip_match_key ON punishments(ip_match_key, active, endDate);
CREATE INDEX IF NOT EXISTS idx_notes_uuid ON notes(uuid);
CREATE UNIQUE INDEX IF NOT EXISTS uq_player_ips_player_ip ON player_ips(player_uuid, ip);
CREATE INDEX IF NOT EXISTS idx_player_ips_ip ON player_ips(ip);

CREATE TABLE IF NOT EXISTS ip_bans (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    target VARCHAR(64) NOT NULL,
    reason VARCHAR(2000) NOT NULL,
    punisher_uuid VARCHAR(46),
    source VARCHAR(30) NOT NULL CHECK (source IN ('PLAYER', 'CONSOLE', 'WEB')),
    punisher_reference VARCHAR(200),
    issueDate BIGINT NOT NULL,
    endDate BIGINT NOT NULL,
    active BOOLEAN NOT NULL,
    CHECK (endDate = issueDate + 86400000)
);

CREATE INDEX IF NOT EXISTS idx_ip_bans_active ON ip_bans(active, endDate);
CREATE INDEX IF NOT EXISTS idx_ip_bans_target ON ip_bans(target, active, endDate);

CREATE TABLE IF NOT EXISTS name_bans (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    target VARCHAR(18) NOT NULL,
    reason VARCHAR(2000) NOT NULL,
    punisher_uuid VARCHAR(46),
    source VARCHAR(30) NOT NULL CHECK (source IN ('PLAYER', 'CONSOLE', 'WEB')),
    punisher_reference VARCHAR(200),
    issueDate BIGINT NOT NULL,
    endDate BIGINT NOT NULL,
    active BOOLEAN NOT NULL,
    CHECK (endDate = issueDate + 86400000),
    CHECK (target = LOWER(target))
);

CREATE INDEX IF NOT EXISTS idx_name_bans_active ON name_bans(active, endDate);
CREATE INDEX IF NOT EXISTS idx_name_bans_target ON name_bans(target, active, endDate);

CREATE TABLE IF NOT EXISTS target_ban_locks (
    kind VARCHAR(4) PRIMARY KEY CHECK (kind IN ('IP', 'NAME')),
    revision BIGINT NOT NULL
);

INSERT INTO target_ban_locks (kind, revision) VALUES ('IP', 0), ('NAME', 0);
