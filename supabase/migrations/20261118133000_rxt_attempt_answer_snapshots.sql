CREATE OR REPLACE FUNCTION public.rxt_submit_attempt(p_test_id uuid, p_answers jsonb)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
  v_user_id uuid := auth.uid();
  v_test public.rxt_tests%ROWTYPE;
  v_question record;
  v_selected integer;
  v_correct integer := 0;
  v_total integer := 0;
  v_details jsonb := '[]'::jsonb;
  v_attempt_id uuid;
  v_answer_count integer;
  v_unique_answer_count integer;
BEGIN
  IF v_user_id IS NULL THEN RAISE EXCEPTION 'Authentication required'; END IF;
  IF jsonb_typeof(p_answers) <> 'array' THEN RAISE EXCEPTION 'Answers must be a JSON array'; END IF;

  SELECT * INTO v_test
  FROM public.rxt_tests
  WHERE id = p_test_id AND is_published = true;
  IF NOT FOUND THEN RAISE EXCEPTION 'Test is unavailable'; END IF;

  v_total := COALESCE(array_length(v_test.question_ids, 1), 0);
  IF v_total = 0 THEN RAISE EXCEPTION 'Test has no questions'; END IF;

  SELECT count(*), count(DISTINCT value->>'question_id')
  INTO v_answer_count, v_unique_answer_count
  FROM jsonb_array_elements(p_answers);
  IF v_answer_count <> v_total OR v_unique_answer_count <> v_total THEN
    RAISE EXCEPTION 'Provide exactly one answer for every test question';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM jsonb_array_elements(p_answers) a(value)
    WHERE NOT ((a.value->>'question_id')::uuid = ANY(v_test.question_ids))
  ) THEN
    RAISE EXCEPTION 'Answer contains a question outside this test';
  END IF;

  FOR v_question IN
    SELECT q.id, q.question, q.options, q.correct_option, q.explanation
    FROM public.rxt_questions q
    WHERE q.id = ANY(v_test.question_ids) AND q.is_published = true
    ORDER BY array_position(v_test.question_ids, q.id)
  LOOP
    SELECT CASE
      WHEN (a.value->>'selected') IS NULL OR a.value->>'selected' = 'null' THEN NULL
      ELSE (a.value->>'selected')::integer
    END INTO v_selected
    FROM jsonb_array_elements(p_answers) a(value)
    WHERE a.value->>'question_id' = v_question.id::text
    LIMIT 1;

    IF v_selected IS NOT NULL
       AND (v_selected < 0 OR v_selected >= jsonb_array_length(v_question.options)) THEN
      RAISE EXCEPTION 'Selected option is out of range';
    END IF;

    IF v_selected IS NOT NULL AND v_selected = v_question.correct_option THEN
      v_correct := v_correct + 1;
    END IF;

    v_details := v_details || jsonb_build_array(jsonb_build_object(
      'question_id', v_question.id,
      'question', v_question.question,
      'options', v_question.options,
      'selected', v_selected,
      'correct_option', v_question.correct_option,
      'correct', (v_selected IS NOT NULL AND v_selected = v_question.correct_option),
      'explanation', v_question.explanation
    ));
  END LOOP;

  IF jsonb_array_length(v_details) <> v_total THEN
    RAISE EXCEPTION 'One or more test questions are no longer published';
  END IF;

  INSERT INTO public.rxt_attempts
    (user_id, test_id, total_questions, correct_count, wrong_count, score, answers)
  VALUES
    (v_user_id, p_test_id, v_total, v_correct, v_total - v_correct, v_correct, v_details)
  RETURNING id INTO v_attempt_id;

  RETURN jsonb_build_object(
    'attempt_id', v_attempt_id,
    'total_questions', v_total,
    'correct_count', v_correct,
    'wrong_count', v_total - v_correct,
    'score', v_correct,
    'answers', v_details
  );
END;
$$;
REVOKE ALL ON FUNCTION public.rxt_submit_attempt(uuid, jsonb) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.rxt_submit_attempt(uuid, jsonb) TO authenticated;