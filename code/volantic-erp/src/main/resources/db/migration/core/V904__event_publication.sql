-- Spring Modulith event publication registry (Outbox). Persists domain-event publications so they
-- survive a crash and are retried until completed. The JDBC registry uses the unqualified table name
-- 'event_publication', which resolves via the Postgres search_path to the public schema — created here
-- explicitly (Flyway's default schema is core).

CREATE TABLE public.event_publication (
    id               UUID                     NOT NULL,
    listener_id      TEXT                     NOT NULL,
    event_type       TEXT                     NOT NULL,
    serialized_event TEXT                     NOT NULL,
    publication_date TIMESTAMP WITH TIME ZONE NOT NULL,
    completion_date  TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id)
);

CREATE INDEX event_publication_serialized_event_hash_idx
    ON public.event_publication USING HASH (serialized_event);

CREATE INDEX event_publication_by_completion_date_idx
    ON public.event_publication (completion_date);
