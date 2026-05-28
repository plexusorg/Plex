CREATE TABLE IF NOT EXISTS players (
    uuid VARCHAR(46) NOT NULL PRIMARY KEY,
    last_known_name VARCHAR(18),
    login_msg VARCHAR(2000),
    prefix VARCHAR(2000),
    staffChat BOOLEAN,
    commandspy BOOLEAN
);

CREATE INDEX IF NOT EXISTS idx_players_last_known_name ON players(last_known_name);

CREATE TABLE IF NOT EXISTS punishments (
    id BIGSERIAL PRIMARY KEY,
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

CREATE INDEX IF NOT EXISTS idx_punishments_punished ON punishments(punished_uuid);
CREATE INDEX IF NOT EXISTS idx_punishments_ip ON punishments(ip);

CREATE TABLE IF NOT EXISTS notes (
    row_id BIGSERIAL PRIMARY KEY,
    id INT NOT NULL,
    uuid VARCHAR(46) NOT NULL,
    written_by_uuid VARCHAR(46),
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

CREATE TABLE IF NOT EXISTS player_module_data (
    player_uuid VARCHAR(46) NOT NULL,
    module VARCHAR(100) NOT NULL,
    data_key VARCHAR(64) NOT NULL,
    value_json TEXT NOT NULL,
    updated_at BIGINT NOT NULL,
    PRIMARY KEY (player_uuid, module, data_key)
);
