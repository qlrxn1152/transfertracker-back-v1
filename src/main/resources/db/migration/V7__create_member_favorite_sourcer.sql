CREATE TABLE member_favorite_sourcer(
    member_favorite_sourcer_id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    journalist ENUM('FABRIZIO_ROMANO', 'DAVID_ORNSTEIN', 'MATTEO_MORETTO', 'FLORIAN_PLETTENBERG', 'SANTI_AOUNA', 'DI_MARZIO') NOT NULL,

    PRIMARY KEY (member_favorite_sourcer_id),

    CONSTRAINT uk_member_favorite_sourcer_member_journalist UNIQUE (member_id, journalist),

    CONSTRAINT fk_member_favorite_sourcer_member FOREIGN KEY (member_id) REFERENCES member(member_id)

);