###############################################################
#
# Gesture Recognizer Test-Bed
#
# (c) Oct. 2008, Sven Kratz, Deutsche Telekom Laboratories, TU Berlin
###############################################################

import e32, graphics, sysinfo
import appuifw
import axyz
import audio

from key_codes import *
import sys
sys.path.append('e:\\python\\libs')
import gesturerec3d as gr
import audio

GESTURE_LIBRARY_PATH = "e:\\gesture_lib.dat"


running = True

key_dn = appuifw.EEventKeyDown
key_up = appuifw.EEventKeyUp

# maybe a bit too pythonic...

states = {"RECOGNITION":0, "TRAINING":1}


def unregister_accel():
    axyz.disconnect()

class GestureApp:
    def __init__(self):
        #gr settings
        self.g = gr.GestureRec3D(gLibrary=GESTURE_LIBRARY_PATH,
                                 resample_size = 50, 
                                 gesture_recognized_callback = self.recognized_callback )
        self.RECOGNIZING = False
        self.STATE = states["RECOGNITION"]
        self.gesture_list = []
        self.gesture_detected = ""
        self.current_training_code = 0
        self.old_acceleration = [0,0,0]
        self.sound = False
        # app settings
        # self.tx = appuifw.app.body = appuifw.Text()
        # self.tx.bind(, self.recognizer_clutch_down)
        #self.tx.bind(EEventKeyUP, self.recognizer_clutch.up)
        self.body = appuifw.app.body = appuifw.Canvas(None, self.handle_event)
        #tx.color(255,0,0)
        #self.tx.style = appuifw.STYLE_BOLD   
        appuifw.app.exit_key_handler = self.quit
        appuifw.app.screen="normal"
        self.menu = appuifw.app.menu = [(u"Accelerometer:",
                             ((u"Accelerometer ON", self.register_accel), (u"Accelerometer OFF", unregister_accel))),
                             (u"Gesture Recognition:",
                             ((u"ON", self.gesture_rec_on),(u"OFF", self.gesture_rec_off))),
                             (u"Training",
                             ((u"Gesture 1", lambda: self.train("1")),
                              (u"Gesture 2", lambda: self.train("2")),
                              (u"Gesture 3", lambda: self.train("3")),
                              (u"Gesture 4", lambda: self.train("4")),
                              (u"Gesture 5", lambda: self.train("5")),
                              (u"Save Library", self.g.gesture_lib.save),
                              (u"Clear Library",  self.clear_gesture_lib))),
                              (u"Audio",((u"ON", lambda: self.set_sound(True)),(u"OFF", lambda: self.set_sound(False)))),
                              (u"Quit", self.quit)]
        self.register_accel()
    def set_sound(self, toggle):
        self.sound = toggle
    def printout_(self,x,y,z):
        # function for callgate
        #self.tx.clear()
        #self.body.clear()
        #update = u"x %d y %d z%d" % (x,y,z)
        #self.tx.add(update)
        #self.body.text([10,10], update)
        if self.RECOGNIZING:
            old = self.old_acceleration
            delta = [x-old[0], y-old[1],z-old[2]]
            self.gesture_list.append([float(delta[0]),float(delta[1]),float(delta[2])])
            self.old_acceleration = [x,y,z]
    def quit(self):
        running = False
        unregister_accel()
        app_lock.signal()
        appuifw.app.exit_key_handler = self.quit   
    def register_accel(self):
        axyz.connect(e32.ao_callgate(self.printout_)) 
    def gesture_rec_off(self):
        print "Turning gesture recognition OFF"
        self.gesture_list = []
        self.RECOGNIZING = False
    def recognized_callback(self, gID):
        print "Recognized Callback", gID
        if self.sound:
            try:
                audio.say("Geeste " + str(gID))
            except:
                pass
        self.body.clear()
        self.body.text([10,20], unicode(str(gID)), font=u'LatinBold19')
    def gesture_rec_on(self):
        print "Turning gesture recognition ON"
        self.gesture_list  = []
        self.RECOGNIZING = True
        self.STATE = states["RECOGNITION"]
    def handle_event(self, event):
        if event['type'] == key_dn:
            print "Key Down"
            print "KeyEvent -- scancode", str(event['scancode']), str(EScancodeSelect)
            if event['scancode'] == EScancodeSelect:      
                if not self.RECOGNIZING :
                    self.RECOGNIZING = True
                    self.gesture_list = []
        elif event['type'] == key_up:
            print "Key UP"  
            if event['scancode'] == EScancodeSelect: 
                if self.RECOGNIZING :
                    self.RECOGNIZING = False
                    if self.STATE == states["RECOGNITION"]:
                        print "Detecting Gesture"
                        self.gesture_detected = self.g.recognize_gesture(self.gesture_list, True) 
                        print "Recognized the following Gesture", self.gesture_detected
                        #self.recognized_callback(self.gesture_detected)
                    elif self.STATE == states["TRAINING"]:
                        print "Saving Data to Gesture Library"
                        print "Got", len(self.gesture_list), "tuples."
                        print self.gesture_list
                        self.g.save_data_to_library(self.current_training_code, [10,20,50], self.gesture_list)
                        self.gesture_list = []                       
    def recognier_clutch_up(self, event):
        print "KeyUP", str(event['keycode'])         
    def train(self,x):
        self.current_training_code = x
        print "Currently training Gesture:",x
        self.STATE = states["TRAINING"]
    def clear_gesture_lib(self):
        self.g.gesture_lib.clear_library()


application = GestureApp()
app_lock = e32.Ao_lock()
app_lock.wait()
appuifw.app.body = None
print "App Terminated"
