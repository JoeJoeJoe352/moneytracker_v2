# Moneytracker

A Moneytracker egy személyes pénzügyi nyilvántartó webalkalmazás. Segít nyomon követni a bevételeket és a kiadásokat, így átláthatóvá válik, hogy mire megy el a pénz, és könnyebb tudatos döntéseket hozni: félretenni, tervezni, vagy épp megengedni magadnak valamit.

## Funkciók

- **Regisztráció és bejelentkezés** – minden felhasználó csak a saját adatait látja (JWT alapú hitelesítés).
- **Tárcák** – több tárca kezelése különböző pénznemekben (HUF, EUR, USD), két típussal:
  - _alapértelmezett_ – a mindennapi kiadásokhoz,
  - _megtakarítás_ – hosszú távú célokhoz (pl. lakásvásárlás, utazás).
- **Egyenleg szinkronizálása** – ha a tárca valós egyenlege eltér a rendszerben lévőtől (pl. hónap végi egyeztetéskor), a különbözetről automatikusan létrejön egy korrekciós tranzakció. Az alkalmazás jelzi, ha az utolsó szinkronizáció több mint 30 napja volt.
- **Tranzakciók** – bevételek és kiadások rögzítése, módosítása és törlése, kategóriákkal. Új kategória közvetlenül a tranzakció formból is felvehető.
- **Tételes tranzakciók** – egy tranzakció több tételre bontható (pl. egy bevásárlás termékei). A tétel ára megadható közvetlenül, vagy mennyiség × egységár formában.
- **Tranzakció előzmények** – szűrés név, dátum és tranzakció szám alapján. Egy tranzakció közvetlen linkkel is megnyitható (`/transactions/:id`).
- **Főoldal (dashboard)** – teljes egyenleg, havi bevételek és kiadások, legutóbbi tranzakciók.
- **Diagramok** – a tranzakciók grafikus megjelenítése.
- **Többnyelvűség** – magyar, angol és német felület.
- **Mobilbarát megjelenés**

## Képernyőképek

![Dashboard](https://github.com/user-attachments/assets/ac4d500f-41b1-4862-a15b-5cdb4385b7e8 "dashboard")
![Tárcák](https://github.com/user-attachments/assets/869e74b2-6212-417d-8433-8f7efb9b30de "wallet")
![Tranzakció form](https://github.com/user-attachments/assets/75273817-0df0-49ff-8135-8dc9941ce29d "form")
![Diagramok](https://github.com/user-attachments/assets/8eae0859-f79f-4ea2-837e-ff5febfcf9b5 "charts")
![Mobil nézet](https://github.com/user-attachments/assets/8a78f9f6-17cf-4133-a068-e893f8487566 "mobile")

## Technológiák

**Frontend** (`angular/`)

- Angular 22 (standalone komponensek, signalok, zoneless change detection)
- Angular Material
- ngx-translate (fordítások: `angular/public/i18n/`, alapnyelv: `hu.json`)
- Chart.js + ng2-charts
- Vitest (unit tesztek), ESLint, Prettier

**Backend** (`backend/`)

- Java 21, Spring Boot 4
- Spring Security + JWT
- Spring Data JPA, Flyway (adatbázis migrációk)
- MySQL 8
- springdoc-openapi (Swagger UI)
- Tesztek: JUnit, Testcontainers, H2
- Kódminőség: Checkstyle, SpotBugs, Spotless

**Infrastruktúra**

- Docker Compose a fejlesztői környezethez
- GitHub Actions CI a frontendhez és a backendhez (build, tesztek, sérülékenység-ellenőrzés)

## Projekt felépítése

```
moneytracker/
├── angular/                 # Angular frontend
├── backend/                 # Spring Boot backend
├── .github/workflows/       # CI pipeline-ok
└── docker-compose.dev.yml   # fejlesztői környezet (frontend, backend, MySQL)
```

## Fejlesztői környezet indítása

1. Docker Desktop elindítása
2. A gyökérmappában:

   ```
   docker compose -f docker-compose.dev.yml up --build
   ```

   A `--build` kapcsoló akkor kell, ha a `pom.xml`, a `package.json` vagy a Docker konfiguráció változott.

Ezután elérhető:

- frontend → http://localhost:3000
- backend → http://localhost:8080
- MySQL → localhost:3306

### Hasznos parancsok

- Backend újraindítása:

  ```
  docker compose -f docker-compose.dev.yml restart backend
  ```

- Belépés egy konténerbe (pl. `dev-backend`):

  ```
  docker exec -it dev-backend sh
  ```

- Leállítás a volume-ok törlésével:

  ```
  docker compose -f docker-compose.dev.yml down -v
  ```

  Figyelem: ilyenkor az adatbázis is törlődik.

## Tesztek

Frontend (az `angular/` mappában):

```
npx ng test                              # összes teszt
npx ng test --include="**/xyz.spec.ts"   # egy adott teszt fájl
```

Backend (a `backend/` mappában):

```
./mvnw test
```
