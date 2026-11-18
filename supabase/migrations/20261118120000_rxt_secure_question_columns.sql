-- Make the student question view obey RLS and expose only non-answer columns.
ALTER VIEW public.rxt_student_questions SET (security_invoker = true);

REVOKE SELECT ON public.rxt_questions FROM authenticated;
GRANT SELECT (id, chapter_id, question, options, difficulty, is_published, created_at)
  ON public.rxt_questions TO authenticated;

DROP POLICY IF EXISTS "rxt questions read published" ON public.rxt_questions;
CREATE POLICY "rxt questions read published"
ON public.rxt_questions
FOR SELECT
TO authenticated
USING (is_published = true OR rxt_is_admin());

-- Admin-only RPC returns answer keys for the authoring/review interface.
CREATE OR REPLACE FUNCTION public.rxt_admin_list_questions(p_chapter_id uuid DEFAULT NULL)
RETURNS TABLE(
  id uuid,
  chapter_id uuid,
  question text,
  options jsonb,
  correct_option integer,
  explanation text,
  difficulty text,
  source_note_id uuid,
  is_published boolean,
  created_at timestamptz
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
BEGIN
  IF NOT public.rxt_is_admin() THEN
    RAISE EXCEPTION 'Admin access required';
  END IF;
  RETURN QUERY
  SELECT q.id, q.chapter_id, q.question, q.options, q.correct_option,
         q.explanation, q.difficulty, q.source_note_id, q.is_published, q.created_at
  FROM public.rxt_questions q
  WHERE p_chapter_id IS NULL OR q.chapter_id = p_chapter_id
  ORDER BY q.created_at DESC
  LIMIT 200;
END;
$$;
REVOKE ALL ON FUNCTION public.rxt_admin_list_questions(uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.rxt_admin_list_questions(uuid) TO authenticated;