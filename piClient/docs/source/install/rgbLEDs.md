# RGB-LED install

Die LEDs werden über das PWM Signal des RPi angesteuert, damit keine Problem bei der parallelen Verwendung des I2C-Bus entsteht.

Folgendes Git wurde verwendet:

* [ws281x-python Git](https://github.com/rpi-ws281x/rpi-ws281x-python)

## Verwendung unter Python

Für den Python Test wird folgendes Package benötigt:

    sudo pip install rpi_ws281x

> An dieser Stelle funktionierte das Beispiel strandtest.py ohne Probleme. Ohne die Ausführung einer anderen Installation.

Laut der folgenden [Anleitung](https://tutorials-raspberrypi.com/connect-control-raspberry-pi-ws2812-rgb-led-strips/) sollte man einen Chip auf dem RPi deaktivieren, es funktionierte auch ohne die Deaktivierung, aber es könnte die Lösung für das Problem mit dem HDMI-Kabel sein. Man sollte in `boot/config` diese Zeile auskommentieren:

    # Enable audio (loads snd_bcm2835)
    dtparam=audio=on

**Nun sind die RGB-LED bereit** Für den Anschluss wurde BCM pin 18 verwendet.
