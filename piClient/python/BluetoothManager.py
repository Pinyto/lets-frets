import socket
import signal
import time
import traceback
import pexpect
import re
import json

class BlueManager():
    HOSTMACADR = 'B8:27:EB:60:00:70' # hciconfig
    PORT = 0 # port 1 belegt?
    BACKLOG = 1
    SIZE = 1024
    WAITING_TIME = 50
    logger = None
    has_logger = False
    ENCODING = "iso-8859-1"

    def __init__(self, logger=None):
        self.logger = logger
        if self.logger is not None:
            self.has_logger = True

    # Socket

    def build_socket(self):
        server_socket = socket.socket(socket.AF_BLUETOOTH,\
                                      socket.SOCK_STREAM,\
                                      socket.BTPROTO_RFCOMM)
        server_socket.bind((self.HOSTMACADR,self.PORT))
        server_socket.listen(self.BACKLOG)
        self.ilog("Socket bound and listening", 10)
        return server_socket

    def build_connection(self, server_socket):
        """ Waits for a client

        Todo:
            Rebuild the alarm, its a sketchy way of interrupting
            a method.

        """
        signal.signal(signal.SIGALRM, self.handler)
        signal.alarm(self.WAITING_TIME)
        self.ilog("Waiting for connection", 20)
        client, address = server_socket.accept()
        self.ilog("Accepted connection from {}".format(str(address)), 20)
        signal.alarm(0)
        return (client, address)

    # Acitve commands

    def await_data(self, client):
        self.ilog("Waiting for data",20)
        data = client.recv(self.SIZE)
        self.ilog("Message received {}".format(str(data)),10)
        return data

    def send_data(self, client, data):
        client.send(data)

    def send_json(self, client, data):
        """ Shortcut to send json data via bluetooth to client """
        self.send_data(client, json.dumps(data).encode(self.ENCODING))

    def clean_up(self, client, server_socket):
        self.ilog("Client and Socket closing", 20)
        if client is not None: client.close()
        if server_socket is not None: server_socket.close()
        self.ilog("Client and Socket closed", 10)

    # Tools

    def ilog(self, msg, level):
        if self.has_logger:
            self.logger.log(level, msg)
        else:
            #print(msg)
            pass

    # Hacks

    def handler(self, signalnumber, frame):
        raise UserWarning

    # Bluetooth Control

    def reset_devices(self):
        """ Uses bluetoothctl to remove all paired devices """
        PAT_PWR_DOWN = ".*Controller "+self.HOSTMACADR+" Powered: no"
        PAT_DISC_OFF = ".*Controller "+self.HOSTMACADR+" Discovering: no"
        PAT_CLASS_ID = ".*Controller "+self.HOSTMACADR+" Class:"
        PAT_POWER_ON = ".*Controller "+self.HOSTMACADR+" Powered: yes"
        PAT_C_DEVICE = ".*Device .*:.*:.*:.*:.*:.*"

        self.ilog(__name__+":Start",20)

        # Starting bluetoothctl with disabled echo (double messages) ######
        child = pexpect.spawn("bluetoothctl")
        child.setecho(False)
        #Debug line
        #child.logfile = sys.stdout.buffer

        # Agent is starting, power off and on again #######################
        child.expect(".*Agent registered")
        child.sendline("power off")
        child.expect(".*Changing power off succeeded")

        # Read in up to 3 messages
        count = 3
        while count:
            try:
                i = child.expect(
                        [PAT_PWR_DOWN, PAT_DISC_OFF, PAT_CLASS_ID,"\r\n"],
                        timeout=3)
                count = count - 1
                self.ilog(__name__+"iter {} | value {}".format(count,i), 10)
            except pexpect.TIMEOUT as e:
                self.ilog(__name__+"did not found all pwr off msgs", 10)
                self.ilog(traceback.format_exc(), 10)
                break
        time.sleep(3)

        #  Power On! ######################################################
        child.sendline("power on")
        child.expect(".*Changing power on succeeded")
        try:
            child.expect(PAT_POWER_ON, timeout=3)
        except pexpect.TIMEOUT as e:
            self.ilog(__name__+"Timeout waiting for ack",10)
            self.ilog(traceback.format_exc(), 10)
            pass
        else:
            self.ilog(__name__+"Controller power on succeeded",10)

        # Removing all devices! ###########################################
        child.sendline("paired-devices")
        devicesdone = False
        devicesfound = []
        while not devicesdone:
            try:
                child.expect(PAT_C_DEVICE, timeout=2)
            except pexpect.TIMEOUT as e:
                devicesdone = True
                self.ilog(__name__+"Done searching for devices", 10)
            else:
                devicesfound.append(child.after)
                self.ilog(__name__+"Device(s) found",10)
        parseddevices = []
        for line in devicesfound:
            parseddevices += re.findall(
                    "(?:[0-9a-fA-F]:?){12}",
                    line.decode("utf-8"))
        for address in parseddevices:
            self.ilog(__name__+"Removing {} ...".format(address), 10)
            child.sendline("remove "+address)
            child.expect(".*Device "+address+".*")
            child.expect(".*Device has been removed.*")
            self.ilog(__name__+"Device {} removed".format(address), 10)
        child.kill(0)
        time.sleep(2)
        self.ilog(__name__+"Device Reset Done", 20)

    def ready_bluetooth(self):
        """ Uses bluetoothctl to actiavate discoverable for 60 secs

        Return:
            int: 1 if failed, 0 if success

        """

        # Start Bluetoothctl with disabled echo (double messages) #########
        child = pexpect.spawn('bluetoothctl')
        child.setecho(False)
        #Debug line
        #child.logfile = sys.stdout.buffer
        try:
            child.expect('Agent registered')
            child.sendline('discoverable on')
            self.ilog(__name__+"Discoverable activated", 20)
        except pexpect.TIMEOUT as e:
            self.ilog(__name__+"Failed to activate discoverable", 40)
            self.ilog(traceback.format_exc(), 10)
            return 1
        child.kill(0)
        return 0
