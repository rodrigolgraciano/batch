DROP TABLE race IF EXISTS;

CREATE TABLE race (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    PILOT VARCHAR(30) NOT NULL,
    POSITION INT NOT NULL
);
