#!flask/bin/python
import subprocess
from thread import start_new_thread as SmartLenseThread
import time
from time import gmtime, strftime
import sys
import thread
from neopixel import *
from flask import Flask
from flask import request
import bluetooth
from flask import send_from_directory
import logging
import os

app = Flask(__name__)


# Funktion liefert das favicon (ist pflicht bei http)
@app.route('/favicon.ico')
def favicon():
    return send_from_directory(os.path.join(app.root_path),
                          'favicon.ico',mimetype='image/vnd.microsoft.icon')
# Funktion testet ob der Smart Lense Server aktiv ist.
# Beispielaufruf http://192.168.178.59:5000
# zu erwartendes Ergebnis: Nachricht: Smart Lenses Server is alive :-)
@app.route('/')
def index():
    return ("Smart Lenses Server is alive :-)")

# Funktion schaltet alle LEDs aus
# Beispielaufruf http://192.168.178.58:5000/off
# zu erwartendes Ergebnis: alle LEDs sind aus und Nachricht: Alle LEDs wurden ausgeschaltet
@app.route('/off')
def off():
    for j in range(63):
            strip.setPixelColor(j, 0)
    strip.show()
    return ("Alle LEDs wurden ausgeschaltet")
    

@app.route('/<command>')
def webAccess(command):
    command=command.upper()
    if (command.upper()=="IP"):
        return (getInternetAddress())
    if (command.upper()=="HCI"):
        return (getBluetoothAddress())
    if (command.find("@")>0):
        return (setNetwork(command))
    if (command.upper()=="BYE"):
        return (shutdown())
    if (command.upper()[:5]=="HOST="):
        return (setHostname(command[5:]))
    if (command.upper()[:5]=="HOST"):
        return (getHostname())
    if (len(command)==8):
        return setPixel(command)
    return ("Unknown Smart Lenses Command:[" + command + "] \n setPixel: PPRRGGBB\ngetIP: ip?\nsetNetwork: ssid@pks\nsetHostname:host=hostname\ngetHostName: host?\nShutdown Smart Lenses Server: bye")

# Funktion setzt die Farbe einer LED im Panel
# command=PPRRGGBB
#         PP=Nummer der LED
#         RR=Rotanteil als hex
#         GG=Gruenanteil als hex
#         BB=Blauanteil als hex
# Beispielaufruf: http://192.168.178.59:5000/33FF0000
# zu erwartendes Ergebnis: Nachricht: Farbe der LED: 33 wurde gesetzt auf: 0xFF0000 = RGB(255,0,0)
def setPixel(command):
    pixel=int(command[0:2])
    if ((pixel>0) and (pixel<65) and (len(command)==8)):
        red=int(command[2:4],16)
        green=int(command[4:6],16)  
        blue=int(command[6:8],16)
        color=Color(green, red, blue)
        strip.setPixelColor(pixel, color)
        strip.show()
        result="Farbe der LED: " + str(pixel) + " gesetzt auf: 0x" + command[2:8] + " = RGB(" + str(red) + ", " + str(green) + ", " + str(blue) + ")"
        logger.debug(result)
    else:
        result="Komando hat Fehler: " + command
    logger.debug(result)
    return (result)

def shutdown():
    py2output=subprocess.check_output("cd /; shutdown -h now", shell=True)
    return (py2output)

# Funktion liefert die Bluetooth Adresse
def getBluetoothAddress():
    py2output=subprocess.check_output("sudo hciconfig -a", shell=True)
    return (py2output[44:61])

# Funktion liefert aktuellen Hostname des Smart Lenses Server
def getHostname():
    hostname1=subprocess.check_output("sudo hostname", shell=True)
    hostname2=subprocess.check_output("sudo hciconfig hci0 name", shell=True)
    if (hostname2.find(hostname1)>0):
        return hostname1
    return "Hostnamen fuer IP und HCI sind nicht gleich! Bitte setzen sie einen Hostnamen!" 

def setHostname(hostname):
    if (len(hostname)>3):
        py2output=subprocess.check_output("sudo hostname " + hostname, shell=True)
        py2output=subprocess.check_output("sudo hciconfig hci0 name " + hostname, shell=True)
        return ("Hostname gesetzt auf: " + hostname)
    else:
        return ("Hostname nicht geeignet. Bitte waehlen sie einen anderen Namen!")

# Funktion liefert die Internet Adresse des Smart Lense Server
def getInternetAddress():
    py2output=str(subprocess.check_output("ifconfig wlan0", shell=True))
    return py2output[py2output.find("inet ")+5:py2output.find("  netmask")]

# Funktion  setzt ssid und psk auf dem Smart Lense Server. Die Infos kommen vom SmartPhone
def setNetwork(command):
    posParameter=command.find("@")
    ssid=command[:posParameter]
    psk=command[posParameter+1:]
    wpaConfigFile= open("/etc/wpa_supplicant/wpa_supplicant.conf","w+")
    wpaConfigFile.write("ctrl_interface=DIR=/var/run/wpa_supplicant GROUP=netdev\nupdate_config=1\ncountry=DE\nnetwork={" + "\nssid=\""
                        + ssid + "\"\npsk=\"" + psk +"\"\n}")
    wpaConfigFile.close()
    logger.debug("Network set to ssid: " + ssid)
    return ("Network set to ssid: " + ssid + " and psk: " + psk)
# 
# Funktion startet den Bluetooth server und wartet auf Nachrichten vom Smartphone     
def bluetoothServer():
    #try:
        while True:
            server_sock=bluetooth.BluetoothSocket( bluetooth.RFCOMM )
            server_sock.bind(("", 1))
            server_sock.listen(1)
            logger.info("Starte socket neu ...")
            command=""
            # das accept liefert 2 Werte 
            client_sock,address = server_sock.accept()
            logger.debug("Accepted connection from " + str(address))
            try:
                while True:
                  command=client_sock.recv(1024)
                  logger.debug ("Received Command: " + command)
                  posParameter=command.find("@")
                  if (command.find("@")>0):
                      setNetwork(command)
                      #logger.info("Set Network:" + ssid + ", with key: " + psk)
                  elif (command.upper() == "IP"):
                      logger.debug("Actual Internet Address transfered: " + getInternetAddress())
                      client_sock.send(str(getInternetAddress()+"             ")[:15])
                  elif (command.upper()== "SHUTDOWN"):
                      shutdown()
                  elif (len(command) == 8):
                      setPixel(command)
            except Exception, e:
                logger.error (str(e))
                client_sock.close()
                server_sock.close()
    #except:
    #    writeConsole ("Unexpected error:", sys.exc_info()[0])
        return ("Done")



if __name__ == '__main__':
    # create the logger .info .warning .error .crtical
    logger = logging.getLogger('SmartLenses')
    logger.setLevel(logging.DEBUG)
    format = logging.Formatter("%(asctime)s - %(name)s - %(levelname)s - %(message)s")
    # Ausgabe console
    ch = logging.StreamHandler(sys.stdout)
    ch.setFormatter(format)
    logger.addHandler(ch)
    # Ausgabe Datei
    fh = logging.FileHandler('/tmp/smartlenses.log')
    fh.setFormatter(format)
    fh.setLevel(logging.INFO)
    logger.addHandler(fh)
  

    bluetoothAddress=getBluetoothAddress()
    internetAddress=getInternetAddress()
    logger.info("Aktuelle Bluetooth Adresse: " + bluetoothAddress)
    logger.info("Aktuelle Internet Adresse: " + internetAddress)
    # erzeugt ein strip - ein Steuerungselement fuer 
    strip = Adafruit_NeoPixel(64, 18, 800000, 10, False, 255, 0)
    
    # Initialisiert den Strip
    strip.begin()
    
    #startet den Bluetooth Server als Thread
    if (getBluetoothAddress().count(":")==5):
        logger.info("Start Bluetooth server on " + bluetoothAddress)
        SmartLenseThread(bluetoothServer,())
    
    while True:
        if (getInternetAddress().count(".")==3):
            logger.info ("Start Web Server on: " + internetAddress)
            SmartLenseThread( app.run(host='0.0.0.0'),())
        else:
            time.sleep(10000)    
