# logic/

Lógica de negocio y casos de uso.

- Cada clase orquesta uno o más repositorios/APIs para resolver una operación de negocio.
- `UserLogic`: operaciones relacionadas con el usuario.
- `SteamPriceLogic`: consultas de precios del mercado de Steam.
- Los ViewModels consumen esta capa, nunca acceden directamente a `dataservice/`.
