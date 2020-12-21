import utils
import BluetoothManager
import LedManager
import time
import tinyled
import threading
import traceback
import json
import os
import FDCManager
import signal

def modeShow(client, data):
    sharedVariable = utils.SharedVariable("")
    thread_indicator_done = threading.Thread(
        target=thread_wait_for_cancel, args=(client,sharedVariable,),daemon=True)
    thread_indicator_done.start();
    mainlogger.info(__name__+" Mode-Show started, Thread init, Waiting For End")
    threshold = 9
    needy_counter = 0
    while(not sharedVariable.get_task_done()):
        answer = {"version":"1.0","mode":"show"}
        try:
            matrix = fm.quick_get_data()
        except Exception as e:
            answer["success"] = False
            bm.send_json(client, answer)
            mainlogger.error(__name__+"Error while fetching data from chips,"+
                " returning to catch in main")
            raise e
        # Search for a value, worth sending
        needy_counter = needy_counter + 1
        if(not utils.findValueBiggerThreshold(matrix, threshold)):
            if(not needy_counter>=10):
                continue
            else:
                needy_counter = 0
        else:
            needy_counter = 0
        answer["success"] = True
        results = {}
        #TODO: Check for relevant values (threashold)
        for row in range(len(matrix)):
            results[row] = matrix[row]
        answer["refined"] = results
        bm.send_json(client, answer)
    bm.send_json(client, {"version":"1.0","mode":"show_done"})
    thread_indicator_done.join(0.2)
    mainlogger.info(__name__+"Show mode exit normally, returning to main loop")
#End of mode_show


def modeTSG(client, data):
    """ This method controls the train single grip state

    Note:
        If tsg_feedback is on, this method will return a second positiv
        signal to the phone, as soon as the grip is pressed successfully.
    Todo:
        Maybe add a X-Minute Timeout

    """
    tsg_feedback = False
    stringsToHit = "000000"
    error = False
    position_matrix = []
    base = [-2]*6
    for i in range(4):
        position_matrix.append(base.copy())
    lm.cleanup();
    try:
        if(data["version"]=="1.0"):
            mainlogger.debug(__name__+" TSG_Version Check Complete")
            tsg_feedback = data["tsg_feedback"]
            stringsToHit = data["strings"]
            lm.set_strings_to_hit(stringsToHit)
            positions = data["positions"]
            for ele in positions:
                fret = ele["fret"]
                string = ele["string"]
                fingerID = ele["finger"]
                lm.set_single_in_matrix(fret, string, fingerID)
                position_matrix[fret][string] = fingerID
        else:
            error = True
    except KeyError as e:
        mainlogger.warning(__name__+" attrbute not found: "
        +traceback.format_exc())
        error = True
    lm.applydata()

    #Signals phone if data was correct
    answer = {}
    answer["prepare_done"] = not error
    answer["tsg_feedback"] = tsg_feedback
    answer["version"] = "1.0"
    bm.send_data(client,  json.dumps(answer).encode("iso-8859-1"))
    if(error):
        #Return to mode select after notifying the phone
        raise KeyError

    mainlogger.debug(__name__ + "Expected: "+str(position_matrix))
    if(tsg_feedback):
        #TODO: loop till correct fields are pressed, then send confirmation
        grip_complete = False
        while(not grip_complete):
            try:
                grip_complete = True
                # TODO: Change quick_get_data to fret::string
                matrix = fm.quick_get_data();

                # TODO: Threshold
                threshold = 10
                for fret_id in range(len(matrix)):
                    for string_id in range(len(matrix[fret_id])):
                        expect_result = position_matrix[fret_id][string_id]!=-2
                        got_result = matrix[fret_id][string_id]>threshold
                        if(expect_result and not got_result):
                            grip_complete = False
                            break
                        elif(got_result and not expect_result):
                            grip_complete = False
                            break
                    if(not grip_complete):
                        break
            except OSError as e:
                mainlogger.warning(__name__+
                    "IO Error while trying to recieve data from chips")
                raise OSError
        answer = {}
        answer["version"] = "1.0"
        answer["success"] = True
        bm.send_data(client,  json.dumps(answer).encode("iso-8859-1"))
        # Hold colors, they will get swiped when the next grip arrives
        #lm.colorwipe()
#End of tsg_mode

def modeConfig(client, data):
    """ Handles sending and receiving config data """
    config_done = False
    if(data["version"]=="1.0"):
        while(not config_done):
            current_config = lm.CONFIG
            config_to_send = {}
            config_to_send["version"] = "1.0"
            config_to_send["mode"] = "config"
            config_to_send["top_installed"] = current_config["TOP_INSTALLED"]
            config_to_send["brightness"] = current_config["BRIGHTNESS"]
            config_to_send["wait_ms"] = current_config["WAIT_MS"]
            config_to_send["colors"] = current_config["finger_color"]
            bm.send_json(client, config_to_send)
            mainlogger.info(__name__ + "Send current config to phone")
            mainlogger.debug(__name__ + "Data Send: "+str(config_to_send))
            new_data = bm.await_data(client)
            new_data = json.loads(new_data)
            try:
                if(new_data["version"]=="1.0"):
                    if(new_data["mode"]=="main"):
                        mainlogger.info(__name__ + "Returning to main dataloop")
                        return
                    elif(new_data["mode"]=="config"):
                        mainlogger.debug(__name__+ " new config received:"+
                            str(new_data))
                        if("top_installed" in new_data):
                            lm.CONFIG["TOP_INSTALLED"] = new_data["top_installed"]
                        if("brightness" in new_data):
                            lm.setbrightness(new_data["brightness"])
                        if("wait_ms" in new_data):
                            lm.CONFIG["WAIT_MS"] = new_data["wait_ms"]
                        if("colors" in new_data):
                            new_colors = new_data["colors"]
                            for id in range(-2,5):
                                if(str(id) in new_colors):
                                    lm.CONFIG["finger_color"][str(id)] = new_colors[str(id)]
                    elif(new_data["mode"]=="reboot"):
                        os.system("reboot")
                    elif(new_data["mode"]=="power_off"):
                        os.system("poweroff")
            except KeyError as e:
                mainlogger.warning(__name__ + "")
                # TODO: Cont storing new config in file, sending new config to phone
            lm.store_config()
            mainlogger.info(__name__ + "one config round complete")

def dataloop(client):
    """ Manages incomming data

    Swaps to a other method to manage the current state.

    Todo:
        Add exception for encoding error
        Control the incomming json?
        Send the json to the ledcontrfoller

    """
    #TODO: Show state in LEDs
    #TODO: Maybe start thread with LED circle
    while 1:
        shared_thread_counter = utils.SharedVariable(0)
        thread_indicator = threading.Thread(
            target=lm.thread_idle_indicator, args=(shared_thread_counter,),
                daemon=True)
        thread_indicator.start()
        data = bm.await_data(client)
        shared_thread_counter.set_task_done()
        thread_indicator.join(2)
        #Debug call
        mainlogger.debug("Incomming data: {}".format(data))
        data = data.decode('iso-8859-1')

        cur_json = json.loads(data)
        state = "UNFDEFINED"
        try:
            if cur_json["version"]=="1.0":
                state = cur_json["mode"]
                if state == "config":
                    modeConfig(client, cur_json)
                elif state == "single":
                    raise NotImplementedError
                elif state == "show":
                    # Enables show mode ->
                    # Send 10 packets of raw values ->
                    # Wait for continue packet (loop)
                    # Or interrupt asynchron ?
                    modeShow(client, cur_json)
                elif state == "TSG":
                    modeTSG(client, cur_json)
                elif state == "row":
                    raise NotImplementedError
                elif state == "clear":
                    lm.cleanup()
                elif state == "menu":
                    pass
        except OSError as e:
            mainlogger.warning(__name__ + traceback.format_exc())
            mainlogger.warning(__name__ + "Problem with I2C. Resetting Chips")
            #Todo: Trigger chip reset
        except KeyError as e:
            mainlogger.warning(__name__ + traceback.format_exc())
            mainlogger.warning(__name__ + "Key-Exception in: {}".format(state))
        except json.decoder.JSONDecodeError as e:
            mainlogger.error(__name__
                + " there was an error in the incomming data. Retry please")
            #Todo: Maybe try to readout mode
#End of dataloop

def connectionloop(socket):
    """ Loops endless till a connection is established

    Resets all devices every 4 tries, each take one minute

    Todo:
        Handle ready_bluetooth() return value

    Return:
        client: A socket connection to a client
    """
    client = None

    while 1:
        shared_thread_counter = utils.SharedVariable(0)
        #Todo: Maybe its not possible to pass a function of a class
        thread_indicatorA = threading.Thread(
            target=lm.thread_load_indicator, args=(shared_thread_counter,),
                daemon=True)
        thread_indicatorA.start()
        while shared_thread_counter.peek_storage() < 5:
            bm.ready_bluetooth()
            try:
                client, address = bm.build_connection(socket)
            except UserWarning as e:
                mainlogger.warning(__name__ + " timeout occured.")
            except KeyboardInterrupt:
                shared_thread_counter.set_task_done()
                thread_indicatorA.join(0.5)
                raise KeyboardInterrupt
            else:
                shared_thread_counter.set_task_done()
                thread_indicatorA.join(2)
                return client
            shared_thread_counter.increment_storage()
        else:
            shared_thread_counter.set_storage(-1)
            bm.reset_devices()
            shared_thread_counter.set_task_done()
            thread_indicatorA.join(2)
#END connectionloop

def main():
    global mainlogger
    global bm
    global lm
    global fm

    loggerlevel = 10
    refine = False
    if(len(os.sys.argv)>1):
        loggerlevel = os.sys.argv[1]
        if(len(os.sys.argv)>2):
            refine = os.sys.argv[2]
    mainlogger = utils.prepare_logger(__name__,int(loggerlevel))
    mainlogger.info("Logger ready")
    mainlogger.info("Building up bluetooth connection")


    bm = BluetoothManager.BlueManager(mainlogger.getChild("BluetoothManager"))
    lm = LedManager.LedManager(mainlogger.getChild("LedManager"))
    fm = FDCManager.FDCManager(mainlogger.getChild("FDCManager"),refine)

    socket = bm.build_socket()
    client = None

    try:
        while 1:
            client = connectionloop(socket)
            try:
                dataloop(client)
            except Exception as e:
                if e == UnboundLocalError:
                    mainlogger.error(traceback.format_exc())
                elif e == ConnectionResetError:
                    mainlogger.warning("Connection reset")
                    mainlogger.warning("Connection reset error")
                    continue
                elif e == Exception:
                    print(e)
                    mainlogger.error(traceback.format_exc())
                    mainlogger.error("Unknown/handled exception")
                    break
                else:
                    mainlogger.warning("Error unhandeld. Doing nothing")
                    mainlogger.warning(traceback.format_exc())
    finally:
        lm.colorwipe()
        bm.clean_up(client, socket)


# Hacks

def handler(signalnumber, frame):
    mainlogger.warning("Package lost, continue")
    raise UserWarning

def thread_wait_for_cancel(client, sharedVariable):
    """ Thread that listens to the bluetooth connection to stop loop """
    data = bm.await_data(client)
    data = json.loads(data.decode(bm.ENCODING))
    sharedVariable.set_task_done()
    sharedVariable.set_storage(data)


if __name__ == "__main__":
    main()
