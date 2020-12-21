# I2C Install

Für das erste Einlesen wurde folgende Anleitung verwendet:

Siehe: [RPI-I2C-Python](http://www.netzmafia.de/skripten/hardware/RasPi/RasPi_I2C.html/ "Ausführliche Anleitung mit Beispielen in Scratch und Python")

## Vorbereitung

Ausgeführt wurden:

    sudo raspi-config
    -> Interfacing -> I2C -> Yes

Danach funktionierten die **SMBus**-Befehle ohne Probleme. Zum verwenden nötigte Pakete so installieren:

    apt-get update
    apt-get install i2c-tools      # I2C-Toolkit fuer die Kommandozeile
    apt-get install python-smbus   # Python-Bibliothek fuer I2C

> ... Welche auf der Raspian-Full Version schon instaliert waren

Die oben genannte Anleitung war für die Verwendung der **SMBus**-Befehle sehr hilfreich. Eine etwas **kompaktere Ansicht** findet man [hier](http://wiki.erazor-zone.de/wiki:linux:python:smbus:doc).

Für Python und I2C gab es nichts weiter, was man einstellen musste.

** Nun ist I2C bereit **
