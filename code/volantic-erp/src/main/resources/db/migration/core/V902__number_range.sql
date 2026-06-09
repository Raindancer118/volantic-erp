-- Gap-free document number ranges (GoBD, DB architecture §5.1). The counter is advanced under a
-- pessimistic row lock inside the caller's transaction, so a rollback rolls the number back too.

CREATE TABLE core.number_range (
    id         UUID         NOT NULL PRIMARY KEY,
    version    BIGINT       NOT NULL,
    range_key  VARCHAR(100) NOT NULL UNIQUE,    -- e.g. 'sales.invoice'
    prefix     VARCHAR(50)  NOT NULL,           -- e.g. 'RE-'
    padding    INTEGER      NOT NULL,           -- zero-pad width of the numeric part
    next_value BIGINT       NOT NULL            -- next number to be allocated
);
