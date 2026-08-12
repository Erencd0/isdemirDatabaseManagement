-- Operators (the people who run a heat) and the dokum -> operator link.
--
-- operator_tablosu was created by hand on the development database, so it is NOT in the
-- V1 baseline. CREATE TABLE IF NOT EXISTS covers both cases: the existing database keeps
-- its table, an empty one (a new container) gets it here.
--
-- aktif = the operator still works here. false means retired/left: they may not be picked
-- for a new heat, but they stay on the old heats they actually ran.

CREATE TABLE IF NOT EXISTS public.operator_tablosu (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    operator_adi character varying(100),
    operator_soyadi character varying(100),
    operator_rolu character varying(100),
    aktif boolean
);

-- 10 operators. operator_rolu is 'dokumcu' for every one of them (the only role for now).
-- 3 of them are inactive on purpose: the "this operator no longer works here" case has to
-- be testable. The id is left to the identity column.
--
-- WHERE NOT EXISTS: seed only an empty table, so re-running this against a database that
-- already has operators cannot duplicate them.
INSERT INTO public.operator_tablosu (operator_adi, operator_soyadi, operator_rolu, aktif)
SELECT * FROM (VALUES
    ('Ahmet',   'Yılmaz',   'dokumcu', true),
    ('Mehmet',  'Kaya',     'dokumcu', true),
    ('Mustafa', 'Demir',    'dokumcu', true),
    ('Hüseyin', 'Şahin',    'dokumcu', true),
    ('Ali',     'Çelik',    'dokumcu', true),
    ('Osman',   'Yıldız',   'dokumcu', true),
    ('İbrahim', 'Aydın',    'dokumcu', true),
    ('Hasan',   'Öztürk',   'dokumcu', false),
    ('Murat',   'Arslan',   'dokumcu', false),
    ('Kemal',   'Doğan',    'dokumcu', false)
) AS seed (operator_adi, operator_soyadi, operator_rolu, aktif)
WHERE NOT EXISTS (SELECT 1 FROM public.operator_tablosu);

-- The link itself. Nullable: the 1438 heats already in the table were recorded before
-- operators existed and have none.
ALTER TABLE public.dokum_tablosu ADD COLUMN IF NOT EXISTS operator_id bigint;

ALTER TABLE public.dokum_tablosu
    DROP CONSTRAINT IF EXISTS fk_operator_id;

ALTER TABLE public.dokum_tablosu
    ADD CONSTRAINT fk_operator_id FOREIGN KEY (operator_id)
    REFERENCES public.operator_tablosu(id);
