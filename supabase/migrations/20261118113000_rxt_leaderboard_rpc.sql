CREATE OR REPLACE FUNCTION public.rxt_get_leaderboard()
RETURNS TABLE(rank bigint, display_name text, total_score numeric, tests_taken bigint)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
BEGIN
  IF auth.uid() IS NULL THEN RAISE EXCEPTION 'Authentication required'; END IF;
  RETURN QUERY
  SELECT
    row_number() OVER (ORDER BY sum(a.score) DESC, count(*) DESC, max(a.completed_at) DESC) AS rank,
    coalesce(nullif(p.display_name, ''), 'Student') AS display_name,
    sum(a.score)::numeric AS total_score,
    count(*)::bigint AS tests_taken
  FROM public.rxt_attempts a
  JOIN public.rxt_profiles p ON p.id = a.user_id
  GROUP BY p.id, p.display_name
  ORDER BY sum(a.score) DESC, count(*) DESC, max(a.completed_at) DESC
  LIMIT 25;
END;
$$;
REVOKE ALL ON FUNCTION public.rxt_get_leaderboard() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.rxt_get_leaderboard() TO authenticated;