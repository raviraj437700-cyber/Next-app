DROP POLICY IF EXISTS "rxt notes read admin" ON public.rxt_notes;
CREATE POLICY "rxt notes read published chapters"
ON public.rxt_notes
FOR SELECT
TO authenticated
USING (
  rxt_is_admin()
  OR EXISTS (
    SELECT 1
    FROM public.rxt_chapters c
    JOIN public.rxt_subjects s ON s.id = c.subject_id
    WHERE c.id = rxt_notes.chapter_id
      AND c.is_published = true
      AND s.is_published = true
  )
);

DROP POLICY IF EXISTS "rxt notes read published files" ON storage.objects;
CREATE POLICY "rxt notes read published files"
ON storage.objects
FOR SELECT
TO authenticated
USING (
  bucket_id = 'rxt-notes'
  AND EXISTS (
    SELECT 1
    FROM public.rxt_notes n
    JOIN public.rxt_chapters c ON c.id = n.chapter_id
    JOIN public.rxt_subjects s ON s.id = c.subject_id
    WHERE n.file_path = storage.objects.name
      AND c.is_published = true
      AND s.is_published = true
  )
);