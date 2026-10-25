# Normsafio 2.1 — proyecto fuente

Proyecto nuevo para Paper 1.21.1 / Java 21. Incluye interacciones de máquina expendedora, pozo y kart, economía básica persistente y los modelos del pack enlazados mediante CustomModelData.

## Compilar
1. Instala Java 21 y Maven 3.9+.
2. En esta carpeta ejecuta `mvn package` con conexión a Internet para descargar Paper API.
3. El JAR generado estará en `target/Normsafio-2.1.jar`.
4. Instala el JAR en `plugins/` y reinicia el servidor.
5. Instala el resource pack incluido en `Normsafio-pack-2.1.zip` en los clientes/servidor.

## Comandos
- `/normsafio items` — entrega los objetos de prueba (requiere OP o `normsafio.admin`).
- `/deditas` — consulta saldo.
- `/deditas give <jugador> <cantidad>` — administrador.

## Avisos de alcance
Esta reconstrucción no contiene el código fuente del plugin original y, por tanto, no conserva automáticamente todos sus sistemas anteriores (gulags, portales, retos, runas y demás). La tienda y economía de esta reconstrucción son una base funcional sencilla; los objetos de la tienda son ejemplos y deben enlazarse con el registro completo de ítems del servidor antes de sustituir el plugin original en producción. No instalar junto al plugin antiguo con el mismo nombre.
