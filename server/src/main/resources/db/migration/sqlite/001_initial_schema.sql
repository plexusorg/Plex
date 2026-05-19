CREATE TABLE IF NOT EXISTS players (
    uuid VARCHAR(46) NOT NULL PRIMARY KEY,
    name VARCHAR(18),
    login_msg VARCHAR(2000),
    prefix VARCHAR(2000),
    staffChat BOOLEAN,
    ips VARCHAR(2000),
    coins BIGINT,
    vanished BOOLEAN,
    commandspy BOOLEAN
);

CREATE TABLE IF NOT EXISTS punishments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    punished VARCHAR(46) NOT NULL,
    punisher VARCHAR(46),
    punisherName VARCHAR(64),
    punishedUsername VARCHAR(16),
    ip VARCHAR(2000),
    type VARCHAR(30),
    reason VARCHAR(2000),
    customTime BOOLEAN,
    active BOOLEAN,
    issueDate BIGINT NOT NULL,
    endDate BIGINT
);

CREATE TABLE IF NOT EXISTS notes (
    row_id INTEGER PRIMARY KEY AUTOINCREMENT,
    id INT NOT NULL,
    uuid VARCHAR(46) NOT NULL,
    written_by VARCHAR(46),
    note VARCHAR(2000),
    timestamp BIGINT
);

CREATE TABLE IF NOT EXISTS player_ips (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid VARCHAR(46) NOT NULL,
    ip VARCHAR(64) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_players_name ON players(name);
CREATE INDEX IF NOT EXISTS idx_punishments_punished ON punishments(punished);
CREATE INDEX IF NOT EXISTS idx_punishments_ip ON punishments(ip);
CREATE INDEX IF NOT EXISTS idx_notes_uuid ON notes(uuid);
CREATE UNIQUE INDEX IF NOT EXISTS uq_player_ips_player_ip ON player_ips(player_uuid, ip);
CREATE INDEX IF NOT EXISTS idx_player_ips_ip ON player_ips(ip);
