CREATE TABLE IF NOT EXISTS `players` (
    `uuid` VARCHAR(46) NOT NULL,
    `last_known_name` VARCHAR(18),
    `login_msg` VARCHAR(2000),
    `prefix` VARCHAR(2000),
    `staffChat` BOOLEAN,
    `ips` VARCHAR(2000),
    `commandspy` BOOLEAN,
    PRIMARY KEY (`uuid`),
    INDEX `idx_players_last_known_name` (`last_known_name`)
);

CREATE TABLE IF NOT EXISTS `punishments` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `punished_uuid` VARCHAR(46) NOT NULL,
    `punisher_uuid` VARCHAR(46),
    `source` VARCHAR(30),
    `punisher_reference` VARCHAR(200),
    `ip` VARCHAR(2000),
    `type` VARCHAR(30),
    `reason` VARCHAR(2000),
    `customTime` BOOLEAN,
    `active` BOOLEAN,
    `issueDate` BIGINT NOT NULL,
    `endDate` BIGINT,
    PRIMARY KEY (`id`),
    INDEX `idx_punishments_punished` (`punished_uuid`),
    INDEX `idx_punishments_ip` (`ip`(64))
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
