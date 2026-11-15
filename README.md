Odwóz-Dowóz — System Ewidencji Obecności uczniów w Transporcie Szkolnym
Odwóz-Dowóz to nowoczesna, w pełni lokalna (offline-first) aplikacja dla systemu Android, przeznaczona do szybkiej i intuicyjnej ewidencji obecności uczniów w pojazdach transportu szkolnego (gimbusach, przewozach PKS i autobusach szkolnych).
Aplikacja została zaprojektowana z myślą o opiekunach przewozów szkolnych, zapewniając szybką obsługę na urządzeniach mobilnych i tabletach w warunkach dynamicznej pracy przy pojazdach.

Kluczowe Funkcje
Ewidencja Obecności w Czasie Rzeczywistym:
Szybki, trzy-stanowy przycisk obecności w kolumnie akcji:
1. Szare tło, znak "?" — stan początkowy / niepotwierdzony.
2. Zielony przycisk "+" — uczeń obecny.
3. Czerwony przycisk "-" — uczeń nieobecny.

Ukrywanie danych wrażliwych podczas codziennej ewidencji — widoczne są tylko imiona uczniów, klasy oraz nazwy miejscowości (brak nazwisk ze względu na ochronę prywatności).

Moduł Wydruku i Raportowania (PDF):
1. Raporty Dzienne: Prezentacja statusów obecności (+ / -) dla wybranego kursu i dnia wraz z podsumowaniem liczby obecnych i nieobecnych uczniów oraz miejscem na podpis opiekuna.
2. Raporty Tygodniowe: Zestawienie zbiorcze frekwencji z nagłówkami zawierającymi konkretne dni miesiąca dla wybranego tygodnia.
3. Podgląd wydruku na żywo w aplikacji przed utworzeniem dokumentu.
Wykorzystanie natywnego interfejsu Android PDF / Print API umożliwiającego drukowanie przez Wi-Fi/Bluetooth oraz udostępnianie (Email, WhatsApp, Dysk Google).

Personalizacja i Ułatwienia Obsługi:
1. Tryb Jasny i Ciemny dostępny bezpośrednio z poziomu paska głównego okna aplikacji (ikony słońca i księżyca).
2. Edycja godziny planowanego odjazdu bezpośrednio z poziomu ekranu sprawdzania obecności.
3. Zarządzanie danymi opiekuna (Imię i Nazwisko widoczne na raportach oficjalnych).
4. Kopie Zapasowe i Przenośność Danych:
5. Pełna praca offline bez konieczności stałego połączenia z siecią Internet.
6. Import i eksport struktury danych uczniów, grup i tras w formacie JSON.

Trasy i Przewozy
Aplikacja obsługuje 5 domyślnych typów kursów przewozowych:
1. Dowóz PKS
2. Dowóz GIMBUS
3. Odwóz PKS I
4. Odwóz PKS II
5. Odwóz GIMBUS

Architektura i Technologie
- Aplikacja została napisana zgodnie z najnowszymi standardami programowania na platformę Android:
- Język: Kotlin
- UI: Jetpack Compose + Material 3 Design
- Baza Danych: Room Database (lokalny zapis relacyjny)
- Generowanie Dokumentów: android.graphics.pdf.PdfDocument / PrintManager
- Format Exchange: JSON (Import / Export)
- Architektura: Modularna (Clean Architecture / MVVM)

Struktura Modułów:
1. Calendar — obsługa zakresów dat i wybranych tygodni.
2. Groups — podział uczniów na trasy i kursy.
3. Student List — listy uczniów z filtrowaniem wg klas i miejscowości.
4. Guardian — konfiguracja danych opiekuna dyżurującego.
5. Import-Export — przenoszenie bazy danych.
6. Print — podgląd i generowanie raportów PDF.

Wymagania Techniczne
- System operacyjny: Android 8.0 (API level 26) lub nowszy
- Zalecane urządzenie: Smartfon lub tablet o przekątnej ekranu min. 6"

Instalacja i Uruchomienie
1. Sklonuj repozytorium:
2. git clone https://github.com/twoj-username/odwoz-dowoz.git
3. Otwórz projekt w Android Studio (Ladybug lub nowszym).
4. Zsynchronizuj pliki Gradle (Sync Project with Gradle Files).
5. Uruchom aplikację na emulatorze lub podłączonym urządzeniu fizycznym z systemem Android.

Licencja

Projekt dystrybuowany jest na licencji MIT. Szczegółowe informacje znajdują się w pliku LICENSE.
