screenshots:
![Alt text](https://github.com/user-attachments/assets/ac4d500f-41b1-4862-a15b-5cdb4385b7e8 "dashboard")
![Alt text](https://github.com/user-attachments/assets/869e74b2-6212-417d-8433-8f7efb9b30de "wallet")
![Alt text](https://github.com/user-attachments/assets/75273817-0df0-49ff-8135-8dc9941ce29d "form")
![Alt text](https://github.com/user-attachments/assets/8eae0859-f79f-4ea2-837e-ff5febfcf9b5 "charts")
![Alt text](https://github.com/user-attachments/assets/8a78f9f6-17cf-4133-a068-e893f8487566 "mobile")



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
