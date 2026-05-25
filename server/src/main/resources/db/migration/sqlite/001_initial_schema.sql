CREATE TABLE IF NOT EXISTS players (
    uuid VARCHAR(46) NOT NULL PRIMARY KEY,
    last_known_name VARCHAR(18),
    login_msg VARCHAR(2000),
    prefix VARCHAR(2000),
    staffChat BOOLEAN,
    ips VARCHAR(2000),
    commandspy BOOLEAN
);

CREATE TABLE IF NOT EXISTS punishments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    punished_uuid VARCHAR(46) NOT NULL,
    punisher_uuid VARCHAR(46),
    source VARCHAR(30),
    punisher_reference VARCHAR(200),
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
CREATE INDEX IF NOT EXISTS idx_notes_uuid ON notes(uuid);
CREATE UNIQUE INDEX IF NOT EXISTS uq_player_ips_player_ip ON player_ips(player_uuid, ip);
CREATE INDEX IF NOT EXISTS idx_player_ips_ip ON player_ips(ip);
