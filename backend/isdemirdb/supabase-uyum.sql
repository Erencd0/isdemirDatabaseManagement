-- Supabase (mobil uygulama) semasinda olmayan, bu backend'in kullandigi kolonlar.
-- Hepsi nullable ya da DEFAULT'lu: mobil uygulamanin mevcut INSERT'leri etkilenmez.
-- Calistirma:
--   psql -h aws-0-eu-central-1.pooler.supabase.com -p 5432 \
--        -U postgres.ktxbzifcfxvjcclwodux -d postgres -f supabase-uyum.sql

ALTER TABLE dokum_tablosu    ADD COLUMN IF NOT EXISTS kayit_zamani timestamp;
ALTER TABLE "malzeme_Tanim"  ADD COLUMN IF NOT EXISTS aktif_pasif boolean NOT NULL DEFAULT true;
ALTER TABLE malzeme_kullanim ADD COLUMN IF NOT EXISTS kullanici_id bigint;
ALTER TABLE malzeme_kullanim ADD COLUMN IF NOT EXISTS islem_zamani timestamp DEFAULT LOCALTIMESTAMP;
ALTER TABLE operator         ADD COLUMN IF NOT EXISTS operator_rolu varchar(50) NOT NULL DEFAULT 'dokumcu';
