# GPS_TRACKER — Torolalana amin'ny teny Malagasy

Ity tetikasa ity dia Android GPS tracker izay alefan'ny tompon'ny finday amin'ny alalan'ny bokotra Start, ary ajanony amin'ny Stop. Misy fampandrenesana maharitra rehefa mandeha ny fanarahana toerana.

## Fepetra
- Android 8.0 (API 26) na mihoatra.
- Internet rehefa mandefa toerana any amin'ny Supabase; raha tsy misy Internet dia tehirizina vetivety ao anaty tahiry eo an-toerana.
- Omeo alalana Location ny app. Avelao ny notification.
- Ampiasao amin'ny fitaovana anao na amin'ny fitaovana nahazoanao alalana mazava ihany.

## Fanamboarana
Ny GitHub Actions dia manamboatra debug APK rehefa misy push amin'ny main na rehefa alefa amin'ny tanana ny workflow.
Mankanesa amin'ny GitHub repository > Actions > Build GPS Tracker APK. Rehefa vita ny build dia alao ao amin'ny Artifacts ny GPS-TRACKER-APK.

## Dashboard
Sokafy ny dashboard.html amin'ny navigateur. Mila Internet ny sarintany OpenStreetMap/Leaflet. Ny dashboard dia tokony ampifandraisina amin'ny fomba fidirana Supabase voaaro alohan'ny hampiasana azy amin'ny angona tena izy.

## Fiarovana
Aza ampidirina ao amin'ny app na repository mihitsy ny Supabase service_role key. Ny publishable key ihany no ampiasaina miaraka amin'ny Row Level Security (RLS). Aza vonoina ny RLS.
