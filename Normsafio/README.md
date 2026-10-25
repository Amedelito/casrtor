# Normsafio 2.1

Proyecto Maven para compilar el plugin con GitHub Actions.

## Estructura
- `pom.xml`: configuración Maven
- `src/main/java/es/normsafio/NormsafioPlugin.java`: código Java
- `src/main/resources/plugin.yml`: configuración del plugin
- `.github/workflows/build.yml`: compilación automática en GitHub Actions

## GitHub
Extrae el ZIP. En el repositorio de GitHub, sube el CONTENIDO de esta carpeta `Normsafio` a la raíz del repositorio. La raíz debe mostrar `pom.xml`, `src/`, `.github/` y `README.md` juntos.

Al hacer commit, Actions ejecutará Maven. Si la compilación termina bien, el archivo JAR aparecerá en Artifacts de esa ejecución.
