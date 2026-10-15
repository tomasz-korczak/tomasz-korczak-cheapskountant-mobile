# Kontrakt API

`openapi.yaml` jest dokumentacją endpointów `cheapskountant-service` dla aplikacji klienckiej. Opisuje zastosowanie, parametry, typy, ograniczenia i odpowiedzi wszystkich zaimplementowanych endpointów.

Kontrakty w `specs/*/contracts/` pozostają dokumentami poszczególnych funkcji. Ten plik jest zestawieniem API, które obecnie obsługuje serwis.

## Endpointy

| Metoda | Ścieżka | Zastosowanie | Dostęp |
| --- | --- | --- | --- |
| `POST` | `/api/transcription` | odczyt paragonu z 1–5 zdjęć | `Bearer` |
| `POST` | `/api/receipt` | zapis paragonu i wydatków pozycji | `Bearer` |
| `POST` | `/api/expense` | zapis 1–500 wydatków | `Bearer` |
| `GET` | `/api/expense` | sumy kategorii i walut w zakresie dat | `Bearer` |
| `GET` | `/api/expense/daily` | wydatki własne i sumy paragonów z dnia | `Bearer` |
| `GET` | `/api/expenses` | 34 nazwy kategorii gospodarstwa | `Bearer` |
| `GET` | `/actuator/health` | stan procesu | publiczny |

Chronione żądania wysyłają `Authorization: Bearer <API_KEY>`.

## Podgląd

```shell
npx @redocly/cli preview-docs docs/api/openapi.yaml
```

## Generowanie klienta

Stabilne `operationId` nadają się do generatora. Przykład klienta TypeScript:

```shell
npx @openapitools/openapi-generator-cli generate -i docs/api/openapi.yaml -g typescript-fetch -o client/src/api
```

Inny generator wybiera się flagą `-g`, na przykład `kotlin`, `swift5` albo `dart`.
