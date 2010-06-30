##############################################################
#
# GestureLibrary.py -- Library for gesture data captured with WiiMote
#
# (c) Sven Kratz, Deutsche Telekom Labs, TU Berlin, 2008
#
# Saves and loads captured gesture data to and from disk.
#
#  ++PyS60 Version++
#
###############################################################


#import cPickle as pickle      # object serialization (save to disk) -- faster c-version
import pickle
import traceback
import random

class GestureLibrary:
    def __init__(self,path):
        self.file_path = path
        self.gesture_dict = {}  # saved as key : gesturelist pairs
        self.gesture_codes = []
        # gids which we accept (errors are marked as "z", for example)
        self.accepted_gids = ["a\n","b\n","c\n","d\n","e\n","f\n","g\n","h\n","i\n","j\n"]
        try:
            f = open(self.file_path, 'rb')
            try:
                self.gesture_dict = pickle.load(f)
                self.gesture_codes = self.gesture_dict.keys()
                print "Gesture Library Loaded Successfully"
                self.print_stats()

            except:
                print "Error unpacking file, gesture dict will be empty"
                self.gesture_dict = {}
        except:
            print "File",self.file_path," doesn't exist...creating"
            f = open(self.file_path, 'w')
            self.gesture_dict  = {}
            f.close

    def get_gesture_lists(self,gesture_length=250, getTrainingSets = False, getOnlyCandidates = False):
        """returns None if no gesture exists, else returns all gestures of length=250"""
        out = []
        for gid,g_dict in self.gesture_dict.iteritems():
            # filter out training sets if desired
            if g_dict.get("trainingSet") != True or getTrainingSets:
                if getOnlyCandidates == g_dict.get("candidateSet"):
                        glist = g_dict[gesture_length]
                        out.append([gid,glist])
                else:
                    glist = g_dict[gesture_length]
                    out.append([gid,glist])
        return out

    def getGesturesByIdAndLength(self,gestureID,gesture_length=250):
        """ get gestures by Gesture Id and by Length
            used to retrieve list of training gestures"""
        out = None
        gestures  = self.gesture_dict.get(gestureID)
        if gestures != None:
            out = gestures.get(gesture_length)
        return out

    def add_gesture(self, gestureCode, gesture, save=False, gesture_length=None):
        """adds gesture to gesture library, and updates disk on file"""
        if gesture_length == None:
            gesture_length = len(gesture)
        g_dict = self.gesture_dict.get(gestureCode)
        if g_dict == None:            # not in top-level dict
            g_dict = {}                     # create sub-dictionary
            self.gesture_dict[gestureCode] = g_dict # assign to dict
        glist = g_dict.get(gesture_length)
        if glist == None:               # nothing in sub-dictionary
            glist = []
        glist.append(gesture)
        self.gesture_dict[gestureCode][gesture_length] = glist
        self.gesture_codes = self.gesture_dict.keys()
        if save:
           try:
               self.save()
               self.print_stats()
           except Exception, e:
                print "Problem While Saving Gesture Library"
                print e
                print traceback.print_exc()

    def has_gestures(self):
        """checks if there are Gestures in the dictionary"""
        return len(self.gesture_dict) > 0

    def to_string(self):
        self.print_stats()

    def print_stats(self):
        print "Gesture Dictionary Statistics"
        print "Code, Nr. of Gestures"
        for code,gdict in self.gesture_dict.iteritems():
            print "GID:",code
            for l, gesturelist in gdict.iteritems():
                try:
                    print "    ",str(l), str(len(gesturelist))
                except:
                    pass
            print "\n"


    def get_training_and_candidate_sets(self):
        """returns training and candidate sets (if any)"""
        candidate_sets = {}
        training_sets = {}
        for gid in self.gesture_dict.iterkeys():
            # strip identifier and save
            if gid[0] == 'T':
                training_sets[gid[1:]] = self.gesture_dict[gid]
            elif gid[0] == "C":
                candidate_sets[gid[1:]] = self.gesture_dict[gid]
        return candidate_sets, training_sets

    def generate_training_and_candidate_sets_random(self, numElements = 5):
        """Creates training sets of for the gesture classes in order to perform recognition tests
            and to define a new training set for release versions
            @param numelements: amount of elements to be randomly included in training set, default 5 """
        for gid,gdict in self.gesture_dict.items():
            trainingID = "T"+gid
            candidateID = "C"+gid
            self.gesture_dict[trainingID] = {}
            self.gesture_dict[candidateID] = {}
            self.gesture_dict[trainingID]["trainingSet"]=True
            print "Adding Training Set",trainingID
            print "Adding Candidate Set",candidateID
            for l, gestureList in gdict.items():
                selectList = []
                trainingSet = []
                # perform a list copy of the original list
                selectList = selectList + gestureList
                # naive selection algorithm
                if len(selectList) >= numElements:
                    for i in range(numElements):
                        # select random element
                        elem = random.choice(selectList)
                        # append selection to output
                        trainingSet.append(elem)
                        # remove selected element from list of options
                        selectList.remove(elem)
                    self.gesture_dict[trainingID][l] = trainingSet
                    self.gesture_dict[candidateID][l]= selectList
    def generate_training_and_candidate_sets(self, numElements = 5, endElement = 15):
        """Creates training sets for the gesture classes in order to perform recognition tests
            @param numelements: defines the amount of elements (from beginning of sample list) to be included in training set"""
        for gid,gdict in self.gesture_dict.items():
            if gid in self.accepted_gids:
                trainingID = "T"+gid
                candidateID = "C"+gid
                self.gesture_dict[trainingID] = {}
                self.gesture_dict[candidateID] = {}
                self.gesture_dict[trainingID]["trainingSet"]=True
                print "adding Training Set", trainingID,
                print "adding Candidate Set", candidateID,
                for l, gestureList in gdict.items():
                    if len(gestureList) >= 15:
                        # first five samples are the training set
                        self.gesture_dict[trainingID][l] = gestureList[0:numElements]
                        print len(self.gesture_dict[trainingID][l]),
                        # rest are candidate ghestures
                        self.gesture_dict[candidateID][l] = gestureList[numElements:endElement]
                        print len(self.gesture_dict[candidateID][l]),";",
                    else:
                        print "Error: Gesture List not long enough"
                print ""


    def dump_to_csv(self, resampling = 150):
        """dumps library contents to csv formatted file"""
        filepath = self.file_path + "_l_"+str(resampling)+".csv"
        print "Writing to:",filepath
        h = open(filepath,'w')
        outstr = ""
        for gid, dict in self.gesture_dict.iteritems():
            count  = 0
            for trace in dict[resampling]:
                outstr = outstr + "gesture_id: " + str(gid) + "nr: "+ str(count)+"\n"
                for tuple in trace:
                    outstr = outstr + str(tuple[0]) + ","+str(tuple[1])+","+str(tuple[2])+"\n"
                count = count + 1
        h.write(outstr)
        h.close()
        print "Done!"
        
    def clear_library(self):
        """Deletes all entries in the library"""
        self.gesture_codes = []
        self.gesture_dict = {}

    def save(self):
        f  = open(self.file_path,'wb')
        pickle.dump(self.gesture_dict,f)    # dump out the dictionaries
        f.flush()
        f.close()