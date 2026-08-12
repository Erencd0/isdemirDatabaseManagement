-- Gives the heats that were recorded before operators existed an operator.
--
-- Every heat from now on gets one from the form, but the ~1400 rows already in the table have
-- operator_id NULL, so "Detay Gör" shows an empty operator box for all of them. This spreads
-- the existing operators over those rows.
--
-- INACTIVE OPERATORS ARE INCLUDED ON PURPOSE: the "Bu operatör artık çalışmamaktadır" warning
-- only has something to show if some past heats belong to operators who have left.
--
-- The mapping is dokum_id % (number of operators), so it is deterministic (the same heat always
-- gets the same operator) and spreads evenly - no random(), which would give a different result
-- on every database. The operators are addressed by their position, not by hardcoded ids, so
-- this also works where the identity column handed out different numbers.
--
-- WHERE operator_id IS NULL: heats that already have an operator are never overwritten, which
-- also makes re-running this harmless.

UPDATE public.dokum_tablosu d
SET operator_id = o.id
FROM (
    SELECT id,
           row_number() OVER (ORDER BY id) - 1 AS position,
           count(*) OVER ()                    AS total
    FROM public.operator_tablosu
) o
WHERE d.operator_id IS NULL
  AND o.position = d.dokum_id % o.total;
