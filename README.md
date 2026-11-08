# Pesadilla

Mod de terror para **Minecraft 1.20.1 (Forge 47.x)** de Sarex Studio: un enemigo que solo se mueve cuando no lo miras, jumpscares y sustos aleatorios.

## Compilar
Requiere Java 17.

    gradle wrapper --gradle-version 8.1.1   # solo la primera vez (o copia el wrapper de tu MDK)
    ./gradlew build                         # el .jar queda en build/libs
    ./gradlew runClient                     # para probarlo

Cliente y servidor necesitan el mod.

## Que hace
- **El Acechador**: figura alta y negra con ojos y sonrisa que brillan.
  - *Vigilante*: aparece lejos y te mira. Si le sostienes la mirada ~2,5 s desaparece. Si lo ignoras o te acercas, se vuelve cazador.
  - *Cazador*: corre hacia ti, pero se congela mientras lo miras. Si te alcanza de espaldas: jumpscare + dano. Si lo golpeas, se esfuma.
- **Director de sustos** (cada jugador, a intervalos aleatorios segun intensidad): pasos que se acercan a tu espalda, sonidos raros detras, puertas, antorchas que se apagan, apagon de pantalla con latidos, flash subliminal, falso "Herobrine joined the game" y jumpscare directo.
- Mas probabilidad de cazador en cuevas y a oscuras.

## Comandos (op)
- `/terror on|off`, `/terror intensidad 1-5`, `/terror letal true|false`
- `/terror susto` (uno al azar), `/terror jumpscare [jugador]`, `/terror acechador`, `/terror vigilante`

Los ajustes de los comandos se reinician al reiniciar el servidor (por defecto: activado, intensidad 2, no letal).

## Personalizar
- Cara del jumpscare: `assets/pesadilla/textures/gui/jumpscare.png` (256x256).
- Skin del enemigo: `textures/entity/stalker.png` (64x64, formato zombie) y `stalker_eyes.png` (zonas que brillan).
