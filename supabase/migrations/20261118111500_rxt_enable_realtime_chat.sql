DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_publication_tables
    WHERE pubname = 'supabase_realtime' AND schemaname = 'public' AND tablename = 'rxt_group_messages'
  ) THEN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.rxt_group_messages;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM pg_publication_tables
    WHERE pubname = 'supabase_realtime' AND schemaname = 'public' AND tablename = 'rxt_support_messages'
  ) THEN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.rxt_support_messages;
  END IF;
END $$;