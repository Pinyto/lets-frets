import RPi.GPIO as io

class tinyled():
    """ Small Class to manage simple gpio leds """

    OUTPUT_PINS = []
    STATUS = []
    
    def __init__(self):
        self.OUTPUT_PINS = [17,27,24,23]
        self.STATUS = [0]*len(self.OUTPUT_PINS)
        self.ledsetup()
        
    def all_off(self):
        """ Turn all known leds off  """
        self.STATUS = [0]*len(self.OUTPUT_PINS)
        self.apply_status()
    
    def apply_status(self,swap=False):
        """ Applys the current state to all leds """
        for idx in range(len(self.OUTPUT_PINS)):
            if swap:
                io.output(self.OUTPUT_PINS[idx], not self.STATUS[idx])
            else:
                io.output(self.OUTPUT_PINS[idx],self.STATUS[idx])
            
    def change_led(self, idx=1):
        """ Swapping the status of one led  """
        io.output(self.OUTPUT_PINS[idx-1],not self.STATUS[idx-1])
        self.STATUS[idx-1] = not self.STATUS[idx-1]
        
    def ledsetup(self):
        """ Neccessary setup for GPIO, call once  """
        io.setmode(io.BCM)
        io.setwarnings(False)
        for pin in self.OUTPUT_PINS:
            io.setup(pin,io.OUT)
        
    def clean_up(self):
        """ Default cleanup method for GPIO """
        io.cleanup()
