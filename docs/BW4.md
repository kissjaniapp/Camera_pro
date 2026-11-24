# HEIYOUCAM BW4 gyári firmware – Camera Pro

## Bizonyított megfigyelések
- Saját AP mód: 192.168.168.1, a 81/TCP elérhető, a gyári O-KAM Pro élőképe és LED vezérlése működött.
- Routeres mód: a felhasználói teszten 192.168.1.218, 81/TCP elérhető, a gyári P2P élőkép és távoli vezérlés meghiúsult.
- A 81/TCP **nem** szabványos HTTP/RTSP videószerver. A GET / kérés kapcsolatmegszakítással végződött.
- A feltöltött O-KAM Pro 3.0.22 APK arm64-v8a natív könyvtárakat tartalmaz: libOKSMARTPPCS.so, libOKSMARTPLAY.so. A PPCS kommunikáció gyári, zárt megvalósítása nem került átvételre.

## v0.3 felderítési próba
A VStarcam/Eye4 PPPP LAN_SEARCH nyilvánosan dokumentált, XOR-olt 4 bájtos UDP 32108 kérése:
`F5 3B 43 22` (dekódolva `F1 E0 00 00`).
A diagnosztika csak küldi a kérdést, figyeli a LAN_NOTIFY (0x91) választ és jelzi a válasz forrás-IP/portját. Nem küld hitelesítési adatot, nem kezdeményez videóstreamet, nem módosít kamerabeállítást.
A BW4 konkrét firmware-verzióján a LAN_SEARCH **nem volt tesztelve**; az egyéb VStarcam modelleken ismert módszer nem garantáltan kompatibilis.

## Következő munka
A LAN_NOTIFY eredmény után ellenőrizhető a helyi P2P handshake és a DRW-keretezés. A videófolyamig eljutáshoz hitelesítés és H.264 reassembly kell; a TCP-port nyitottsága nem elegendő. A jelenlegi alkalmazásban nem állítunk tényleges BW4 élőképet.

## Források
- https://github.com/hmchan/eye4-rtsp-proxy/blob/main/PROTOCOL.md
- https://github.com/neonil123/BW4-ONVIF-RTSP-CAMERAS/blob/main/docs/DISCOVERY.md

Ne tölts fel kameranaplót, Wi-Fi-jelszót, egyedi kamerakulcsot vagy a gyári APK-t a nyilvános repóba.
