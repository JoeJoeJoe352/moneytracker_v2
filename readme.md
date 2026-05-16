indítás: gyökérmappában kiadni a parancsot: 
docker compose -f docker-compose.dev.yml up *--build*

(--build akkor kell, ha a pom.xml, package.json, vagy a docker változott)

Ezután:
- backend → http://localhost:8080
- frontend → http://localhost:3000
- mysql → localhost:3306

