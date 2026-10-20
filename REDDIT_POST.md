# Reddit post draft — r/ (Taigun / VW India forum)

_Paste and edit. First line is the title._

---

**[OC] I built OpenVW — a free, open-source alternative to the My Volkswagen Connect Plus app (no ads, no tracking)**

The official Connect Plus app does the job but it's heavy and ad-y, so I reverse-engineered its API and built a clean native Android client for our cars. It talks to the **same** Volkswagen/Cerence servers — your data only ever goes to you and VW, same as the official app. It's open source (MIT) so you can read exactly what it does.

**What it shows**
- Live cluster: speed, RPM, fuel, battery, coolant, engine load, odometer — updates in real time while driving (push, not polling)
- Trips with place names, driver score, and your **real measured mileage** (km/l from actual fuel use)
- Engine on/off + alarm events (sudden brake, sharp turn…)
- Locate car on a map + Open in Google Maps; nearest workshops with directions
- Fuel-spend estimate with **your city's petrol price** (not hardcoded to Bengaluru)
- Car colour auto-detected (or pick it): white/black/grey/red/blue/yellow
- 6 home-screen widgets, dark mode, foldable/tablet layout, privacy mode
- Permissions: just Internet. No ads, no analytics.

**Works with:** any VW India car on **Connect Plus** with the OBD dongle — Taigun, Virtus, Tiguan, etc. Log in with your registered mobile + OTP. (Car image is Taigun-only for now; data is yours regardless.)

**Not** affiliated with Volkswagen. Unofficial, use at your own risk. APK + source: <LINK>

Happy to add artwork for other models and fix anything that looks off on your car — drop a comment.
