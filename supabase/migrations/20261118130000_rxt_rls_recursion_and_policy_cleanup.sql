-- RLS cleanup: remove recursive group-membership policy and duplicate permissive policies.
CREATE OR REPLACE FUNCTION public.rxt_is_group_member(p_group_id uuid)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
  SELECT auth.uid() IS NOT NULL
     AND EXISTS (
       SELECT 1 FROM public.rxt_group_members gm
       WHERE gm.group_id = p_group_id
         AND gm.user_id = auth.uid()
     );
$$;
REVOKE ALL ON FUNCTION public.rxt_is_group_member(uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.rxt_is_group_member(uuid) TO authenticated;

-- Subjects
DROP POLICY IF EXISTS "rxt subjects admin all" ON public.rxt_subjects;
DROP POLICY IF EXISTS "rxt subjects read published" ON public.rxt_subjects;
CREATE POLICY "rxt subjects read published" ON public.rxt_subjects
  FOR SELECT TO authenticated
  USING (is_published OR (SELECT public.rxt_is_admin()));
CREATE POLICY "rxt subjects admin insert" ON public.rxt_subjects
  FOR INSERT TO authenticated WITH CHECK ((SELECT public.rxt_is_admin()));
CREATE POLICY "rxt subjects admin update" ON public.rxt_subjects
  FOR UPDATE TO authenticated USING ((SELECT public.rxt_is_admin()))
  WITH CHECK ((SELECT public.rxt_is_admin()));
CREATE POLICY "rxt subjects admin delete" ON public.rxt_subjects
  FOR DELETE TO authenticated USING ((SELECT public.rxt_is_admin()));

-- Chapters
DROP POLICY IF EXISTS "rxt chapters admin all" ON public.rxt_chapters;
DROP POLICY IF EXISTS "rxt chapters read published" ON public.rxt_chapters;
CREATE POLICY "rxt chapters read published" ON public.rxt_chapters
  FOR SELECT TO authenticated
  USING (is_published OR (SELECT public.rxt_is_admin()));
CREATE POLICY "rxt chapters admin insert" ON public.rxt_chapters
  FOR INSERT TO authenticated WITH CHECK ((SELECT public.rxt_is_admin()));
CREATE POLICY "rxt chapters admin update" ON public.rxt_chapters
  FOR UPDATE TO authenticated USING ((SELECT public.rxt_is_admin()))
  WITH CHECK ((SELECT public.rxt_is_admin()));
CREATE POLICY "rxt chapters admin delete" ON public.rxt_chapters
  FOR DELETE TO authenticated USING ((SELECT public.rxt_is_admin()));

-- Questions: SELECT is limited by column grants; answer keys stay behind admin RPC.
DROP POLICY IF EXISTS "rxt questions admin all" ON public.rxt_questions;
DROP POLICY IF EXISTS "rxt questions read published" ON public.rxt_questions;
CREATE POLICY "rxt questions read published" ON public.rxt_questions
  FOR SELECT TO authenticated
  USING (is_published OR (SELECT public.rxt_is_admin()));
CREATE POLICY "rxt questions admin insert" ON public.rxt_questions
  FOR INSERT TO authenticated WITH CHECK ((SELECT public.rxt_is_admin()));
CREATE POLICY "rxt questions admin update" ON public.rxt_questions
  FOR UPDATE TO authenticated USING ((SELECT public.rxt_is_admin()))
  WITH CHECK ((SELECT public.rxt_is_admin()));
CREATE POLICY "rxt questions admin delete" ON public.rxt_questions
  FOR DELETE TO authenticated USING ((SELECT public.rxt_is_admin()));

-- Tests
DROP POLICY IF EXISTS "rxt tests admin all" ON public.rxt_tests;
DROP POLICY IF EXISTS "rxt tests read published" ON public.rxt_tests;
CREATE POLICY "rxt tests read published" ON public.rxt_tests
  FOR SELECT TO authenticated
  USING (is_published OR (SELECT public.rxt_is_admin()));
CREATE POLICY "rxt tests admin insert" ON public.rxt_tests
  FOR INSERT TO authenticated WITH CHECK ((SELECT public.rxt_is_admin()));
CREATE POLICY "rxt tests admin update" ON public.rxt_tests
  FOR UPDATE TO authenticated USING ((SELECT public.rxt_is_admin()))
  WITH CHECK ((SELECT public.rxt_is_admin()));
CREATE POLICY "rxt tests admin delete" ON public.rxt_tests
  FOR DELETE TO authenticated USING ((SELECT public.rxt_is_admin()));

-- Announcements
DROP POLICY IF EXISTS "rxt announcements admin all" ON public.rxt_announcements;
DROP POLICY IF EXISTS "rxt announcements read published" ON public.rxt_announcements;
CREATE POLICY "rxt announcements read published" ON public.rxt_announcements
  FOR SELECT TO authenticated
  USING (is_published OR (SELECT public.rxt_is_admin()));
CREATE POLICY "rxt announcements admin insert" ON public.rxt_announcements
  FOR INSERT TO authenticated WITH CHECK ((SELECT public.rxt_is_admin()));
CREATE POLICY "rxt announcements admin update" ON public.rxt_announcements
  FOR UPDATE TO authenticated USING ((SELECT public.rxt_is_admin()))
  WITH CHECK ((SELECT public.rxt_is_admin()));
CREATE POLICY "rxt announcements admin delete" ON public.rxt_announcements
  FOR DELETE TO authenticated USING ((SELECT public.rxt_is_admin()));

-- Groups
DROP POLICY IF EXISTS "rxt groups admin all" ON public.rxt_groups;
CREATE POLICY "rxt groups admin insert" ON public.rxt_groups
  FOR INSERT TO authenticated WITH CHECK ((SELECT public.rxt_is_admin()));
CREATE POLICY "rxt groups admin update" ON public.rxt_groups
  FOR UPDATE TO authenticated USING ((SELECT public.rxt_is_admin()))
  WITH CHECK ((SELECT public.rxt_is_admin()));
CREATE POLICY "rxt groups admin delete" ON public.rxt_groups
  FOR DELETE TO authenticated USING ((SELECT public.rxt_is_admin()));

-- Notes
DROP POLICY IF EXISTS "rxt notes admin all" ON public.rxt_notes;
DROP POLICY IF EXISTS "rxt notes read published chapters" ON public.rxt_notes;
CREATE POLICY "rxt notes read published chapters" ON public.rxt_notes
  FOR SELECT TO authenticated
  USING (
    (SELECT public.rxt_is_admin())
    OR EXISTS (
      SELECT 1 FROM public.rxt_chapters c
      JOIN public.rxt_subjects s ON s.id = c.subject_id
      WHERE c.id = rxt_notes.chapter_id
        AND c.is_published = true AND s.is_published = true
    )
  );
CREATE POLICY "rxt notes admin insert" ON public.rxt_notes
  FOR INSERT TO authenticated WITH CHECK ((SELECT public.rxt_is_admin()));
CREATE POLICY "rxt notes admin update" ON public.rxt_notes
  FOR UPDATE TO authenticated USING ((SELECT public.rxt_is_admin()))
  WITH CHECK ((SELECT public.rxt_is_admin()));
CREATE POLICY "rxt notes admin delete" ON public.rxt_notes
  FOR DELETE TO authenticated USING ((SELECT public.rxt_is_admin()));

-- Attempts are inserted only through rxt_submit_attempt.
DROP POLICY IF EXISTS "rxt attempts insert own" ON public.rxt_attempts;
DROP POLICY IF EXISTS "rxt attempts own or admin" ON public.rxt_attempts;
CREATE POLICY "rxt attempts own or admin" ON public.rxt_attempts
  FOR SELECT TO authenticated
  USING (user_id = (SELECT auth.uid()) OR (SELECT public.rxt_is_admin()));

-- Profiles
DROP POLICY IF EXISTS "rxt profile read self or admin" ON public.rxt_profiles;
DROP POLICY IF EXISTS "rxt profile update self or admin" ON public.rxt_profiles;
CREATE POLICY "rxt profile read self or admin" ON public.rxt_profiles
  FOR SELECT TO authenticated
  USING (id = (SELECT auth.uid()) OR (SELECT public.rxt_is_admin()));
CREATE POLICY "rxt profile update self or admin" ON public.rxt_profiles
  FOR UPDATE TO authenticated
  USING (id = (SELECT auth.uid()) OR (SELECT public.rxt_is_admin()))
  WITH CHECK (id = (SELECT auth.uid()) OR (SELECT public.rxt_is_admin()));

-- Membership policies without self-referential RLS recursion.
DROP POLICY IF EXISTS "rxt group members read" ON public.rxt_group_members;
DROP POLICY IF EXISTS "rxt join self" ON public.rxt_group_members;
DROP POLICY IF EXISTS "rxt leave self or admin" ON public.rxt_group_members;
CREATE POLICY "rxt group members read" ON public.rxt_group_members
  FOR SELECT TO authenticated
  USING (
    user_id = (SELECT auth.uid())
    OR (SELECT public.rxt_is_admin())
    OR (SELECT public.rxt_is_group_member(group_id))
  );
CREATE POLICY "rxt join self" ON public.rxt_group_members
  FOR INSERT TO authenticated
  WITH CHECK (user_id = (SELECT auth.uid()));
CREATE POLICY "rxt leave self or admin" ON public.rxt_group_members
  FOR DELETE TO authenticated
  USING (user_id = (SELECT auth.uid()) OR (SELECT public.rxt_is_admin()));

-- Group messages
DROP POLICY IF EXISTS "rxt group messages insert members" ON public.rxt_group_messages;
DROP POLICY IF EXISTS "rxt group messages read members" ON public.rxt_group_messages;
CREATE POLICY "rxt group messages read members" ON public.rxt_group_messages
  FOR SELECT TO authenticated
  USING ((SELECT public.rxt_is_admin()) OR (SELECT public.rxt_is_group_member(group_id)));
CREATE POLICY "rxt group messages insert members" ON public.rxt_group_messages
  FOR INSERT TO authenticated
  WITH CHECK (
    user_id = (SELECT auth.uid())
    AND (SELECT public.rxt_is_group_member(group_id))
  );

-- Support messages
DROP POLICY IF EXISTS "rxt support admin reply" ON public.rxt_support_messages;
DROP POLICY IF EXISTS "rxt support insert self" ON public.rxt_support_messages;
DROP POLICY IF EXISTS "rxt support read self or admin" ON public.rxt_support_messages;
CREATE POLICY "rxt support admin reply" ON public.rxt_support_messages
  FOR INSERT TO authenticated
  WITH CHECK ((SELECT public.rxt_is_admin()) AND sender_role = 'admin');
CREATE POLICY "rxt support insert self" ON public.rxt_support_messages
  FOR INSERT TO authenticated
  WITH CHECK (user_id = (SELECT auth.uid()) AND sender_role = 'student');
CREATE POLICY "rxt support read self or admin" ON public.rxt_support_messages
  FOR SELECT TO authenticated
  USING (user_id = (SELECT auth.uid()) OR (SELECT public.rxt_is_admin()));

-- Storage admin write policies; student read is limited to published chapter notes.
DROP POLICY IF EXISTS "rxt notes admin access" ON storage.objects;
CREATE POLICY "rxt notes admin insert" ON storage.objects
  FOR INSERT TO authenticated WITH CHECK (bucket_id = 'rxt-notes' AND (SELECT public.rxt_is_admin()));
CREATE POLICY "rxt notes admin update" ON storage.objects
  FOR UPDATE TO authenticated
  USING (bucket_id = 'rxt-notes' AND (SELECT public.rxt_is_admin()))
  WITH CHECK (bucket_id = 'rxt-notes' AND (SELECT public.rxt_is_admin()));
CREATE POLICY "rxt notes admin delete" ON storage.objects
  FOR DELETE TO authenticated
  USING (bucket_id = 'rxt-notes' AND (SELECT public.rxt_is_admin()));

CREATE INDEX IF NOT EXISTS rxt_notes_uploaded_by_idx ON public.rxt_notes(uploaded_by);
CREATE INDEX IF NOT EXISTS rxt_questions_source_note_idx ON public.rxt_questions(source_note_id);