CREATE TABLE creator (
    creator_id BIGINT NOT NULL AUTO_INCREMENT,

    name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (creator_id),
    UNIQUE KEY uk_creator_email (email)

) ENGINE=InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE classmate (
    classmate_id BIGINT NOT NULL AUTO_INCREMENT,

    name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (classmate_id),
    UNIQUE KEY uk_classmate_email (email)

) ENGINE=InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE class_room (
    class_room_id BIGINT NOT NULL AUTO_INCREMENT,
    creator_id BIGINT NOT NULL,

    title VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    price BIGINT NOT NULL,
    capacity INT NOT NULL,
    enrollment_count INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (class_room_id),

    CONSTRAINT fk_class_room_creator
        FOREIGN KEY (creator_id)
            REFERENCES creator (creator_id),

    CONSTRAINT chk_class_room_price
        CHECK (price >= 0),

    CONSTRAINT chk_class_room_capacity
        CHECK (capacity > 0),

    CONSTRAINT chk_class_room_enrolled_count
        CHECK (enrollment_count >= 0 AND enrollment_count <= capacity),

    CONSTRAINT chk_class_room_status
        CHECK (status IN ('DRAFT', 'OPEN', 'CLOSED')),

    CONSTRAINT chk_class_room_period
        CHECK (start_at < end_at),

    KEY idx_class_room_creator_status_created_at (creator_id, status, created_at DESC),
    KEY idx_class_room_status_created_at (status, created_at DESC)

) ENGINE=InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


CREATE TABLE enrollment (
    enrollment_id BIGINT NOT NULL AUTO_INCREMENT,
    class_room_id BIGINT NOT NULL,
    classmate_id BIGINT NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_expired_at DATETIME NOT NULL,
    confirmed_at DATETIME NULL,
    cancelled_at DATETIME NULL,
    cancel_reason VARCHAR(30) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (enrollment_id),

    CONSTRAINT fk_enrollment_class_room
        FOREIGN KEY (class_room_id)
            REFERENCES class_room (class_room_id),

    CONSTRAINT fk_enrollment_classmate
        FOREIGN KEY (classmate_id)
            REFERENCES classmate (classmate_id),

    CONSTRAINT uk_enrollment_class_room_classmate
        UNIQUE (class_room_id, classmate_id),

    CONSTRAINT chk_enrollment_status
        CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED')),

    KEY idx_enrollment_classmate_created_at (classmate_id, created_at DESC),
    KEY idx_enrollment_class_room_status_created_at (class_room_id, status, created_at DESC),
    KEY idx_enrollment_status_payment_expired_at (status, payment_expired_at)

) ENGINE=InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
