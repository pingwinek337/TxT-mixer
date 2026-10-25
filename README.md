# Pack Mixer (Fabric 1.21.4)

Mod kliencki, który **łączy wiele texturepacków w jeden**. Wrzucasz packi do folderu `resourcepacks`, mod pokazuje, co jest w środku każdego z nich, a Ty wybierasz osobno dla każdego bloku / przedmiotu / moba / elementu GUI, z którego packa ma pochodzić tekstura (np. obsydian z packa A, reszta z packa B).

**Wymaga licencji** - bez klucza nie otwiera się GUI i mod nic nie robi z packami.

## Budowanie

Wymagania: Java 21.

```
./gradlew build          (Windows: gradlew.bat build)
```

Gotowy plik: `build/libs/packmixer-1.0.0.jar` (nie bierz `-sources.jar`). Wrzuć go do `.minecraft/mods` razem z **Fabric Loader 0.16.10+** i **Fabric API** dla 1.21.4.

Wersje (Fabric API, yarn, loader) są w `gradle.properties`. Jeśli któraś nie istnieje, wybierz aktualne dla 1.21.4 na https://fabricmc.net/develop

## Użycie

1. Wrzuć texturepacki (.zip lub foldery) do `.minecraft/resourcepacks`.
2. W grze otwórz mod: klawisz **K** (do zmiany w sterowaniu), albo przycisk **Pack Mixer** w lewym górnym rogu menu głównego i ekranu packów.
3. Za pierwszym razem pojawi się ekran **aktywacji licencji** (patrz niżej).
4. Zakładki w GUI:
   - **Packi** - lista packów z ikoną, opisem i liczbą tekstur (bloki / przedmioty / moby / GUI). Przyciski: *Użyj wszystkiego* (cały pack jako źródło) i *Wyczyść z miksu*.
   - **Bloki, Przedmioty, Moby, GUI** - po lewej lista tekstur (z wyszukiwarką), po prawej karty z podglądem tej tekstury z **każdego packa, który ją ma**. Kliknij kartę, żeby wybrać źródło. Karta *Auto* = normalna kolejność packów. Na dole panel hurtowy: ustaw wszystkie wyświetlane tekstury z wybranego packa.
5. **Zastosuj i przeładuj** zapisuje miks (`config/packmixer/mix.json`) i przeładowuje zasoby.

Jak to działa: mod dokłada wirtualny pack o najwyższym priorytecie, który dla wybranych tekstur oddaje plik z wybranego packa (razem z animacjami `.mcmeta`), a resztę zostawia normalnej kolejności packów.

## Licencje

Licencja to klucz `PMX1-...` podpisany kluczem prywatnym (Ed25519) i **przypisany do jednego urządzenia** (ID urządzenia). Działa tylko na komputerze, dla którego został wystawiony, więc jest jednorazowa: skopiowanie klucza na inny komputer nie zadziała.

### Przepływ sprzedaży

1. Kupujący uruchamia grę z modem, widzi ekran licencji i swoje **ID urządzenia** (przycisk *Kopiuj*), po czym wysyła je do Ciebie.
2. Ty generujesz klucz (z głównego folderu projektu):

   ```
   java tools/LicenseGenerator.java issue XXXX-XXXX-XXXX-XXXX --owner Jan --days 365
   ```

   `--days` jest opcjonalne (bez niego licencja bezterminowa). Każdy wystawiony klucz ląduje w `tools/issued.csv` (serial, urządzenie, właściciel, data).
3. Kupujący wkleja klucz na ekranie licencji i klika **Aktywuj**. Klucz zapisuje się w `config/packmixer/license.key`.

### Pierwsze uruchomienie - ZRÓB TO PRZED SPRZEDAŻĄ

W projekcie jest przykładowa para kluczy. Wygeneruj własną, żeby tylko Ty miał klucz prywatny:

```
java tools/LicenseGenerator.java keygen --force
```

Potem **przebuduj mod** (klucz publiczny wchodzi do jara). Klucz prywatny `tools/keys/private.key` i folder `tools/` trzymaj tylko u siebie, nie dołączaj ich do moda. Nowe klucze unieważniają wszystkie wcześniej wystawione licencje.

Test: `java tools/LicenseGenerator.java verify <klucz> <ID>`

### Ograniczenia (uczciwie)

- Licencja offline jest jednorazowa przez przypisanie do urządzenia. Prawdziwe "klucz użyty raz i spalony", unieważnianie kluczy czy limit urządzeń wymagają serwera aktywacji.
- Moda w Javie da się zdekompilować i usunąć sprawdzanie licencji. To zabezpieczenie przed zwykłym kopiowaniem, nie przed zdeterminowanym crackerem. Pomaga obfuskacja (np. ProGuard).
- ID urządzenia liczy się z nazwy systemu, użytkownika, katalogu domowego i nazwy komputera. Zmiana któregoś z nich wymaga nowej licencji.

## Struktura

```
src/main/java/pl/packmixer/
  PackMixerClient.java        wejście, klawisz K, przyciski w menu
  license/                    LicenseCodec (weryfikacja), LicenseManager
  config/MixConfig.java       zapis miksu
  pack/PackIndex.java         skan packów w tle
  pack/MixerResourcePack.java wirtualny pack z wybranymi teksturami
  mixin/ResourcePackManagerMixin.java  dokłada pack-mikser
  gui/                        LicenseScreen, PackMixerScreen, Ui, TexCache
tools/LicenseGenerator.java   generator licencji (tylko dla autora)
```
