INSERT INTO public.rxt_subjects (name, icon, sort_order, is_published)
SELECT seed.name, seed.icon, seed.sort_order, true
FROM (VALUES
  ('Hindi', '📘', 1),
  ('English', '📖', 2),
  ('Mathematics', '➗', 3),
  ('Science', '🔬', 4),
  ('Social Science', '🌍', 5)
) AS seed(name, icon, sort_order)
WHERE NOT EXISTS (
  SELECT 1 FROM public.rxt_subjects existing
  WHERE lower(existing.name) = lower(seed.name)
);