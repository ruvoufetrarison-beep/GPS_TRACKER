# GPS_TRACKER

Android GPS tracker with user-controlled start/stop controls, a visible foreground-service notification, 5-minute location updates, Supabase storage, an offline queue, and a Leaflet/OpenStreetMap dashboard.

## Repository contents

- `app/`: Android application source (Kotlin)
- `.github/workflows/build-apk.yml`: GitHub Actions workflow to build a debug APK
- `dashboard.html`: standalone web map dashboard
- `README_MG.md`: setup and usage guide in Malagasy

## Build status

The source and workflow are being uploaded. The APK must be built by GitHub Actions and checked before it can be considered ready. This repository does not yet contain a verified APK.

## Privacy

Use only on a device you own or have permission to track. Tracking is user-initiated, visible, and can be stopped in the app. Keep Supabase Row Level Security enabled; never put a service-role key in the Android app.
