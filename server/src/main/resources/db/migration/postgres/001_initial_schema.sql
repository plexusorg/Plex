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

CREATE INDEX IF NOT EXISTS idx_players_name ON players(name);

CREATE TABLE IF NOT EXISTS punishments (
    id BIGSERIAL PRIMARY KEY,
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

CREATE INDEX IF NOT EXISTS idx_punishments_punished ON punishments(punished);
CREATE INDEX IF NOT EXISTS idx_punishments_ip ON punishments(ip);

CREATE TABLE IF NOT EXISTS notes (
    row_id BIGSERIAL PRIMARY KEY,
    id INT NOT NULL,
    uuid VARCHAR(46) NOT NULL,
    written_by VARCHAR(46),
    note VARCHAR(2000),
    timestamp BIGINT
);

CREATE INDEX IF NOT EXISTS idx_notes_uuid ON notes(uuid);

CREATE TABLE IF NOT EXISTS player_ips (
    id BIGSERIAL PRIMARY KEY,
    player_uuid VARCHAR(46) NOT NULL,
    ip VARCHAR(64) NOT NULL,
    CONSTRAINT uq_player_ips_player_ip UNIQUE (player_uuid, ip)
);

CREATE INDEX IF NOT EXISTS idx_player_ips_ip ON player_ips(ip);
