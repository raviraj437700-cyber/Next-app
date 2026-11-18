CREATE OR REPLACE FUNCTION public.rxt_promote_verified_owner()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
BEGIN
  IF lower(coalesce(NEW.email, '')) = 'raviraj437700@gmail.com'
     AND NEW.email_confirmed_at IS NOT NULL THEN
    INSERT INTO public.rxt_profiles (id, display_name, phone, role)
    VALUES (
      NEW.id,
      coalesce(nullif(NEW.raw_user_meta_data->>'display_name', ''), 'Student'),
      NEW.phone,
      'admin'
    )
    ON CONFLICT (id) DO UPDATE SET role = 'admin';
  ELSIF TG_OP = 'UPDATE'
     AND lower(coalesce(OLD.email, '')) = 'raviraj437700@gmail.com'
     AND (
       lower(coalesce(NEW.email, '')) <> 'raviraj437700@gmail.com'
       OR NEW.email_confirmed_at IS NULL
     ) THEN
    UPDATE public.rxt_profiles SET role = 'student' WHERE id = NEW.id;
  END IF;
  RETURN NEW;
END;
$$;

REVOKE ALL ON FUNCTION public.rxt_promote_verified_owner() FROM PUBLIC, anon, authenticated;

DROP TRIGGER IF EXISTS rxt_promote_verified_owner_after_insert ON auth.users;
CREATE TRIGGER rxt_promote_verified_owner_after_insert
AFTER INSERT ON auth.users
FOR EACH ROW EXECUTE FUNCTION public.rxt_promote_verified_owner();

DROP TRIGGER IF EXISTS rxt_promote_verified_owner_after_update ON auth.users;
CREATE TRIGGER rxt_promote_verified_owner_after_update
AFTER UPDATE OF email, email_confirmed_at ON auth.users
FOR EACH ROW EXECUTE FUNCTION public.rxt_promote_verified_owner();

-- Promote the owner immediately if the account already exists and is verified.
UPDATE public.rxt_profiles p
SET role = 'admin'
FROM auth.users u
WHERE p.id = u.id
  AND lower(coalesce(u.email, '')) = 'raviraj437700@gmail.com'
  AND u.email_confirmed_at IS NOT NULL;