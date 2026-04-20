# di/

Módulos de inyección de dependencias (Hilt/Dagger).

- `AppModule`: provee las dependencias principales de la app (repositorios, APIs).
- Cada módulo nuevo debe anotarse con `@Module` + `@InstallIn` indicando el componente de Hilt correspondiente.
