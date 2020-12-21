# Python Programmierung

> Der Python Code hält sich größtenteils an den PEP8 Style Guide.

In main.py wird ein logger erschaffen, der für alle Module *children* spawn, um die Logs zu schreiben. In den Modulen wird die Methode *ilog* verwendet um in diesen Logger zu schreiben, falls er bei der Initialisierung übergeben wurde.

## Autostart on RPI (bei Boot)

Falls kein Terminal benötigt wird, kann

    sudo crontab -e

verwendet werden. Einfach mit dem Bezeichner `@Reboot` einen Befehl erstellen, der bei jedem Neustart ausgeführt wird. Zum Beispiel (mit Log-Speicherung):

    @reboot sh /home/pi/autoLaunchPython.sh >/home/pi/autoPythonLogs/cronlog 2>&1

Falls ein Terminal benötigt wird sind die Lösung sehr *hacky*:

    sudo nano /etc/xdg/lxsesssion/LXDE-pi/autostart

Hinzufügen von dieser Zeile:

    @lxterminal -e sudo /home/pi/autostart.sh

sodass es so aussieht:

    @lxpanel --profile LXDE-pi
    @pcmanfm --desktop --profile LXDE-pi
    @xscreensaver -no-splash
    @lxterminal -e sudo /home/pi/autostart.sh
    point-rpi
