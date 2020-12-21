from rpi_ws281x import PixelStrip, Color
import json
import time
import os

#TODO: NEW MATRIX FORMATION [FRET[STRING]
#TODO: NEW Color-Config CONFIG[FingerColor][id]

class LedManager:
    """ Class that controlles the led matrix

    Also parses incomming data
    Uses the config file

    """
    matrix = []
    top_row = []
    # Coloumns and Frets #####################################################
    LED_STRIP = None
    STRING_CNT = 6
    FRET_CNT = 4
    CONFIG = {}
    logger = None
    has_logger = False
    FILE_CONFIG = "ledconfig.json"
    BROKEN_PCB = True

    def __init__(self, logger=None):
        self.logger = logger
        if self.logger is not None:
            self.has_logger = True
        self.reloadconfig()
        self.preparematrix()
        self.preparetop()
        self.dosetup()

    # LED CONTROL

    def colorwipe(self,color=Color(0,0,0)):
        """ Sets one color for the hole strip """
        wait_time = self.CONFIG["WAIT_MS"]
        for i in range(self.LED_STRIP.numPixels()):
            if(self.BROKEN_PCB and i==24):
                continue
            self.LED_STRIP.setPixelColor(i, color)
            self.LED_STRIP.show()
            time.sleep( wait_time / 1000)

    def colorwipeTop(self,color=Color(0,0,0)):
        """ If top installed, clear it """
        wait_time = self.CONFIG["WAIT_MS"]
        if(self.CONFIG["TOP_INSTALLED"]):
            for i in range(6):
                self.LED_STRIP.setPixelColor(i,color)
                self.LED_STRIP.show()
                time.sleep( wait_time / 1000 )

    def applydata(self):
        self.applymatrix()
        self.applytop()

    def applymatrix(self):
        """ State matrix applied on the strip
        """
        cur_mat = self.matrix
        offset = 0
        ## DEBUG:
        for line in cur_mat:
            print(str(list(line)))
        if(self.CONFIG["TOP_INSTALLED"]):
            offset = 6
        for y in range(len(cur_mat)):
            for x in range(len(cur_mat[y])):
                tmp_index = offset + (y*6+x)
                if(tmp_index>=24 and self.BROKEN_PCB):
                    tmp_index += 1
                self.LED_STRIP.setPixelColor(tmp_index,cur_mat[y][x])
                self.LED_STRIP.show()
                time.sleep( self.CONFIG["WAIT_MS"] / 1000)

    def applytop(self):
        """ TopRow-State applied to the strip """
        wait_time = self.CONFIG["WAIT_MS"]
        for x in range(len(self.top_row)):
            self.LED_STRIP.setPixelColor(x, self.top_row[x])
            self.LED_STRIP.show()
            time.sleep( wait_time / 1000)

    def dosetup(self):
        """ Init. of a default rgb strip

        Note:
            The config file need to be loaded before calling this method

        """
        fix = 0
        if self.BROKEN_PCB:
            fix = 1
        LED_COUNT = ( (self.FRET_CNT+self.CONFIG["TOP_INSTALLED"])
                        * self.STRING_CNT + fix )
        LED_PIN = 18
        LED_FREQ = 800000
        LED_DMA = 10
        LED_BRIGHTNESS = self.CONFIG["BRIGHTNESS"]
        self.LED_STRIP = PixelStrip(\
                                    LED_COUNT,\
                                    LED_PIN,\
                                    LED_FREQ,\
                                    LED_DMA,\
                                    False,\
                                    LED_BRIGHTNESS)
        self.LED_STRIP.begin()

    def clean_up(self):
        """ Calls spooky cleanup on strip

        Maybe neccessary

        """
        self.LED_STRIP._cleanup()

    # DATA MANAGMENT

    def preparematrix(self):
        """ Init. dark matrix """
        ROW = [Color(0,0,0)]*self.STRING_CNT
        mat = [[]]*self.FRET_CNT
        for idx in range(len(mat)):
            # Copy the list to create the complete matrix
            mat[idx] = ROW.copy()
        self.matrix = mat
        self.ilog(__name__+" Matrix Prep Done",10)

    def preparetop(self):
        self.top_row = [Color(0,0,0)]*self.STRING_CNT

    def reloadconfig(self):
        """ Reloads LED Config File """
        self.ilog(__name__+ " Starting config reload...",10)
        curconfig = {}
        if(os.path.isfile(self.FILE_CONFIG)):
            with open(self.FILE_CONFIG,"r",encoding="utf-8") as f:
                curconfig = json.load(f)
            self.CONFIG = curconfig
            self.ilog(__name__ + " Config reload done!",20)
        else:
            self.ilog(__name__ + " No config file found. Creating default",10)
            default_colors = {"-2": Color(255,0,0),
                            "-1": Color(52,255,13),
                            "0": Color(255,255,255),
                            "1": Color(255, 0, 0),
                            "2": Color(0,255,0),
                            "3": Color(0,0,255),
                            "4": Color(255, 255, 0)}
            default_config = {"TOP_INSTALLED":False,
                      "WAIT_MS":10,
                      "BRIGHTNESS":20,
                      "finger_color":default_colors}
            with open(self.FILE_CONFIG,"w",encoding="utf-8") as f:
                json.dump(default_config,f,ensure_ascii=False,indent=4)
            self.ilog(__name__ + " Default config created!",20)
            self.reloadconfig()

    def store_config(self):
        """ Stores current config to file """
        self.ilog(__name__ + " Start saving config to file",10)
        with open("ledconfig.json","w",encoding="utf-8") as f:
            json.dump(self.CONFIG, f, ensure_ascii=False, indent=4)
        self.ilog(__name__ + " Currenct config successfully saved!",20)

    def set_strings_to_hit(self, strings):
        """ For each one in a binary six digit string the corresponding
        string will be activated

        """
        for charIDx in range(len(strings)):
            if(strings[charIDx]=="1"):
                self.top_row[charIDx] = self.CONFIG["finger_color"]["0"]
            else:
                self.top_row[charIDx] = Color(0,0,0)


    def set_single_in_matrix(self, pos, string, finger):
        """ Adds given data to internal matrix

        Note:
            Use applymatrix to activate the changes

        """
        cur_mat = self.matrix;
        tmpColor = self.get_finger_color(finger)
        cur_mat[pos][string] = tmpColor

    def set_array_in_matrix(self, data_array):
        """ Adding a collection through set_single_in_matrix

        Needs fret, string, finger per element

        Note:
            Use applymatrix to activate the changes

        """
        for values in data_array:
            self.set_single_in_matrix(values[0], values[1], values[2])

    # MODIFIER

    def setbrightness(self,brightness=20):
        """ Sets brightness value

        Also applies the brightness to the LED strip
        """
        self.CONFIG["BRIGHTNESS"] = brightness
        self.LED_STRIP.setBrightness(self.CONFIG["BRIGHTNESS"])
        self.ilog(__name__ + " Applied new brightness: {} (~{}%)".format(
            brightness, (brightness/255) * 100), 10)

    def setinstalled(self, intalled=True):
        """ (De)activates first row skip """
        self.CONFIG["TOP_INSTALLED"] = installed
        self.ilog(__name__ + "Top installed...", 10)

    def setwaittime(self, time=10):
        """ Sets wait time for the LEDs

        Note:
            Wait time between every led change
        """
        self.CONFIG["WAIT_MS"] = time
        self.ilog(__name__ + "{} ms wait time applied".format(time), 10)

    # Threadfunctions

    def thread_load_indicator(self, sharedVariable):
        """ Thread task, use utils.sharedVariable to indicate finish """
        if(self.CONFIG["TOP_INSTALLED"]):
            accentColor = Color(136,176,75) # 2017 Greenery
            mainColor = Color(255,0,0)
            secondColor = Color(140,0,0)
            thirdColor = Color(60,0,0)
            baseValue = sharedVariable.get_storage() + 1
            lowerBound = baseValue
            current_position = lowerBound
            upwards = True
            upperBound = 5

            while(not sharedVariable.get_task_done()):

                if(sharedVariable.has_changed()):
                    self.ilog(__name__ + " thread, new value!")
                    baseValue = sharedVariable.get_storage() + 1
                    lowerBound = baseValue
                    current_position = lowerBound

                for idx in range(self.STRING_CNT):
                    if(idx < baseValue):
                        self.top_row[idx] = accentColor
                    else:
                        self.top_row[idx] = Color(0,0,0)
                if(upwards):
                    delta = current_position - baseValue
                    if(delta>0):
                        self.top_row[current_position-1] = secondColor
                        if(delta>1):
                            self.top_row[current_position-2] = thirdColor
                else:
                    delta = upperBound - current_position
                    if(delta>0):
                        self.top_row[current_position+1] = secondColor
                        if(delta>1):
                            self.top_row[current_position+2] = thirdColor
                self.top_row[current_position] = mainColor
                self.applytop()
                if(upwards):
                    #Todo inc or dec counter
                    if(current_position==upperBound):
                        upwards = False
                    else:
                        current_position = current_position + 1
                else:
                    if(current_position==lowerBound):
                        upwards = True
                    else:
                        current_position = current_position - 1
            else:
                self.colorwipeTop()
    #END thread_load_indicator

    def thread_idle_indicator(self, sharedVariable):
        """ Thread task, use utils.sharedVariable to indicate finish """
        if(not self.CONFIG["TOP_INSTALLED"]):
            return
        mainColor = Color(0,140,0)

        position = 0

        while(not sharedVariable.get_task_done()):
            self.colorwipeTop()
            if(position>5):
                position=0
            self.top_row[position] = mainColor
            self.applytop()
            position = position + 1
            
        self.colorwipe()

    # Tools

    def ilog(self, msg, level=30):
        """ If logger given, logs on given level """
        if self.has_logger:
            self.logger.log(level, msg)
        else:
            #print(msg)
            pass

    def get_finger_color(self, finger):
        """ Returns the correct color for a finger index

        """
        return self.CONFIG["finger_color"][str(finger)]

    def cleanup(self):
        """ Reset the tempory internal state and clears LED Strip """
        self.preparematrix()
        self.preparetop()
        self.colorwipe()

# Other

if __name__ == "__main__":
    lm = LedManager()

    lm.CONFIG["WAIT_MS"] = 50
    lm.colorwipe(Color(255,0,0))
    lm.colorwipe(Color(255,255,0))
    lm.colorwipe(Color(0,255,0))
    lm.colorwipe(Color(0,255,255))
    lm.colorwipe(Color(0,0,255))
    lm.colorwipe()
    print("LedManager:Main :: Test Load Done!")
