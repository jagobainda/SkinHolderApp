# dataservice/

Capa de acceso a datos: repositorios y clientes API.

- `repository/`: interfaces y sus implementaciones para acceso a datos (local o remoto).
- `api/`: clientes HTTP para comunicación con APIs externas (Steam, SkinHolderAPI, etc.).
- Esta capa abstrae el origen de los datos del resto de la aplicación.
