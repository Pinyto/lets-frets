import utils
import smbus
import time
import traceback
import math
import os.path

class FDCManager:
    """ Manages access to the FDC chips over the TCA
    Todo:
        Implement balance mode: A solid config file that can be reloaded
        to set the measurement values to zero. The value of the chips
        should be middled and then substracted.

    Hint:
        Make sure to setup the hard-coded values correctly.
    """

    CUS_SCL = 1/100
    bus = smbus.SMBus(1)
    matrix = None
    CONNECTED_CHIPS = 6
    TCA_ADDRESS = 0x77
    ADDR = 0x50
    logger = None
    has_logger = False
    config_file = "chipconfig.txt"
    DEFECT_CONNECTIONS = ["32","33"]

    def __init__(self,logger=None, do_refine=False):
         # Time to let the bus settle?
        if logger is not None:
            self.logger = logger
            self.has_logger = True
        self.prepare_matrix()
        if(not os.path.isfile(self.config_file)):
            self.create_default_config()
        time.sleep(0.5)
        self.reset_tca()
        time.sleep(1)
        self.setup_chips()
        if do_refine:
            self.refine_setup()
        self.ilog(__name__+" FDC_Manager setup done", 20)


    def prepare_matrix(self):
        #         1    2    3    4    5    6
        m = [   ["10","11","12","13","00","01"], #fret1
                ["20","21","22","23","02","03"], #fret1
                ["40","41","42","43","30","31"], #fret2
                ["50","51","52","53","32","33"], #fret3
                ]
        self.matrix = m

    def map_to_matrix(self, results_dict):
        """ Maps results from dict to local .matrix """
        m = self.matrix
        failed_entrys =  []
        for y in range(len(m)):
            for x in range(len(m[y])):
                identifier = m[y][x]
                if identifier in results_dict:
                    self.matrix[y][x] = results_dict[identifier]
                else:
                    #Todo: Mabye init. an error value?
                    failed_entrys.append(identifier)
        if(len(failed_entrys)):
            self.ilog(__name__+ " IDs: {} \n ...not in results dict".format(
            failed_entrys), 30)

    def ilog(self, msg, level):
        if self.has_logger:
            self.logger.log(level,msg)
        else:
            #print(msg)
            pass

    def check_results(self, state, active="1111"):
        """ Checks if all results are ready
        Hint:
            The last bits of the config string on the chip (0x0C)
            must 1 if the corresponding measurements are active.

        """
        state = active == state[-4:]
        return state

    def read_result(self, num):
        """ Read a single result
        Reads from the currently connected FDC.

        Args:
            num (int):
                Must be >=0 and <=3. Reads the measurement with this id.
        Returns:
            int: result in twoscomplement

        """
        idx = num * 2
        mp1 = utils.auto_bin(self.bus.read_word_data(self.ADDR, 0x00+idx))
        mp2 = utils.auto_bin(self.bus.read_word_data(self.ADDR, 0x01+idx))
        m = mp1 + mp2[:8]
        mv = utils.twoscomplement(m)
        return utils.floor2(mv/2**19)

    def read_all_results(self):
        """ Reads all results from all chips

        Stores them in a dictionary, chip id + mes id as index

        Return:
            dict: Dictionary that maps (chip+mes) -> Result
        """
        results = {}
        for chipNum in range(self.CONNECTED_CHIPS):
            self.swap_tca(chipNum)
            time.sleep(0.0125*2)
            # Wait for 1 seconds, so that the i2c can settle
            # and the fdc's can measure the results
            # With 0.05 at M_RATE=11 -> 3 Mistakes (10k Measurements)
            # Update: with 3 chips the error rate keeps increasing
            # Update2: 1F Cap. between power in and power out (TCA)
            #           -> Rare mistakes
            #           -> Power Cables on solderd to pins
            #           -> Rate: 0.1 + 0.05 (+printing time)
            #           -> State of the tca gets read out, after every change
            #               (directly, no wait, next statement)
            #           -> 4 seconds wait during switches during setup mode
            #               +3 Seconds after done
            while 1:
                cur_state = utils.auto_bin(self.bus.read_word_data(self.ADDR, 0x0C))
                if self.check_results(cur_state):
                    break
                else:
                    self.ilog(__name__+
                            "Res. not rdy. Id:{}".format(chipNum), 30)
                    self.ilog(__name__+str(cur_state), 30)
                    print("results not rdy")
                    time.sleep(0.1)
            for x in range(4):
                if str(chipNum)+str(x) in self.DEFECT_CONNECTIONS:
                    results[str(chipNum)+str(x)] = 0
                else:
                    results[str(chipNum)+str(x)] = self.read_result(x)
        return results

    def single_setup(self, idx, config, conversion_needed=False):
        """ Single Setup upload to current chip

        Also applies a little -> big endian conversion if flag is set.

        Args:
            idx(int): id of the current measurement to set offset correctly
                        must be between 0 and 3 (including)
            config(str): setup string for chip:measurement
            conversion_needed(boolean): if big/little conversion is needed
        """
        if conversion_needed:
            self.bus.write_word_data(self.ADDR, 0x08+idx,
                int(utils.conv_str(config),2))
        else:
            self.bus.write_word_data(self.ADDR, 0x08+idx, int(config,2))

    def setup_chips(self):
        """ Basic Setup for all chips.

        Hint:
            You can configure M_RATE to setup the refresh rate.
            Change REPEAT to switch between repeating measurements
            and normal mode. If you measure in normal mode you
            need to rewrite the config after each readout.
        """
        self.ilog(__name__+" Begin chip-setup",10)

        M_RATE = "01"
        REPEAT = "1"

        raw_config_arr = []
        with open(self.config_file,"r") as config_file:
            for line in config_file:
                raw_config_arr.append(line.strip())

        main_conf = utils.conv_str('0000'+M_RATE+"0"+REPEAT+"11110000")
        reset = utils.conv_str("1000000000000000")

        for chipNum in range(self.CONNECTED_CHIPS):
            time.sleep(0.5)
            self.swap_tca(chipNum)
            time.sleep(2)
            self.bus.write_word_data(self.ADDR, 0x0C, int(reset,2))
            time.sleep(0.1)
            self.bus.write_word_data(self.ADDR, 0x0C, int(main_conf, 2))
            for x in range(4):
                self.bus.write_word_data(self.ADDR,
                        0x08+x,
                        int(utils.conv_str(raw_config_arr[4*chipNum+x]), 2))
        self.ilog(__name__+" Chip-Setup done", 20)
        time.sleep(1)

    def swap_tca(self, number):
        """ Let the TCA multiplexer switch between port ids """
        self.bus.write_byte(self.TCA_ADDRESS, 1<<number)
        address_on_chip = self.bus.read_byte(self.TCA_ADDRESS)
        self.ilog(__name__+" Address on chip:{}".format(address_on_chip),10)

    def reset_tca(self):
        """ Resets the TCA multiplexer to basic setup """
        self.bus.write_byte(self.TCA_ADDRESS,0)
        self.ilog(__name__+" TCA Reset Done ",10)

    def quick_get_data(self):
        """ One-in-all method to get matrix with results

        Make sure to wait between measurements! You should at least
        wait CUS_SCL before you try to read the next results

        Hint:
            Currently the wait time is applied in read_all_results

        """
        self.prepare_matrix()
        dic = self.read_all_results()
        #DEBUG PRINTS:
        #printStr = ""
        #for x,y in dic.items():
        #   printStr = printStr + "{:6}".format(y)
        #print(printStr)
        self.map_to_matrix(dic)
        self.ilog(__name__+ str(self.matrix), 10)
        return self.matrix

    def refine_setup(self):
        self.ilog(__name__+" Begin chip refining",10)
        old_config_arr = []
        THRESHOLD_DELTA = 4
        THRESHOLD = 3.5
        with open(self.config_file,"r") as file:
            for line in file:
                old_config_arr.append(line.strip())
        for chipNum in range(self.CONNECTED_CHIPS):
            time.sleep(0.1)
            self.swap_tca(chipNum)
            time.sleep(0.1)
            reconfig_done = False
            while not reconfig_done:
                values = self.get_averages(chipNum)
                # (0,1,2) ->  average, max, min
                deltas = [0]*4
                reconfig_done = True
                for j in range(4):
                    if (values[1][j] - values[2][j]) > THRESHOLD_DELTA:
                        deltas[j] = values[1][j] - values[2][j]
                    if values[0][j] > THRESHOLD:
                        self.ilog("Average for chip {}:{} is {}".format(
                            chipNum, j, values[0][j]), 20)
                        #threshold underscored so reconfig_done -> False
                        reconfig_done = False
                        #Reconfig need: average above threshold
                        #Begin to build new config: measurement pin+capdac(docu)
                        newConfig = utils.toBin(j,3) + "100"

                        multiplicator = math.floor(values[0][j]/3)*2 #exact3.125
                        self.ilog(__name__+" new multiplicator {}".format(
                            multiplicator),10)
                        old_multiplicator = int(
                            old_config_arr[chipNum*4+j][6:11],2)
                        new_multiplicator = old_multiplicator + multiplicator
                        new_multiplicator_str = utils.toBin(new_multiplicator,5)
                        # Too much offset catch
                        if(len(new_multiplicator_str)>5):
                            self.ilog(__name__+" MULTIPLATOR EXCEEDED MAX", 30)
                            new_multiplicator_str = "11111"
                            reconfig_done = True
                        newConfig = newConfig + new_multiplicator_str
                        newConfig = newConfig + "00000" #padding
                        old_config_arr[chipNum*4+j] = newConfig

                        self.single_setup(j, newConfig, True)
                        control_value = utils.auto_bin(self.bus.read_word_data(
                            self.ADDR, 0x08+j))
                        self.ilog(__name__+" Config loaded {}".format(
                            newConfig ==  control_value), 20)
                self.ilog(__name__+" Deltas for Chip {} are {}".format(
                    chipNum,deltas), 20)
        with open(self.config_file,"w") as file:
            for item in old_config_arr:
                file.write(item+"\r\n")
        self.ilog(__name__+" chip refining done!", 20)

    def get_averages(self, chipNum, numOfMeasurements=30):
        """ Calculates the average of one connected chip

        Args:
            chipNum(int): ID of the current chip (for logging purposes)
            numOfMeasurements(int): How many values should be read to calc
            measurement.

        Returns:
            (int[], int[], int[]):
                average,
                max
                min values
                    in pairs of 4 (for each measurements)
        """
        average = [0]*4
        max = [0]*4
        min = [0]*4
        firstIterationDone = False

        for idx in range(numOfMeasurements):
            time.sleep(0.125)
            while 1:
                cur_state = \
                        utils.auto_bin(
                                self.bus.read_word_data(self.ADDR, 0x0C))
                if self.check_results(cur_state):
                    break
                else:
                    self.ilog(__name__+"Res. not rdy. Id:{}".format(chipNum),30)
                    self.ilog(__name__+str(cur_state), 30)
                    time.sleep(0.1)
                    #TODO: If cur_state = "0"*16 -> resetup
            for measurement in range(4):
                cur = self.read_result(measurement)
                average[measurement] = average[measurement] + cur
                if measurement == 0:
                    max[measurement] = cur
                    min[measurement] = cur
                else:
                    if max[measurement] < cur:
                        max[measurement] = cur
                    if min[measurement] > cur:
                        min[measurement] = cur
        for i in range(4):
            average[i] = utils.floor2(average[i]/numOfMeasurements)
        return (average, max, min)

    # utils

    def create_default_config(self):
        with open(self.config_file,"w") as config:
            for chipID in range(self.CONNECTED_CHIPS):
                for i in range(4):
                    config.write(utils.toBin(i,3)+"100"+"00000"+"00000")
                    config.write("\r\n")

import time

if __name__ == "__main__":
    counter = 0
    a = FDCManager()
    while 1:
        time.sleep(0.05)
        try:
            a.quick_get_data()
        except OSError:
            counter = counter + 1
            print("cont: {}".format(counter))
            #traceback.print_exc()
            continue
            #a.setup_chips()
