# Guía de pruebas SRooms

## Requisitos
- Paper 1.20.1 (Java 17), online server, opción pvp=true.
- Dos cuentas de prueba y una cuenta con permisos srooms.admin.

## Comprobaciones
1. Crear zonas A y B que no se solapen con /createroom (hacha dorada).
2. Delimitar barrera entre ellas con el hacha diamante.
3. Guardar con /nameroom demo y verificar plugins/SRooms/rooms.yml.
4. Abrir /roomconfig demo, activar efectos y probar ambos spawns.
5. /room join demo con dos jugadores: cristal cerrado, cuenta regresiva y apertura.
6. Comprobar daño antes/después de la pelea, victoria por muerte, abandono y desconexión.
7. Probar keep inventory activado y desactivado con inventarios de prueba.
8. Reiniciar y confirmar que las rooms persisten y el cristal vuelve a su estado original.
9. Borrar con /deleteroom demo; comprobar que no permite borrar mientras está ocupada.

## Compilación
mvn clean package

El flujo GitHub Actions compila y publica el JAR como artifact en cada push a main y pull request.
