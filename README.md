# Camera Pro 0.1 – kísérleti Android kliens
Magyar felület, szabványos RTSP/HTTP videó lejátszása, BW4 TCP-diagnosztika, MP4 import, Google Drive feltöltési sor és napi megőrzési ellenőrzés. A kamera gyári protokollja **még nincs implementálva**, automatikus kamerarögzítés és átkódolás sincs. A minőségválasztás jelenleg mentett preferencia. A lejárt, saját appProperties jelöléssel ellátott fájlok a kukába kerülnek, nem más Drive-fájlok.
## APK
Actions → Android APK → sikeres futás → Artifacts → Camera-Pro-debug.
## Drive beállítása
Google Cloud projektben engedélyezd a Drive API-t, konfiguráld az OAuth consent screent és hozz létre Android OAuth klienst a hu.job.camerapro csomagnévvel és a tényleges APK aláírás SHA-1 ujjlenyomatával. Tesztelési módban add hozzá a fiókot tesztelőként. A debug aláírás a CI futások között változhat, ezért stabil kulcs szükséges a tartós engedélyezéshez. A scope drive.file. Nincs beágyazott titok. Az importált felvétel megőrzése az import idejétől számít. A háttérmunka Android ütemezésétől és hálózattól függ, nem garantált pontos törlési idő. A Drive-ba kerülő fájlokat a felhasználó saját fiókjában tároljuk.
## Korlátok
Valódi BW4 élőképhez protokolladapter szükséges. Folyamatos mentéshez állandóan elérhető kameraforrás és rögzítő eszköz kell. Ezt a kiadást nem szabad biztonsági felügyeletre hagyatkozva használni.