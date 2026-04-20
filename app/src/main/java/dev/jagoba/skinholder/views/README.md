# views/

Capa de UI: Fragments, Activities y ViewModels organizados por feature.

- Cada subcarpeta agrupa un Fragment con su ViewModel correspondiente.
- `home/`, `dashboard/`, `notifications/`: pantallas principales de la bottom navigation.
- Los ViewModels extienden `BaseViewModel` y consumen clases de `logic/`.
