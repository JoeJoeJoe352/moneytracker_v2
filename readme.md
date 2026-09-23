screenshots:
![Alt text](/screenshots/screenshot1?raw=true "dasboard")
![Alt text](/screenshots/screenshot2?raw=true "form")
![Alt text](/screenshots/screenshot3?raw=true "wallet")
![Alt text](/screenshots/screenshot4?raw=true "mobile")

indítás: gyökérmappában kiadni a parancsot:

- docker desktop elindítása
- docker compose -f docker-compose.dev.yml up _--build_

(--build akkor kell, ha a pom.xml, package.json, vagy a docker változott)

Ezután:

- backend → http://localhost:8080
- frontend → http://localhost:3000
- mysql → localhost:3306

docker compose -f docker-compose.dev.yml restart dev-backend

konténerbe belépni: docker exec -it _név_(dev-backend) sh

Ha docker compose -f docker-compose.dev.yml down -v paranccsal a volume is törlődik. A következő induláskor ezért nem biztos, hogy létezik már a db, amikor a spring elindul, ami hibát okoz

tesztek futattása: npx ng test --include="\*\*/xyz.spec.ts"
