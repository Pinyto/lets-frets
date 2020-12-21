# Bluetooth Install

Für die Verwendung von Bleutooth muss beachtet werden, dass jede Art von Bluetooth GUI auf dem RPi evtl. nach einer Kopplungsbestätigung fragt, wodurch es nicht möglich ist eine dauerhafte Verbindung aufzubauen, da die GUI Abfrage zur Bestätigung nicht beantwortet werden kann (weil kein Bildschirm angeschlossen ist). Also müssen alle Bluetooth-Manager deaktiviert werden (zumindest von der Oberfläche entfernt werden)

Wichtig ist auch die richtige UUID zu verwenden. Am besten man verwendet die, die für RFCOMM reserviert ist. Das Installieren von eigenen UUIDs wurde nicht erfolgreich getestet (eine Methode um sicher zu gehen, dass sich nur die richtige App per Bluetooth mit der Smart Guitar verbinden). Die RFCOMM-UUID ist im Code eingetragen.

    sudo apt-get install bluetooth blueman bluez
    # bluetooth wurde installiert
    # blueman wurde nicht installiert
    # bluez wurde nicht installiert, weil bereits vorhanden

Welches dieser Pakete exakt benötigt wird ist unklar.

Das Bluetooth-Modul auf dem RPi wird am besten durch `bluetoothctl` angesteuert.


## Verwendung

    sudo bluetoothctl
    [bluetooth]# power on
    [bluetooth]# agent on
    [bluetooth]# discoverable on
    [bluetooth]# pairable on
    [bluetooth]# scan on

## Bug-Fix

Folgender Bug-Fix musste angewendet werden, um eine dauerhafte Verbindung mit dem Smartphone zu ermöglichen:

    You'll need to add the SP profile to the Pi. Edit this file:


    sudo nano /etc/systemd/system/dbus-org.bluez.service
    Add the compatibility flag, ' -C', at the end of the 'ExecStart=' line. Add a new line after that to add the SP profile. The two lines should look like this:


    ExecStart=/usr/lib/bluetooth/bluetoothd -C
    ExecStartPost=/usr/bin/sdptool add SP
    Save the file and reboot. Pair and trust your Pi and phone with bluetoothctl.

Die Ansteuerung des Bluetooth-Moduls aus Python heraus kann PyblueZ verwendet werden. Allerdings gab es hier Probleme, doch theoretisch ist dies eine bessere Methode, da mehr Funktionen und eine direkte Kommunikation mit dem Bluetooth-Modul möglich ist. Zum Ausprobieren:

Quellen: [Git1](https://github.com/pybluez/pybluez); [Git2](https://github.com/karulis/pybluez) und zusätzliche Header-Files, da diese manchmal bei der normalen Installation zu fehlen scheinen:

    sudo apt-get install libbluetooth-dev

Für diesen Prototypen wurde mittels `pexpect` `bluetoothctl` kontrolliert. Bitte ebenfalls installieren.


    pip3 install pexpect

Hilfreich um die MAC-Adresse des RPi herauszufinden: `hciconfig`

Nützliche App für die rohe Kommunikationstestung unter Android:

> BlueTerm +



** Nun ist das Bluetooth Modul bereit **
