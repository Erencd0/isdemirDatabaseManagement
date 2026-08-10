-- refresh_tokens.kullanici_id was bigint while the column it references,
-- kullanici.kullanici_id, is integer. PostgreSQL permits a foreign key across
-- int8/int4, so this went unnoticed, and the RefreshToken entity maps the field as
-- Integer -- a third spelling of the same value.
--
-- (Checked: Hibernate's ddl-auto=validate does NOT reject this mismatch, so it was
-- never going to fail at startup. It is fixed here because a foreign key that
-- disagrees with its own primary key is a trap for whoever touches it next, not
-- because anything was breaking.)
--
-- integer is the correct side to land on: it matches the referenced primary key.
-- Existing values are small (max id 8 at time of writing), so there is nothing to
-- truncate. PostgreSQL rebuilds the foreign key automatically as part of the ALTER.
ALTER TABLE public.refresh_tokens
    ALTER COLUMN kullanici_id TYPE integer;
