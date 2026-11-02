-- GPS_TRACKER schema baseline
-- Safe to apply when the table already exists: CREATE TABLE IF NOT EXISTS
-- leaves existing data and table definition untouched.

create table if not exists public.gps_locations (
  id bigint generated always as identity primary key,
  user_id uuid not null references auth.users(id),
  device_id text not null,
  latitude double precision not null check (latitude between -90 and 90),
  longitude double precision not null check (longitude between -180 and 180),
  accuracy_m double precision check (accuracy_m is null or accuracy_m >= 0),
  altitude_m double precision,
  speed_mps double precision check (speed_mps is null or speed_mps >= 0),
  bearing_deg double precision check (bearing_deg is null or (bearing_deg >= 0 and bearing_deg < 360)),
  captured_at timestamptz not null,
  received_at timestamptz not null default now()
);

create index if not exists gps_locations_user_captured_idx
  on public.gps_locations (user_id, captured_at desc);

create index if not exists gps_locations_device_captured_idx
  on public.gps_locations (device_id, captured_at desc);

alter table public.gps_locations enable row level security;

do $$
begin
  if not exists (
    select 1 from pg_policies
    where schemaname = 'public'
      and tablename = 'gps_locations'
      and policyname = 'Owners can read their GPS locations'
  ) then
    create policy "Owners can read their GPS locations"
      on public.gps_locations for select to authenticated
      using ((select auth.uid()) = user_id);
  end if;

  if not exists (
    select 1 from pg_policies
    where schemaname = 'public'
      and tablename = 'gps_locations'
      and policyname = 'Owners can insert their GPS locations'
  ) then
    create policy "Owners can insert their GPS locations"
      on public.gps_locations for insert to authenticated
      with check ((select auth.uid()) = user_id);
  end if;

  if not exists (
    select 1 from pg_policies
    where schemaname = 'public'
      and tablename = 'gps_locations'
      and policyname = 'Owners can delete their GPS locations'
  ) then
    create policy "Owners can delete their GPS locations"
      on public.gps_locations for delete to authenticated
      using ((select auth.uid()) = user_id);
  end if;
end
$$;

grant select, insert, delete on public.gps_locations to authenticated;

do $$
declare
  seq_name text;
begin
  seq_name := pg_get_serial_sequence('public.gps_locations', 'id');
  if seq_name is not null then
    execute format('grant usage, select on sequence %s to authenticated', seq_name);
  end if;
end
$$;
