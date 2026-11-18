DROP POLICY IF EXISTS "rxt support admin reply" ON public.rxt_support_messages;
DROP POLICY IF EXISTS "rxt support insert self" ON public.rxt_support_messages;
CREATE POLICY "rxt support insert authorized sender"
ON public.rxt_support_messages
FOR INSERT TO authenticated
WITH CHECK (
  (sender_role = 'admin' AND (SELECT public.rxt_is_admin()))
  OR
  (sender_role = 'student' AND user_id = (SELECT auth.uid()))
);