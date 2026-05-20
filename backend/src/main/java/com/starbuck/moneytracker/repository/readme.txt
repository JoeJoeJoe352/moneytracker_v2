Ide kerül:
- Spring Data JPA repository-k. Adatbázis query-k

Pl:
- UserRepository.java
- ProductRepository.java

Mit tartalmazzon?
- extends JpaRepository<UserEntity, Long>
- opcionális query metódusok (findByEmail, findAllByStatus)

Mit ne tartalmazzon?
- üzleti logika
- DTO kezelés