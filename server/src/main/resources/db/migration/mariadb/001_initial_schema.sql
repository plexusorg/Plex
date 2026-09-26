CREATE TABLE IF NOT EXISTS `players` (
    `uuid` VARCHAR(46) NOT NULL,
    `last_known_name` VARCHAR(18),
    `login_msg` VARCHAR(2000),
    `prefix` VARCHAR(2000),
    `staffChat` BOOLEAN,
    `commandspy` BOOLEAN,
    PRIMARY KEY (`uuid`),
    INDEX `idx_players_last_known_name` (`last_known_name`)
);

CREATE TABLE IF NOT EXISTS `punishments` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `punished_uuid` VARCHAR(46) NOT NULL,
    `punisher_uuid` VARCHAR(46),
    `source` VARCHAR(30) NOT NULL CHECK (`source` IN ('PLAYER', 'CONSOLE', 'WEB')),
    `punisher_reference` VARCHAR(200),
    `ip` VARCHAR(2000),
    `ip_match_key` VARCHAR(64) NOT NULL,
    `type` VARCHAR(30) NOT NULL CHECK (`type` IN ('MUTE', 'FREEZE', 'BAN', 'TEMPBAN', 'KICK', 'SMITE')),
    `reason` VARCHAR(2000) NOT NULL,
    `active` BOOLEAN NOT NULL,
    `issueDate` BIGINT NOT NULL,
    `endDate` BIGINT,
    CHECK ((`type` IN ('MUTE', 'FREEZE', 'BAN', 'TEMPBAN') AND `endDate` IS NOT NULL)
        OR (`type` IN ('KICK', 'SMITE') AND `endDate` IS NULL)),
    CHECK (`type` NOT IN ('KICK', 'SMITE') OR `active` = FALSE),
    CHECK (`type` <> 'BAN' OR `endDate` = `issueDate` + 86400000),
    CHECK (`type` <> 'MUTE' OR `endDate` <= `issueDate` + 604800000),
    PRIMARY KEY (`id`),
    INDEX `idx_punishments_punished` (`punished_uuid`),
    INDEX `idx_punishments_ip` (`ip`(64)),
    INDEX `idx_punishments_ip_match_key` (`ip_match_key`, `active`, `endDate`)
);

CREATE TABLE IF NOT EXISTS `notes` (
    `row_id` BIGINT NOT NULL AUTO_INCREMENT,
    `id` INT NOT NULL,
    `uuid` VARCHAR(46) NOT NULL,
    `written_by_uuid` VARCHAR(46),
    `note` VARCHAR(2000),
    `timestamp` BIGINT,
    PRIMARY KEY (`row_id`),
    INDEX `idx_notes_uuid` (`uuid`)
);

CREATE TABLE IF NOT EXISTS `player_ips` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `player_uuid` VARCHAR(46) NOT NULL,
    `ip` VARCHAR(64) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_player_ips_player_ip` (`player_uuid`, `ip`),
    INDEX `idx_player_ips_ip` (`ip`)
);

CREATE TABLE IF NOT EXISTS `player_module_data` (
    `player_uuid` VARCHAR(46) NOT NULL,
    `module` VARCHAR(100) NOT NULL,
    `data_key` VARCHAR(64) NOT NULL,
    `value_json` LONGTEXT NOT NULL,
    `updated_at` BIGINT NOT NULL,
    PRIMARY KEY (`player_uuid`, `module`, `data_key`)
);

CREATE TABLE IF NOT EXISTS ip_bans (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    target VARCHAR(64) NOT NULL,
    reason VARCHAR(2000) NOT NULL,
    punisher_uuid VARCHAR(46),
    source VARCHAR(30) NOT NULL CHECK (source IN ('PLAYER', 'CONSOLE', 'WEB')),
    punisher_reference VARCHAR(200),
    issueDate BIGINT NOT NULL,
    endDate BIGINT NOT NULL,
    active BOOLEAN NOT NULL,
    CHECK (endDate = issueDate + 86400000),
    INDEX idx_ip_bans_active (active, endDate),
    INDEX idx_ip_bans_target (target, active, endDate)
);

CREATE TABLE IF NOT EXISTS name_bans (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    target VARCHAR(18) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    reason VARCHAR(2000) NOT NULL,
    punisher_uuid VARCHAR(46),
    source VARCHAR(30) NOT NULL CHECK (source IN ('PLAYER', 'CONSOLE', 'WEB')),
    punisher_reference VARCHAR(200),
    issueDate BIGINT NOT NULL,
    endDate BIGINT NOT NULL,
    active BOOLEAN NOT NULL,
    CHECK (endDate = issueDate + 86400000),
    CHECK (target = LOWER(target)),
    INDEX idx_name_bans_active (active, endDate),
    INDEX idx_name_bans_target (target, active, endDate)
);

CREATE TABLE IF NOT EXISTS target_ban_locks (
    kind VARCHAR(4) PRIMARY KEY CHECK (kind IN ('IP', 'NAME')),
    revision BIGINT NOT NULL
);

INSERT INTO target_ban_locks (kind, revision) VALUES ('IP', 0), ('NAME', 0);
