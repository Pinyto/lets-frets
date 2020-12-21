import logging
import math

def prepare_logger(name="Default Name",level=logging.DEBUG):
    """Support method to get a logger.

    To keep code clean, the basic logger initialization is made here.

    Note:
        It's alwas a StreamHandler. If you want to log to a file, you
        need to change it afterwards.

    Args:
        name (string): Name of the logger (else 'Default Name")
        level (int): Logger level, choose:

            ===   ===========
            int   description
            ===   ===========
            00    NOTSET
            10    DEBUG
            20    INFO
            30    WARNING
            40    ERROR
            50    CRITICAL
            ===   ===========

    Returns:
        logger: logger


    """
    logger = logging.getLogger(name)
    logger_handler = logging.StreamHandler()
    logger_form = logging.Formatter("%(name)s - %(levelname)s - %(message)s")
    logger_handler.setFormatter(logger_form)
    logger.addHandler(logger_handler)
    logger.setLevel(level)
    return logger

def floor2(num):
    """ Rounds to two decimal points """
    return (math.trunc(num*100) / 100 )

def conv(le):
    """ Integer Big-Endian <-> Little-Endian Conversion

    Warning:
        Only suits for 8-bit binary integers.

    See Also:
        conv_str() for string conversion.

    """
    return ((le << 8) & 0xFF00) + (le >> 8)

def convStr(le):
    """ Renamend to conv_str

    Note:
        Please refer to the conv_str() version.

    """
    return conv_str(le)

def conv_str(le):
    """ Binary(String) Big-Endian <-> Little-Endian Conversion

    Warning:
        Only supports 16-Bit Strings.

    Todo:
        Imlement with a variable bit amount.

    """
    return le[8:]+le[:8]

def twoscomplement(bin_string):
    """ Calculates the twoscomplement value of an binary string

    Args:
        bin_string(string): A binary representation of a number
    Return:
        Twoscomplement from input string
    """
    result = None
    if int(bin_string[0]):
        result = int(bin_string[1:],2) - int(1<<(len(bin_string)-1))
    else:
        result = int(bin_string[1:],2)

    return result

def toHex(value,count=4):
    """Returns the given value in hex string format

    Note:
        Does not support negative numbers, returns 0 instead.

    Args:
        value(int): Number to change to hex string
        count(int): Site of the string. Padded with 0s.

    Return:
        string: hex representation of given number.
                Without '0x', padded with 0s.
    """
    if(value<0):
        print("Negative values are not support by 'toHex'. Returning 0")
        return toHex(0,count)
    hexValue = format(value,'x')
    if len(hexValue)<count:
        hexValue = '0'*(count-len(hexValue))+hexValue
    elif len(hexValue)>count:
        print("HexValue to big. Returning anyways")
    return hexValue

def toBin(value,count=16):
    """ Return the given number in a padded binary string format

    Note:
        No native support for negative numbers. Uses toBin2C method.

    Args:
        value(int): Number to change to binary string
        count(int): Size of the string. Padded with 0s.

    Return:
        string: binary representation of given number.
                Without '0b', padded according to value:

                1 for negative numbers (toBin2C)
                0 for positive numbers
    """
    if(value<0):
        print("Negative value. Returning toBin2C")
        return toBin2C(value,count)
    binValue = format(value,'b')
    if len(binValue)<count:
        binValue = '0'*(count-len(binValue))+binValue
    elif len(binValue)>count:
        print("BinValue to big. Returning anyways")
    return binValue

def toBin2C(value,count=16):
    """ Returns the twoscomplement format, without '0b' """
    return bin(value % (1<<count))[2:]

def auto_bin(value,count=16):
    """ Shortcut: Calls toBin & conv_str on given arguments """
    return conv_str(toBin(value,count))

def pretty_print(binstring):
    """ pretty print for binary and hex values with word-size"""
    xStr = toHex(int(binstring,2))
    print("|= = = = = = = = = = = =|")
    print("| ..12  ...8  ...4  ...0|")
    print("| "+binstring[0:4] +"  " + binstring[4:8] + "  " + binstring[8:12] + "  " + binstring[12:]+"|")
    print("| "+"0x "+xStr[0]+" "*5+xStr[1]+" "*5+xStr[2]+" "*5+xStr[3]+"|")
    print("|= = = = = = = = = ="+str(int(binstring,2))+"|")

def findValueBiggerThreshold(matrix,threshold):
    for row in matrix:
        for n in row:
            if(n>=threshold):
                return True
    return False

class SharedVariable:
    """ Shared variable to pass to funtions and threads """
    storage = 0
    state_changed = False
    task_done = False

    def __init__(self, toStore):
        self.storage = toStore
        self.task_done = False

    def get_storage(self):
        self.state_changed = False
        return self.storage

    def peek_storage(self):
        return self.storage

    def has_changed(self):
        return self.state_changed

    def set_storage(self,new_value):
        self.storage = new_value
        self.state_changed = True

    def increment_storage(self):
        self.storage = self.storage + 1
        self.state_changed = True

    def get_task_done(self):
        return self.task_done

    def set_task_done(self):
        self.task_done = True
